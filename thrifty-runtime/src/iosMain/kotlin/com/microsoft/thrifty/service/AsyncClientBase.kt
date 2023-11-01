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
package com.microsoft.thrifty.service

import KT62102Workaround.dispatch_attr_serial
import com.microsoft.thrifty.Struct
import com.microsoft.thrifty.ThriftException
import com.microsoft.thrifty.protocol.Protocol
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import okio.Closeable
import okio.IOException
import platform.darwin.DISPATCH_QUEUE_SERIAL
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_suspend
import platform.posix.QOS_CLASS_USER_INITIATED
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implements a basic service client that executes methods asynchronously.
 *
 * Note that, while the client-facing API of this class is callback-based,
 * the implementation itself is **blocking**.  Unlike the Apache
 * implementation, there is no presumption made here about framed encoding
 * at the transport level.  If your backend requires framing, be sure to
 * configure your [Protocol] and [com.microsoft.thrifty.transport.Transport]
 * objects appropriately.
 */
@OptIn(ExperimentalForeignApi::class)
actual open class AsyncClientBase protected actual constructor(
    protocol: Protocol,
    private val listener: Listener
) : ClientBase(protocol), Closeable {

    private val closed = atomic(false)
    private var queue = dispatch_queue_create("client-queue", dispatch_attr_serial())
    private val pendingCalls = mutableSetOf<MethodCall<*>>()


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
     * When invoked by a derived instance, places the given call in a queue to
     * be sent to the server.
     *
     * @param methodCall the remote method call to be invoked
     */
    protected actual fun enqueue(methodCall: MethodCall<*>) {
        check(!closed.value) { "Client has been closed" }

        pendingCalls.add(methodCall)
        dispatch_async(queue) {
            pendingCalls.remove(methodCall)

            if (closed.value) {
                methodCall.callback?.onError(CancellationException("Client has been closed"))
                return@dispatch_async
            }

            var result: Any? = null
            var error: Exception? = null
            try {
                result = invokeRequest(methodCall)
            } catch (e: IOException) {
                fail(methodCall, e)
                close(e)
                return@dispatch_async
            } catch (e: RuntimeException) {
                fail(methodCall, e)
                close(e)
                return@dispatch_async
            } catch (e: ServerException) {
                error = e.thriftException
            } catch (e: Exception) {
                if (e is Struct) {
                    error = e
                } else {
                    throw AssertionError("wat")
                }
            }

            if (error != null) {
                fail(methodCall, error)
            } else {
                complete(methodCall, result)
            }
        }
    }

    override fun close() = close(error = null)

    private fun close(error: Exception?) {
        if (closed.getAndSet(true)) {
            return
        }

        dispatch_suspend(queue)
        queue = null

        for (call in pendingCalls) {
            val e = error ?: CancellationException("Client has been closed")
            fail(call, e)
        }

        dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED.convert(), 0.convert())) {
            if (error != null) {
                listener.onError(error)
            } else {
                listener.onTransportClosed()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun complete(call: MethodCall<*>, result: Any?) {
        val q = dispatch_get_global_queue(QOS_CLASS_USER_INITIATED.convert(), 0.convert())
        dispatch_async(q) {
            val callback = call.callback as ServiceMethodCallback<Any?>?
            callback?.onSuccess(result)
        }
    }

    private fun fail(call: MethodCall<*>, exception: Exception) {
        val q = dispatch_get_global_queue(QOS_CLASS_USER_INITIATED.convert(), 0.convert())
        dispatch_async(q) {
            call.callback?.onError(exception)
        }
    }
}
