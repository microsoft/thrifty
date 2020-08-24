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

import com.microsoft.thrifty.util.UnsafeByteArrayOutputStream
import java.io.EOFException

/**
 * A transport decorator that reads from and writes to the underlying transport
 * in length-prefixed frames.  Used when the server is using a non-blocking
 * implementation, which currently requires such framing.
 */
class FramedTransport(private val inner: Transport) : Transport {
    // Read state
    private var remainingBytes = 0

    // Write state
    private var pendingWrite: UnsafeByteArrayOutputStream? = null

    override fun close() {
        inner.close()
        pendingWrite = null
    }

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
        while (remainingBytes <= 0) {
            readHeader()
        }
        val toRead = Math.min(count, remainingBytes)
        val numRead = inner.read(buffer, offset, toRead)
        remainingBytes -= numRead
        return numRead
    }

    private fun readHeader() {
        val headerBytes = ByteArray(4)
        var numRead = 0
        while (numRead < headerBytes.size) {
            val n = inner.read(headerBytes, numRead, headerBytes.size - numRead)
            if (n == -1) {
                throw EOFException()
            }
            numRead += n
        }
        remainingBytes = (
                   ((headerBytes[0].toInt() and 0xFF) shl 24)
                or ((headerBytes[1].toInt() and 0xFF) shl 16)
                or ((headerBytes[2].toInt() and 0xFF) shl 8)
                or ( headerBytes[3].toInt() and 0xFF))
    }

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        if (pendingWrite == null) {
            pendingWrite = UnsafeByteArrayOutputStream(Math.max(count, 32))
        }
        pendingWrite!!.write(buffer, offset, count)
    }

    override fun flush() {
        val size = if (pendingWrite == null) 0 else pendingWrite!!.size()
        val headerBytes = ByteArray(4)
        headerBytes[0] = ((size shr 24) and 0xFF).toByte()
        headerBytes[1] = ((size shr 16) and 0xFF).toByte()
        headerBytes[2] = ((size shr 8)  and 0xFF).toByte()
        headerBytes[3] = ( size         and 0xFF).toByte()
        inner.write(headerBytes)
        if (size > 0) {
            inner.write(pendingWrite!!.buffer, 0, size)
            pendingWrite!!.reset()
        }
    }
}