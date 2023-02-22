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

import com.microsoft.thrifty.Struct
import com.microsoft.thrifty.ThriftException
import com.microsoft.thrifty.protocol.Protocol
import okio.Closeable

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
     * When invoked by a derived instance, places the given call in a queue to
     * be sent to the server.
     *
     * @param methodCall the remote method call to be invoked
     */
    protected actual fun enqueue(methodCall: MethodCall<*>) {
        TODO()
    }

    override fun close() {
        TODO()
    }
}
