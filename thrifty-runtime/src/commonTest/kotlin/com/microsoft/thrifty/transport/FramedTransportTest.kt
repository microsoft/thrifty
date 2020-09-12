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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import okio.Buffer
import okio.EOFException
import kotlin.test.Test

class FramedTransportTest {
    @Test
    fun sinkWritesFrameLength() {
        val buffer = Buffer()
        val bufferTransport = BufferTransport(buffer)
        val transport = FramedTransport(bufferTransport)
        transport.write("abcde".encodeToByteArray())
        transport.flush()

        buffer.readInt() shouldBe 5
        buffer.readUtf8() shouldBe "abcde"
    }

    @Test
    fun sourceReadsFrameLength() {
        val buffer = Buffer()
        buffer.writeInt(5)
        buffer.writeUtf8("abcdefghij") // buffer.size() is now 14
        val transport = FramedTransport(BufferTransport(buffer))
        val readBuffer = ByteArray(5)

        transport.read(readBuffer, 0, 5) shouldBe 5
        buffer.size shouldBe 5L
        readBuffer.decodeToString() shouldBe "abcde"
    }

    @Test
    fun flushedDataBeginsWithFrameLength() {
        val target = Buffer()
        val source = Buffer()
        val transport = FramedTransport(BufferTransport(target))
        source.writeUtf8("this text contains thirty-seven bytes")
        transport.write(source.readByteArray())
        transport.flush()

        target.size shouldBe 41L
        target.readInt() shouldBe 37
        target.readUtf8() shouldBe "this text contains thirty-seven bytes"
    }

    @Test
    fun readsSpanningMultipleFrames() {
        val buffer = Buffer()
        buffer.writeInt(6)
        buffer.writeUtf8("abcdef")
        buffer.writeInt(4)
        buffer.writeUtf8("ghij")
        val transport = FramedTransport(BufferTransport(buffer))
        val readBuffer = ByteArray(10)

        transport.read(readBuffer, 0, 10) shouldBe 6
        transport.read(readBuffer, 6, 4) shouldBe 4
        readBuffer.decodeToString() shouldBe "abcdefghij"
    }

    @Test
    fun readHeaderWhenEOFReached() {
        val buffer = Buffer()
        val transport = FramedTransport(BufferTransport(buffer))
        val readBuffer = ByteArray(10)
        shouldThrow<EOFException> { transport.read(readBuffer, 0, 10) }
    }
}
