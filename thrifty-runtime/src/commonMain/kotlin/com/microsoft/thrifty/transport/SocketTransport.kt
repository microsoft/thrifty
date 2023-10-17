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

expect class SocketTransport internal constructor(
    builder: Builder
) : Transport {
    class Builder(host: String, port: Int) {
        /**
         * The number of milliseconds to wait for a connection to be established.
         */
        fun connectTimeout(connectTimeout: Int): Builder

        /**
         * The number of milliseconds a read operation should wait for completion.
         */
        fun readTimeout(readTimeout: Int): Builder

        /**
         * Enable TLS for this connection.
         */
        fun enableTls(enableTls: Boolean): Builder

        fun build(): SocketTransport
    }

    @Throws(okio.IOException::class)
    fun connect()
}
