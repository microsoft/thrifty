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
package com.bendb.thrifty

import com.bendb.thrifty.protocol.BinaryProtocol
import com.bendb.thrifty.protocol.CompactProtocol
import com.bendb.thrifty.protocol.JsonProtocol
import com.bendb.thrifty.protocol.SimpleJsonProtocol
import com.bendb.thrifty.transport.BufferTransport
import com.bendb.thrifty.transport.Transport
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource

/**
 * Creates a transport backed by the given [Buffer].
 *
 * @receiver the [Buffer] backing the new transport.
 * @return a transport that reads from/writes to the buffer.
 */
fun Buffer.transport() = BufferTransport(this)

/**
 * Creates a read-only transport from the given [BufferedSource].
 *
 * @receiver the source underlying the new transport.
 * @return a read-only transport.
 */
fun <S : BufferedSource> S.transport() = object : Transport {
    private val self = this@transport

    override fun close() = self.close()

    override fun read(buffer: ByteArray, offset: Int, count: Int) = self.read(buffer, offset, count)

    override fun write(data: ByteArray) = error("read-only transport")

    override fun write(buffer: ByteArray, offset: Int, count: Int) = error("read-only transport")

    override fun flush() {
        // No-op
    }
}

/**
 * Creates a write-only transport from the given [BufferedSink]
 *
 * @receiver the sink underlying the new transport.
 * @return a write-only transport.
 */
fun <S : BufferedSink> S.transport() = object : Transport {
    private val self = this@transport

    override fun close() = self.close()

    override fun read(buffer: ByteArray, offset: Int, count: Int) = error("write-only transport")

    override fun write(data: ByteArray) { self.write(data) }

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        self.write(buffer, offset, count)
    }

    override fun flush() = self.flush()
}

/**
 * Creates a [BinaryProtocol] from the given [Transport].
 */
fun <T : Transport> T.binaryProtocol() = BinaryProtocol(this)

/**
 * Creates a [CompactProtocol] from the given [Transport].
 */
fun <T : Transport> T.compactProtocol() = CompactProtocol(this)

/**
 * Creates a [JsonProtocol] from the given [Transport].
 */
fun <T : Transport> T.jsonProtocol() = JsonProtocol(this)

/**
 * Creates a [SimpleJsonProtocol] from the given [Transport].
 */
fun <T : Transport> T.simpleJsonProtocol() = SimpleJsonProtocol(this)
