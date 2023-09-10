/*
 * Thrifty
 *
 * Copyright (c) Benjamin Bader
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
package com.bendb.thrifty.service

import com.bendb.thrifty.Struct
import com.bendb.thrifty.ThriftException
import com.bendb.thrifty.protocol.Protocol
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException

/**
 * Implements a basic service client that executes methods asynchronously.
 *
 * Note that, while the client-facing API of this class is callback-based,
 * the implementation itself is **blocking**.  Unlike the Apache
 * implementation, there is no presumption made here about framed encoding
 * at the transport level.  If your backend requires framing, be sure to
 * configure your [Protocol] and [com.bendb.thrifty.transport.Transport]
 * objects appropriately.
 */
@Suppress("UNCHECKED_CAST")
actual open class AsyncClientBase protected actual constructor(
        protocol: Protocol,
        private val listener: Listener
) : ClientBase(protocol), Closeable {
    /**
     * Exposes important events in the client's lifecycle.
     */
    actual interface Listener {
        /**
         * Invoked when the client connection has been closed.
         *
         *
         * After invocation, the client is no longer usable.  All subsequent
         * method call attempts will result in an immediate exception on the
         * calling thread.
         */
        actual fun onTransportClosed()

        /**
         * Invoked when a client-level error has occurred.
         *
         *
         * This generally indicates a connectivity or protocol error,
         * and is distinct from errors returned as part of normal service
         * operation.
         *
         *
         * The client is guaranteed to have been closed and shut down
         * by the time this method is invoked.
         *
         * @param error the throwable instance representing the error.
         */
        actual fun onError(error: Throwable)
    }

    /**
     * A single-thread executor on which to invoke method callbacks.
     *
     *
     * I expect that we'll revisit this design choice; it guarantees
     * that method responses won't race each other, but arguably that's
     * a higher-level concern, and this does feel a bit heavy-handed.
     */
    private val callbackExecutor = Executors.newSingleThreadExecutor()

    /**
     * An unbounded queue holding RPC calls awaiting execution.
     */
    private val pendingCalls: BlockingQueue<MethodCall<*>> = LinkedBlockingQueue()
    private val workerThread: WorkerThread

    /**
     * When invoked by a derived instance, places the given call in a queue to
     * be sent to the server.
     *
     * @param methodCall the remote method call to be invoked
     */
    protected actual fun enqueue(methodCall: MethodCall<*>) {
        check(running.get()) { "Cannot write to a closed service client" }
        check(pendingCalls.offer(methodCall)) {
            // This should never happen with an unbounded queue
            "Call queue is full"
        }
    }

    @Throws(IOException::class)
    override fun close() {
        close(null)
    }

    private fun close(error: Throwable?) {
        if (!running.compareAndSet(true, false)) {
            return
        }
        workerThread.interrupt()
        closeProtocol()
        if (!pendingCalls.isEmpty()) {
            val incompleteCalls = mutableListOf<MethodCall<*>>()
            pendingCalls.drainTo(incompleteCalls)
            val e = CancellationException()
            for (call in incompleteCalls) {
                try {
                    fail(call, e)
                } catch (ignored: Exception) {
                    // nope
                }
            }
        }
        callbackExecutor.execute {
            if (error != null) {
                listener.onError(error)
            } else {
                listener.onTransportClosed()
            }
        }
        try {
            // Shut down, but let queued tasks finish.
            // Don't terminate!
            callbackExecutor.shutdown()
        } catch (ignored: Exception) {
            // nope
        }
    }

    private inner class WorkerThread : Thread() {
        override fun run() {
            var error: Throwable? = null
            while (running.get()) {
                try {
                    invokeRequest()
                } catch (e: Throwable) {
                    error = e
                    break
                }
            }
            try {
                close(error)
            } catch (ignored: Throwable) {
                // nope
            }
        }

        @Throws(ThriftException::class, IOException::class, InterruptedException::class)
        private fun invokeRequest() {
            val call = pendingCalls.take()
            if (!running.get()) {
                fail(call, CancellationException())
                return
            }

            var result: Any? = null
            var error: Exception? = null
            try {
                result = this@AsyncClientBase.invokeRequest(call)
            } catch (e: IOException) {
                fail(call, e)
                throw e
            } catch (e: RuntimeException) {
                fail(call, e)
                throw e
            } catch (e: ServerException) {
                error = e.thriftException
            } catch (e: Exception) {
                error = if (e is Struct) {
                    e
                } else {
                    // invokeRequest should only throw one of the caught Exception types or
                    // an Exception extending Struct from MethodCall
                    throw AssertionError("Unexpected exception", e)
                }
            }

            try {
                if (error != null) {
                    fail(call, error)
                } else {
                    complete(call, result)
                }
            } catch (e: RejectedExecutionException) {
                // The client has been closed out from underneath; as there will
                // be no further use for this thread, no harm in running it
                // synchronously.
                if (error != null) {
                    call.callback!!.onError(error)
                } else {
                    (call.callback as ServiceMethodCallback<Any?>).onSuccess(result)
                }
            }
        }
    }

    private fun complete(call: MethodCall<*>, result: Any?) {
        callbackExecutor.execute { (call.callback as ServiceMethodCallback<Any?>).onSuccess(result) }
    }

    private fun fail(call: MethodCall<*>, error: Throwable) {
        callbackExecutor.execute { call.callback!!.onError(error) }
    }

    init {
        workerThread = WorkerThread()
        workerThread.isDaemon = true
        workerThread.start()
    }
}
