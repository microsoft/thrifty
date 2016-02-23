/*
 * Thrifty
 *
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED ON AN  *AS IS* BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING
 * WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE,
 * FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
 *
 * See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */
package com.microsoft.thrifty.service;

import com.microsoft.thrifty.ThriftException;
import com.microsoft.thrifty.protocol.MessageMetadata;
import com.microsoft.thrifty.protocol.Protocol;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
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
 * configure your {@link Protocol} and {@link com.microsoft.thrifty.transport.Transport}
 * objects appropriately.
 */
public class ClientBase implements Closeable {
    /**
     * Exposes important events in the client's lifecycle.
     */
    public interface Listener {
        /**
         * Invoked when the client connection has been closed.
         *
         * <p>After invocation, the client is no longer usable.  All subsequent
         * method call attempts will result in an immediate exception on the
         * calling thread.
         */
        void onTransportClosed();

        /**
         * Invoked when a client-level error has occurred.
         *
         * <p>This generally indicates a connectivity or protocol error,
         * and is distinct from errors returned as part of normal service
         * operation.
         *
         * <p>The client is guaranteed to have been closed and shut down
         * by the time this method is invoked.
         *
         * @param error the throwable instance representing the error.
         */
        void onError(Throwable error);
    }

    /**
     * A sequence ID generator; contains the most-recently-used
     * sequence ID (or zero, if no calls have been made).
     */
    private final AtomicInteger seqId = new AtomicInteger(0);

    /**
     * A flag indicating whether the client is active and connected.
     */
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * A single-thread executor on which to invoke method callbacks.
     *
     * <p>I expect that we'll revisit this design choice; it guarantees
     * that method responses won't race each other, but arguably that's
     * a higher-level concern, and this does feel a bit heavy-handed.
     */
    private final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();

    /**
     * A queue of method calls waiting to be sent to the server.
     */
    private final Queue<MethodCall<?>> outbox = new LinkedList<>();

    /**
     * A map of method calls awaiting response from the server,
     * indexed by the sequence ID generated when the call was
     * sent.
     */
    private final Map<Integer, MethodCall> inbox = new HashMap<>();

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

    /**
     * When invoked by a derived instance, places the given call in a queue to
     * be sent to the server.
     *
     * @param methodCall the remote method call to be invoked
     */
    protected void enqueue(MethodCall<?> methodCall) {
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
            int sid = seqId.incrementAndGet();

            protocol.writeMessageBegin(call.name, call.callTypeId, sid);
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
                    MethodCall<?> oldCall = inbox.put(sid, call);
                    if (oldCall != null) {
                        throw new IllegalStateException("Reused sequence ID! (id=" + sid + ")");
                    }
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
            MessageMetadata metadata = protocol.readMessageBegin();

            MethodCall call;
            lock.lock();
            try {
                call = inbox.remove(metadata.seqId);
            } finally {
                lock.unlock();
            }

            if (call == null) {
                throw new ThriftException(
                        ThriftException.Kind.BAD_SEQUENCE_ID,
                        "Unrecognized sequence ID");
            }

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
