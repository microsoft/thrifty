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
package com.microsoft.thrifty.protocol

import com.microsoft.thrifty.transport.BufferTransport
import io.kotest.matchers.shouldBe
import okio.Buffer
import org.junit.jupiter.api.Test
import java.io.IOException

class CompactProtocolTest {
    @Test
    @Throws(IOException::class)
    fun varint32() {
        val buffer = Buffer()
        val transport = BufferTransport(buffer)
        val protocol = CompactProtocol(transport)

        protocol.writeI32(0)
        buffer.readByteArray() shouldBe byteArrayOf(0)

        protocol.writeI32(1)
        buffer.readByteArray() shouldBe byteArrayOf(2)

        protocol.writeI32(7)
        buffer.readByteArray() shouldBe byteArrayOf(14)

        protocol.writeI32(150)
        buffer.readByteArray() shouldBe byteArrayOf(172.toByte(), 2)

        protocol.writeI32(15000)
        buffer.readByteArray() shouldBe byteArrayOf(176.toByte(), 234.toByte(), 1)

        protocol.writeI32(0xFFFF)
        buffer.readByteArray() shouldBe byteArrayOf(254.toByte(), 255.toByte(), 7)

        protocol.writeI32(0xFFFFFF)
        buffer.readByteArray() shouldBe byteArrayOf(254.toByte(), 255.toByte(), 255.toByte(), 15)

        protocol.writeI32(-1)
        buffer.readByteArray() shouldBe byteArrayOf(1)

        protocol.writeI32(-7)
        buffer.readByteArray() shouldBe byteArrayOf(13)

        protocol.writeI32(-150)
        buffer.readByteArray() shouldBe byteArrayOf(171.toByte(), 2)

        protocol.writeI32(-15000)
        buffer.readByteArray() shouldBe byteArrayOf(175.toByte(), 234.toByte(), 1)

        protocol.writeI32(-0xFFFF)
        buffer.readByteArray() shouldBe byteArrayOf(253.toByte(), 255.toByte(), 7)

        protocol.writeI32(-0xFFFFFF)
        buffer.readByteArray() shouldBe byteArrayOf(253.toByte(), 255.toByte(), 255.toByte(), 15)
    }

    @Test
    @Throws(Exception::class)
    fun roundtrip() {
        val xtruct = Xtruct.Builder()
                .byte_thing(254.toByte())
                .i32_thing(0xFFFF)
                .i64_thing(0xFFFFFFFFL)
                .string_thing("foo")
                .double_thing(Math.PI)
                .bool_thing(true)
                .build()
        val buffer = Buffer()
        val transport = BufferTransport(buffer)
        val proto = CompactProtocol(transport)
        Xtruct.ADAPTER.write(proto, xtruct)
        Xtruct.ADAPTER.read(CompactProtocol(transport)) shouldBe xtruct
    }
}
