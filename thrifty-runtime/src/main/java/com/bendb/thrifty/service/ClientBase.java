/*
 * Copyright (C) 2015 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.thrifty.service;

import com.bendb.thrifty.ThriftException;
import com.bendb.thrifty.protocol.MessageMetadata;
import com.bendb.thrifty.protocol.Protocol;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implements a basic service client that executes methods asynchronously.
 *
 * <p>Note that, while the client-facing API of this class is callback-based,
 * the implementation itself is <strong>blocking</strong>.  Unlike the Apache
 * implementation, there is no presumption made here about framed encoding
 * at the transport level.  If your backend requires framing, be sure to
 * configure your {@link Protocol} and {@link com.bendb.thrifty.transport.Transport}
 * objects appropriately.
 */
public class ClientBase implements Closeable {
    public interface Listener {
        void onTransportClosed();
        void onError(Throwable error);
    }

    private final AtomicInteger seqId = new AtomicInteger(0);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();

    private final Queue<MethodCall<?>> outbox = new LinkedList<>();
    private final Queue<MethodCall<?>> inbox = new LinkedList<>();

    private final Lock lock = new ReentrantLock();
    private final Condition hasQueuedData = lock.newCondition();
    private final Condition waitingForReply = lock.newCondition();

    private final Protocol protocol;
    private final Listener listener;
    private final RunLoop writer;
    private final RunLoop reader;

    protected ClientBase(Protocol protocol, Listener listener) {
        this.protocol = protocol;
        this.listener = listener;
        this.writer = new WriterThread();
        this.reader = new ReaderThread();

        writer.setDaemon(true);
        reader.setDaemon(true);

        writer.start();
        reader.start();
    }

    protected void enqueue(MethodCall<?> methodCall) throws IOException {
        if (!running.get()) {
            throw new IllegalStateException("Cannot write to a closed service client");
        }

        lock.lock();
        try {
            if (!running.get()) {
                throw new IllegalStateException("Cannot write to a closed service client");
            }

            outbox.add(methodCall);
            hasQueuedData.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        close(null);
    }

    private void close(Throwable error) {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        lock.lock();
        try {
            outbox.clear();
            inbox.clear();

            waitingForReply.signalAll();
            hasQueuedData.signalAll();
        } finally {
            lock.unlock();
        }

        reader.interrupt();
        writer.interrupt();

        try {
            protocol.close();
        } catch (IOException ignored) {
            // nope
        }

        try {
            callbackExecutor.shutdown();
        } catch (Exception ignored) {
            // nope
        }

        // Listener callbacks need to be synchronous - we have just
        // shut down our executor.  If we rearranged this method, it
        // wouldn't make a difference - the executor would still have
        // been shut down
        if (error != null) {
            listener.onError(error);
        } else {
            listener.onTransportClosed();
        }
    }

    private abstract class RunLoop extends Thread {
        @Override
        public void run() {
            Throwable error = null;
            while (running.get()) {
                try {
                    act();
                } catch (InterruptedIOException | InterruptedException e) {
                    // Either we were closed, in which case transition normally,
                    // or we were interrupted for some mysterious reason, in which
                    // case just keep on truckin'.
                } catch (Exception e) {
                    error = e;
                    break;
                }
            }

            try {
                close(error);
            } catch (Throwable ignored) {
                // nope
            }
        }

        abstract void act() throws Exception;
    }

    private class WriterThread extends RunLoop {
        @SuppressWarnings("Duplicates")
        @Override
        void act() throws Exception {
            final MethodCall call;

            lock.lock();
            try {
                while (outbox.isEmpty()) {
                    hasQueuedData.await();

                    if (!running.get()) {
                        return;
                    }
                }

                call = outbox.remove();
            } finally {
                lock.unlock();
            }

            boolean isOneWay = call.callTypeId == TMessageType.ONEWAY;

            protocol.writeMessageBegin(call.name, call.callTypeId, seqId.incrementAndGet());
            call.send(protocol);
            protocol.writeMessageEnd();

            // Small messages may be lingering in a send buffer, but too
            // many flushes are not good.  As a first guess at an heuristic
            // to improve latency, only flush if there are no more queued
            // method calls.
            boolean hasMoreCalls;

            lock.lock();
            try {
                hasMoreCalls = !outbox.isEmpty();
            } finally {
                lock.unlock();
            }

            if (!hasMoreCalls) {
                protocol.flush();
            }

            if (isOneWay) {
                // null is always safe to pass here - oneway methods
                // are guaranteed to be Void anyways.
                //noinspection unchecked
                complete(call, null);
            } else {
                lock.lock();
                try {
                    inbox.add(call);
                    waitingForReply.signal();
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    private class ReaderThread extends RunLoop {
        @SuppressWarnings("Duplicates")
        @Override
        void act() throws Exception {
            final MethodCall call;

            lock.lock();
            try {
                while (inbox.isEmpty()) {
                    waitingForReply.await();
                    if (!running.get()) {
                        return;
                    }
                }

                call = inbox.remove();
            } finally {
                lock.unlock();
            }

            MessageMetadata metadata = protocol.readMessageBegin();

            if (metadata.type == TMessageType.EXCEPTION) {
                ThriftException e = ThriftException.read(protocol);
                fail(call, e);
                protocol.readMessageEnd();
            } else if (metadata.type != TMessageType.REPLY) {
                throw new ThriftException(
                        ThriftException.Kind.INVALID_MESSAGE_TYPE,
                        "Invalid message type: " + metadata.type);
            }

            if (metadata.seqId != seqId.get()) {
                throw new ThriftException(
                        ThriftException.Kind.BAD_SEQUENCE_ID,
                        "Out-of-order response");
            }

            if (!metadata.name.equals(call.name)) {
                throw new ThriftException(
                        ThriftException.Kind.WRONG_METHOD_NAME,
                        "Unexpected method name in reply; expected " + call.name
                                + " but received " + metadata.name);
            }

            try {
                complete(call, call.receive(protocol, metadata));
            } catch (Exception e) {
                fail(call, e);
            } finally {
                protocol.readMessageEnd();
            }
        }
    }

    private void complete(final MethodCall call, final Object result) {
        callbackExecutor.submit(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                call.callback.onSuccess(result);
            }
        });
    }

    private void fail(final MethodCall<?> call, final Throwable error) {
        callbackExecutor.submit(new Runnable() {
            @Override
            public void run() {
                call.callback.onError(error);
            }
        });
    }
}
