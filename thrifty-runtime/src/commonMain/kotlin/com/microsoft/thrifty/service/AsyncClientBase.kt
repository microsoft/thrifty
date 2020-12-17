package com.microsoft.thrifty.service

import com.microsoft.thrifty.internal.Closeable
import com.microsoft.thrifty.protocol.Protocol

/**
 * Implements a basic service client that executes methods asynchronously.
 *
 * Note that, while the client-facing API of this class is callback-based,
 * the implementation itself is **blocking**.  Unlike the Apache
 * implementation, there is no presumption made here about framed encoding
 * at the transport level.  If your backend requires framing, be sure to
 * configure your [Protocol] and [com.microsoft.thrifty.transport.Transport]
 * objects appropriately.
 *
 * @param protocol the [Protocol] used to encode/decode requests and responses.
 * @param listener a callback object to receive client-level events.
 */
expect open class AsyncClientBase protected constructor(
    protocol: Protocol,
    listener: Listener
) : ClientBase, Closeable {
    /**
     * Exposes important events in the client's lifecycle.
     */
    interface Listener {
        /**
         * Invoked when the client connection has been closed.
         *
         * After invocation, the client is no longer usable.  All subsequent
         * method call attempts will result in an immediate exception on the
         * calling thread.
         */
        fun onTransportClosed()

        /**
         * Invoked when a client-level error has occurred.
         *
         * This generally indicates a connectivity or protocol error,
         * and is distinct from errors returned as part of normal service
         * operation.
         *
         * The client is guaranteed to have been closed and shut down
         * by the time this method is invoked.
         *
         * @param error the throwable instance representing the error.
         */
        fun onError(error: Throwable)
    }

    /**
     * Enqueues a method call for asynchronous execution.
     *
     * WARNING:
     * This method is *NOT* part of the public API.  It is an implementation
     * detail, for use by generated code only.  As multi-platform code evolves,
     * expect this to change and/or be removed entirely!
     */
    protected fun enqueue(methodCall: MethodCall<*>)
}