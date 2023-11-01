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
package com.microsoft.thrifty.transport

import KT62102Workaround.dispatch_get_target_default_queue
import KT62102Workaround.nw_connection_send_with_default_context
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.Pinned
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import okio.Closeable
import okio.IOException
import platform.Network.nw_connection_cancel
import platform.Network.nw_connection_create
import platform.Network.nw_connection_receive
import platform.Network.nw_connection_set_queue
import platform.Network.nw_connection_set_state_changed_handler
import platform.Network.nw_connection_start
import platform.Network.nw_connection_state_cancelled
import platform.Network.nw_connection_state_failed
import platform.Network.nw_connection_state_invalid
import platform.Network.nw_connection_state_preparing
import platform.Network.nw_connection_state_ready
import platform.Network.nw_connection_state_t
import platform.Network.nw_connection_state_waiting
import platform.Network.nw_connection_t
import platform.Network.nw_endpoint_create_host
import platform.Network.nw_error_domain_dns
import platform.Network.nw_error_domain_invalid
import platform.Network.nw_error_domain_posix
import platform.Network.nw_error_domain_t
import platform.Network.nw_error_domain_tls
import platform.Network.nw_error_get_error_code
import platform.Network.nw_error_get_error_domain
import platform.Network.nw_error_t
import platform.Network.nw_parameters_copy_default_protocol_stack
import platform.Network.nw_parameters_create
import platform.Network.nw_protocol_stack_prepend_application_protocol
import platform.Network.nw_protocol_stack_set_transport_protocol
import platform.Network.nw_tcp_create_options
import platform.Network.nw_tcp_options_set_connection_timeout
import platform.Network.nw_tcp_options_set_no_delay
import platform.Network.nw_tls_create_options
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_data_apply
import platform.darwin.dispatch_data_create
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_t
import platform.darwin.dispatch_semaphore_wait
import platform.darwin.dispatch_time
import platform.darwin.dispatch_time_t
import platform.posix.QOS_CLASS_DEFAULT
import platform.posix.intptr_t
import platform.posix.memcpy
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalForeignApi::class)
class NwSocket(
    private val conn: nw_connection_t,
    private val readWriteTimeoutMillis: Long,
) : Closeable {
    private val isConnected = atomic(true)
    private val lastError = atomic<nw_error_t>(null)

    init {
        nw_connection_set_state_changed_handler(conn, this::handleStateChange)
    }

    fun read(buffer: ByteArray, offset: Int = 0, count: Int = buffer.size): Int {
        require(offset >= 0)
        require(count >= 0)
        require(offset + count <= buffer.size)

        check(isConnected.value) { "Socket not connected" }

        buffer.usePinned { pinned ->
            var totalRead = 0
            while (totalRead < count) {
                val numRead = readOneChunk(pinned, offset + totalRead, count - totalRead)

                if (numRead == 0) {
                    break
                }

                totalRead += numRead
            }

            return totalRead
        }
    }

    @Throws(IOException::class)
    private fun readOneChunk(pinned: Pinned<ByteArray>, offset: Int, count: Int): Int {
        val sem = dispatch_semaphore_create(0)
        var networkError: nw_error_t = null
        var numRead = 0

        nw_connection_receive(
            connection = conn,
            minimum_incomplete_length = 0.convert(),
            maximum_length = count.convert()
        ) { contents, _, _, error ->
            dispatch_data_apply(contents) { _, _, dataPtr, size ->
                memcpy(pinned.addressOf(offset + numRead), dataPtr, size)
                numRead += size.toInt()
                true // keep going
            }

            networkError = error

            dispatch_semaphore_signal(sem)
        }

        if (!sem.waitWithTimeout(readWriteTimeoutMillis)) {
            val e = IOException("Timed out waiting for read")
            println(e.stackTraceToString())
            throw e
        }

        networkError?.throwError()

        return numRead
    }

    fun write(buffer: ByteArray, offset: Int = 0, count: Int = buffer.size) {
        require(offset >= 0)
        require(count >= 0)
        require(offset + count <= buffer.size)

        check(isConnected.value) { "Socket not connected" }

        buffer.usePinned { pinned ->
            val sem = dispatch_semaphore_create(0)
            val toWrite = dispatch_data_create(
                buffer = pinned.addressOf(offset),
                size = count.convert(),
                queue = dispatch_get_target_default_queue(), // Our own method, see KT62102Workaround
                destructor = ::noopDispatchBlock
            )

            var err: nw_error_t = null
            nw_connection_send_with_default_context(
                connection = conn,
                content = toWrite,
                is_complete = false
            ) { networkError ->
                err = networkError
                dispatch_semaphore_signal(sem)
            }

            if (!sem.waitWithTimeout(readWriteTimeoutMillis)) {
                throw IOException("Timed out waiting for write")
            }

            if (err != null) {
                err.throwError()
            }
        }
    }

    fun flush() {
        // no-op?
    }

    override fun close() {
        nw_connection_cancel(conn)
    }

    private fun handleStateChange(state: nw_connection_state_t, networkError: nw_error_t) {
        // If there isn't a last-error value already, set this one.
        lastError.compareAndSet(null, networkError)

        when (state) {
            nw_connection_state_invalid -> { }

            nw_connection_state_waiting -> { }

            nw_connection_state_preparing -> { }

            nw_connection_state_ready -> {
                isConnected.value = true
            }

            nw_connection_state_failed -> {
                isConnected.value = false
                nw_connection_set_state_changed_handler(conn, null)
            }

            nw_connection_state_cancelled -> {
                isConnected.value = false
                nw_connection_set_state_changed_handler(conn, null)
            }

            else -> {
                println("Unexpected nw_connection_state_t value: $state")
            }
        }
    }

    companion object {
        private val INTPTR_ZERO = 0.convert<intptr_t>()

        fun connect(
            host: String,
            port: Int,
            enableTls: Boolean,
            sendTimeoutMillis: Long = 0,
            connectTimeoutMillis: Long = 0,
        ): NwSocket {
            // Network.framework, at the C level, is a little tedious to use.
            // Rather than a sockaddr_t and a socket descriptor, there are relatively
            // more "things".  We've got to set up an endpoint, then connection parameters,
            // then TCP options, then TLS options, and finally a connection.
            // The remainder of the weirdness is ours, since we're using semaphores
            // to make this asynchronous API into a synchronous one.
            require(connectTimeoutMillis >= 0L) { "negative connect timeouts are not supported" }
            require(sendTimeoutMillis >= 0L) { "negative send timeouts are not supported" }

            val endpoint = nw_endpoint_create_host(host, "$port") ?: error("Invalid host/port: $host:$port")

            val parameters = nw_parameters_create()
            val stack = nw_parameters_copy_default_protocol_stack(parameters)

            val tcpOptions = nw_tcp_create_options()
            if (connectTimeoutMillis != 0L) {
                nw_tcp_options_set_connection_timeout(
                    tcpOptions,
                    maxOf(1, connectTimeoutMillis / 1000).convert()
                )
            }
            nw_tcp_options_set_no_delay(tcpOptions, true)
            nw_protocol_stack_set_transport_protocol(stack, tcpOptions)

            if (enableTls) {
                val tlsOptions = nw_tls_create_options()
                nw_protocol_stack_prepend_application_protocol(stack, tlsOptions)
            }

            val connection = nw_connection_create(endpoint, parameters) ?: error("Unable to create connection")
            val globalQueue = dispatch_get_global_queue(QOS_CLASS_DEFAULT.convert(), 0.convert())
            nw_connection_set_queue(connection, globalQueue)

            val sem = dispatch_semaphore_create(0)
            val didConnect = atomic(false)
            val connectionError = atomic<nw_error_t>(null)

            nw_connection_set_state_changed_handler(connection) { state, error ->
                if (error != null) {
                    connectionError.value = error
                }

                if (state == nw_connection_state_ready) {
                    didConnect.value = true
                }

                if (state in setOf(nw_connection_state_ready, nw_connection_state_failed, nw_connection_state_cancelled)) {
                    dispatch_semaphore_signal(sem)
                }
            }

            nw_connection_start(connection)
            val finishedInTime = sem.waitWithTimeout(connectTimeoutMillis)

            if (connectionError.value != null) {
                nw_connection_cancel(connection)
                connectionError.value.throwError("Error connecting to $host:$port")
            }

            if (!finishedInTime) {
                nw_connection_cancel(connection)
                throw IOException("Timed out connecting to $host:$port")
            }

            if (didConnect.value) {
                return NwSocket(connection, sendTimeoutMillis)
            }

            throw IOException("Failed to connect, but got no error")
        }

        /**
         * A function, usable as a [platform.darwin.dispatch_block_t], that does nothing.
         *
         * When used with [dispatch_data_create], this block causes the data
         * *not* to be copied.  This is what we want, since we're using semaphores
         * to wait for write completion, and we can guarantee that our memory
         * outlives the dispatch_data_t that wraps it.
         */
        private fun noopDispatchBlock() {}

        /**
         * Returns true if the semaphore was signaled, false if it timed out.
         */
        private fun dispatch_semaphore_t.waitWithTimeout(timeoutMillis: Long): Boolean {
            return dispatch_semaphore_wait(this, computeTimeout(timeoutMillis)) == INTPTR_ZERO
        }

        private fun computeTimeout(timeoutMillis: Long): dispatch_time_t {
            return if (timeoutMillis == 0L) {
                DISPATCH_TIME_FOREVER
            } else {
                val nanos = timeoutMillis.milliseconds.inWholeNanoseconds
                dispatch_time(DISPATCH_TIME_NOW, nanos)
            }
        }

        private fun nw_error_t.throwError(message: String? = null): Nothing {
            val domain = nw_error_get_error_domain(this)
            val code = nw_error_get_error_code(this)
            val errorBody = message ?: "Network error"
            throw IOException("$errorBody: $this (domain=${domain.name} code=$code)")
        }

        private val nw_error_domain_t.name: String
            get() = when (this) {
                nw_error_domain_dns -> "dns"
                nw_error_domain_tls -> "tls"
                nw_error_domain_posix -> "posix"
                nw_error_domain_invalid -> "invalid"
                else -> "$this"
            }
    }
}
