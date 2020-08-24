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

import com.microsoft.thrifty.TType
import com.microsoft.thrifty.transport.BufferTransport
import com.microsoft.thrifty.util.ProtocolUtil.skip
import io.kotest.assertions.fail
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.contain
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import org.junit.Test
import java.io.IOException
import java.net.ProtocolException

class BinaryProtocolTest {
    @Test
    fun readString() {
        val buffer = Buffer()
        buffer.writeInt(3)
        buffer.writeUtf8("foo")
        val proto = BinaryProtocol(BufferTransport(buffer))
        proto.readString() shouldBe "foo"
    }

    @Test
    fun readStringGreaterThanLimit() {
        val buffer = Buffer()
        buffer.writeInt(13)
        buffer.writeUtf8("foobarbazquux")
        val proto = BinaryProtocol(BufferTransport(buffer), 12)
        try {
            proto.readString()
            fail("Expected a ProtocolException")
        } catch (e: ProtocolException) {
            e.message should contain("String size limit exceeded")
        }
    }

    @Test
    fun readBinary() {
        val buffer = Buffer()
        buffer.writeInt(4)
        buffer.writeUtf8("abcd")
        val proto = BinaryProtocol(BufferTransport(buffer))
        proto.readBinary() shouldBe "abcd".encodeUtf8()
    }

    @Test
    fun readBinaryGreaterThanLimit() {
        val buffer = Buffer()
        buffer.writeInt(6)
        buffer.writeUtf8("kaboom")
        val proto = BinaryProtocol(BufferTransport(buffer), 4)
        try {
            proto.readBinary()
            fail("Expected a ProtocolException")
        } catch (e: ProtocolException) {
            e.message should contain("Binary size limit exceeded")
        }
    }

    @Test
    fun writeByte() {
        val buffer = Buffer()
        val proto = BinaryProtocol(BufferTransport(buffer))
        proto.writeByte(127.toByte())
        buffer.readByte() shouldBe 127.toByte()
    }

    @Test
    fun writeI16() {
        val buffer = Buffer()
        val proto = BinaryProtocol(BufferTransport(buffer))
        proto.writeI16(Short.MAX_VALUE)
        buffer.readShort() shouldBe Short.MAX_VALUE

        // Make sure it's written big-endian
        buffer.clear()
        proto.writeI16(0xFF00.toShort())
        buffer.readShort() shouldBe 0xFF00.toShort()
    }

    @Test
    fun writeI32() {
        val buffer = Buffer()
        val proto = BinaryProtocol(BufferTransport(buffer))
        proto.writeI32(-0xf0ff01)
        buffer.readInt() shouldBe -0xf0ff01
    }

    @Test
    fun writeI64() {
        val buffer = Buffer()
        val proto = BinaryProtocol(BufferTransport(buffer))
        proto.writeI64(0x12345678)

        buffer.readLong() shouldBe 0x12345678
    }

    @Test
    fun writeDouble() {
        val buffer = Buffer()
        val proto = BinaryProtocol(BufferTransport(buffer))

        // Doubles go on the wire as the 8-byte blobs from
        // Double#doubleToLongBits().
        proto.writeDouble(Math.PI)
        buffer.readLong() shouldBe java.lang.Double.doubleToLongBits(Math.PI)
    }

    @Test
    fun writeString() {
        val buffer = Buffer()
        val proto = BinaryProtocol(BufferTransport(buffer))
        proto.writeString("here is a string")
        buffer.readInt() shouldBe 16
        buffer.readUtf8() shouldBe "here is a string"
    }

    @Test
    fun adapterTest() {
        // This test case comes from actual data, and is intended
        // to ensure in particular that readers don't grab more data than
        // they are supposed to.
        val payload = "030001000600" +
                "0200030600030002" +
                "0b00040000007f08" +
                "0001000001930600" +
                "0200a70b00030000" +
                "006b0e00010c0000" +
                "000206000100020b" +
                "0002000000243030" +
                "3030303030302d30" +
                "3030302d30303030" +
                "2d303030302d3030" +
                "3030303030303030" +
                "3031000600010001" +
                "0b00020000002430" +
                "613831356232312d" +
                "616533372d343966" +
                "622d616633322d31" +
                "3636363261616366" +
                "62333300000000"
        val binaryData: ByteString = payload.decodeHex()
        val buffer = Buffer()
        buffer.write(binaryData)
        val protocol = BinaryProtocol(BufferTransport(buffer))
        read(protocol)
    }

    @Throws(IOException::class)
    fun read(protocol: Protocol) {
        protocol.readStructBegin()
        while (true) {
            val field = protocol.readFieldBegin()
            if (field.typeId == TType.STOP) {
                break
            }
            when (field.fieldId.toInt()) {
                1 -> {
                    if (field.typeId == TType.BYTE) {
                        protocol.readByte()
                    } else {
                        skip(protocol, field.typeId)
                    }
                }
                2 -> {
                    if (field.typeId == TType.I16) {
                        protocol.readI16()
                    } else {
                        skip(protocol, field.typeId)
                    }
                }
                3 -> {
                    if (field.typeId == TType.I16) {
                        protocol.readI16()
                    } else {
                        skip(protocol, field.typeId)
                    }
                }
                4 -> {
                    if (field.typeId == TType.STRING) {
                        protocol.readBinary()
                    } else {
                        skip(protocol, field.typeId)
                    }
                }
                else -> {
                    skip(protocol, field.typeId)
                }
            }
            protocol.readFieldEnd()
        }
    }
}