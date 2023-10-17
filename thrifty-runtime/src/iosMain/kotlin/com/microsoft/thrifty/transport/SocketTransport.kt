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

import okio.IOException
import platform.Network.nw_connection_t

actual class SocketTransport actual constructor(
    builder: Builder,
) : Transport {
    private val host = builder.host
    private val port = builder.port
    private val connectTimeout: Long = builder.connectTimeout.toLong()
    private val readTimeout: Long = builder.readTimeout.toLong()
    private val tls = builder.useTransportSecurity

    private var socket: NwSocket? = null

    /**
     * Just here for testing
     */
    internal constructor(connection: nw_connection_t) : this(Builder("", 0)) {
        socket = NwSocket(connection, 0L)
    }

    actual class Builder actual constructor(
        val host: String,
        val port: Int,
    ) {
        var connectTimeout: Int = 0
        var readTimeout: Int = 0
        var useTransportSecurity: Boolean = false

        actual fun connectTimeout(connectTimeout: Int): Builder {
            this.connectTimeout = maxOf(connectTimeout, 0)
            return this
        }

        actual fun readTimeout(readTimeout: Int): Builder {
            this.readTimeout = maxOf(readTimeout, 0)
            return this
        }

        actual fun enableTls(enableTls: Boolean): Builder {
            this.useTransportSecurity = enableTls
            return this
        }

        actual fun build(): SocketTransport {
            return SocketTransport(this)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
        return socket!!.read(buffer, offset, count)
    }

    override fun write(data: ByteArray) {
        write(data, 0, data.size)
    }

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        require(offset >= 0)
        require(count >= 0)
        require(count <= buffer.size - offset)
        socket!!.write(buffer, offset, count)
    }

    override fun flush() {
        // no-op?
        socket?.flush()
    }

    override fun close() {
        socket?.close()
    }

    @Throws(IOException::class)
    actual fun connect() {
        socket = NwSocket.connect(
            host = host,
            port = port,
            enableTls = tls,
            sendTimeoutMillis = readTimeout,
            connectTimeoutMillis = connectTimeout
        )
    }
}
