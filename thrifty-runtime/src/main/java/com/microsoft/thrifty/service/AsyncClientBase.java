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

import com.microsoft.thrifty.Struct;
import com.microsoft.thrifty.ThriftException;
import com.microsoft.thrifty.protocol.Protocol;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

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
public class AsyncClientBase extends ClientBase implements Closeable {
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
     * A single-thread executor on which to invoke method callbacks.
     *
     * <p>I expect that we'll revisit this design choice; it guarantees
     * that method responses won't race each other, but arguably that's
     * a higher-level concern, and this does feel a bit heavy-handed.
     */
    private final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();

    /**
     * An unbounded queue holding RPC calls awaiting execution.
     */
    private final BlockingQueue<MethodCall<?>> pendingCalls = new LinkedBlockingQueue<>();

    private final Listener listener;
    private final WorkerThread workerThread;

    protected AsyncClientBase(Protocol protocol, Listener listener) {
        super(protocol);
        this.listener = listener;
        this.workerThread = new WorkerThread();

        workerThread.setDaemon(true);
        workerThread.start();
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

        if (!pendingCalls.offer(methodCall)) {
            // This should never happen with an unbounded queue
            throw new IllegalStateException("Call queue is full");
        }
    }

    @Override
    public void close() throws IOException {
        close(null);
    }

    private void close(final Throwable error) {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        workerThread.interrupt();

        closeProtocol();

        if (!pendingCalls.isEmpty()) {
            List<MethodCall<?>> incompleteCalls = new ArrayList<>();
            pendingCalls.drainTo(incompleteCalls);
            CancellationException e = new CancellationException();
            for (MethodCall<?> call : incompleteCalls) {
                try {
                    fail(call, e);
                } catch (Exception ignored) {
                    // nope
                }
            }
        }

        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (error != null) {
                    listener.onError(error);
                } else {
                    listener.onTransportClosed();
                }
            }
        });

        try {
            // Shut down, but let queued tasks finish.
            // Don't terminate!
            callbackExecutor.shutdown();
        } catch (Exception ignored) {
            // nope
        }
    }

    @SuppressWarnings("unchecked")
    private class WorkerThread extends Thread {
        @Override
        public void run() {
            Throwable error = null;
            while (running.get()) {
                try {
                    invokeRequest();
                } catch (Throwable e) {
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

        private void invokeRequest() throws ThriftException, IOException, InterruptedException {
            MethodCall<?> call = pendingCalls.take();
            if (!running.get()) {
                if (call != null) {
                    fail(call, new CancellationException());
                }
                return;
            }

            if (call == null) {
                return;
            }

            Object result = null;
            Exception error = null;
            try {
                result = AsyncClientBase.this.invokeRequest(call);
            } catch (IOException | RuntimeException e) {
                fail(call, e);
                throw e;
            } catch (ServerException e) {
                error = e.thriftException;
            } catch (Exception e) {
                if (e instanceof Struct) {
                    error = e;
                } else {
                    // invokeRequest should only throw one of the caught Exception types or
                    // an Exception extending Struct from MethodCall
                    throw new AssertionError("Unexpected exception", e);
                }
            }

            try {
                if (error != null) {
                    fail(call, error);
                } else {
                    complete(call, result);
                }
            } catch (RejectedExecutionException e) {
                // The client has been closed out from underneath; as there will
                // be no further use for this thread, no harm in running it
                // synchronously.
                if (error != null) {
                    call.callback.onError(error);
                } else {
                    //noinspection RedundantCast
                    ((MethodCall) call).callback.onSuccess(result);
                }
            }
        }
    }

    private void complete(final MethodCall call, final Object result) {
        callbackExecutor.execute(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                call.callback.onSuccess(result);
            }
        });
    }

    private void fail(final MethodCall<?> call, final Throwable error) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                call.callback.onError(error);
            }
        });
    }
}
