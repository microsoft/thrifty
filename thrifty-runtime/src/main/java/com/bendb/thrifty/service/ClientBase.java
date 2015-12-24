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
import java.net.ProtocolException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final Map<Integer, MethodCall<?>> calls = new HashMap<>();
    private final AtomicInteger seqId = new AtomicInteger(0);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();

    private final Queue<MethodCall<?>> outbox = new LinkedList<>();

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
            throw new IOException("Cannot write to a closed service client");
        }

        methodCall.sequenceId = seqId.incrementAndGet();

        synchronized (outbox) {
            outbox.add(methodCall);
            outbox.notify();
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

        reader.interrupt();
        writer.interrupt();

        outbox.notifyAll();
        calls.notifyAll();

        outbox.clear();
        calls.clear();

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
                } catch (InterruptedException e) {
                    // Either we were closed, in which case transition normally
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
        @Override
        void act() throws Exception {
            MethodCall call;
            synchronized (outbox) {
                while (outbox.isEmpty()) {
                    outbox.wait();
                }
                call = outbox.remove();
            }

            if (!running.get()) {
                return;
            }

            boolean isOneWay = call.callTypeId == TMessageType.ONEWAY;

            // Stash the call by its sequence ID, if we expect that it will
            // have a response.
            if (!isOneWay) {
                synchronized (calls) {
                    calls.put(call.sequenceId, call);
                }
            }

            protocol.writeMessageBegin(call.name, call.callTypeId, call.sequenceId);
            call.send(protocol);
            protocol.writeMessageEnd();

            // Small messages may be lingering in a send buffer, but too
            // many flushes are not good.  As a first guess at an heuristic
            // to improve latency, only flush if there are no more queued
            // method calls.
            boolean hasMoreCalls;
            synchronized (outbox) {
                hasMoreCalls = !outbox.isEmpty();
            }

            if (!hasMoreCalls) {
                protocol.flush();
            }

            if (isOneWay) {
                // null is always safe to pass here - oneway methods
                // are guaranteed to be Void anyways.
                //noinspection unchecked
                call.callback.onSuccess(null);
            }
        }
    }

    private class ReaderThread extends RunLoop {
        @Override
        void act() throws Exception {
            MessageMetadata metadata = protocol.readMessageBegin();

            final MethodCall call;
            synchronized (calls) {
                call = calls.remove(metadata.seqId);
            }

            if (call == null || !call.name.equals(metadata.name)) {
                throw new ThriftException(ThriftException.Kind.BAD_SEQUENCE_ID, "Out-of-order response");
            }

            if (metadata.type == TMessageType.EXCEPTION) {
                fail(call, ThriftException.read(protocol));
            } else if (metadata.type == TMessageType.REPLY) {
                try {
                    Object result = call.receive(protocol, metadata);
                    complete(call, result);
                } catch (Exception e) {
                    fail(call, e);
                }
            } else {
                ThriftException e = new ThriftException(
                        ThriftException.Kind.INVALID_MESSAGE_TYPE,
                        "Invalid message type: " + metadata.type);
                fail(call, e);
            }

            protocol.readMessageEnd();
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
}
