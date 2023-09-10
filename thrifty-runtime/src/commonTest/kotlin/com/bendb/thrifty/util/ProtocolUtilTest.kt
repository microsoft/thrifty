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
package com.bendb.thrifty.util

import com.bendb.thrifty.TType
import com.bendb.thrifty.internal.ProtocolException
import com.bendb.thrifty.protocol.BinaryProtocol
import com.bendb.thrifty.protocol.Xtruct
import com.bendb.thrifty.transport.BufferTransport
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.throwable.shouldHaveMessage
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test

class ProtocolUtilTest {
    private val buffer = Buffer()
    private val protocol = BinaryProtocol(BufferTransport(buffer))

    @Test
    fun skipConsumesLists() {
        val strings = listOf("foo", "bar", "baz", "quux")
        protocol.writeListBegin(TType.STRING, strings.size)
        for (string in strings) {
            protocol.writeString(string)
        }
        protocol.writeListEnd()
        ProtocolUtil.skip(protocol, TType.LIST)
        buffer.size shouldBe 0L
    }

    @Test
    fun skipSets() {
        val set = setOf(
                "hello there".encodeUtf8(),
                "here is some more test data".encodeUtf8(),
                "take it respectfully!".encodeUtf8(),
        )
        protocol.writeSetBegin(TType.STRING, set.size)
        for (bytes in set) {
            protocol.writeBinary(bytes)
        }
        protocol.writeSetEnd()
        ProtocolUtil.skip(protocol, TType.SET)
        buffer.size shouldBe 0L
    }

    @Test
    fun skipConsumesMap() {
        val map = mapOf(
                1 to 10L,
                2 to 20L,
                4 to 30L,
                8 to 40L,
        )
        protocol.writeMapBegin(TType.I32, TType.I64, map.size)
        for ((key, value) in map) {
            protocol.writeI32(key)
            protocol.writeI64(value)
        }
        protocol.writeMapEnd()
        ProtocolUtil.skip(protocol, TType.MAP)
        buffer.size shouldBe 0L
    }

    @Test
    fun skipConsumesStructs() {
        val struct = Xtruct.Builder()
                .byte_thing(1.toByte())
                .i32_thing(3)
                .i64_thing(5L)
                .string_thing("testing")
                .build()
        Xtruct.ADAPTER.write(protocol, struct)
        ProtocolUtil.skip(protocol, TType.STRUCT)
        buffer.size shouldBe 0L
    }

    @Test
    fun skipListOfStructs() {
        val structs = listOf(
                Xtruct.Builder()
                        .byte_thing(1.toByte())
                        .i32_thing(1)
                        .i64_thing(1L)
                        .string_thing("one")
                        .build(),
                Xtruct.Builder()
                        .byte_thing(2.toByte())
                        .i32_thing(2)
                        .i64_thing(2L)
                        .string_thing("two")
                        .build(),
                Xtruct.Builder()
                        .byte_thing(3.toByte())
                        .i32_thing(3)
                        .i64_thing(3L)
                        .string_thing("three")
                        .build(),
        )
        protocol.writeListBegin(TType.STRUCT, structs.size)
        for (struct in structs) {
            Xtruct.ADAPTER.write(protocol, struct)
        }
        protocol.writeListEnd()
        ProtocolUtil.skip(protocol, TType.LIST)
        buffer.size shouldBe 0L
    }

    @Test
    fun throwsProtocolExceptionOnUnknownTTypeValue() {
        protocol.writeStructBegin("Test")
        protocol.writeFieldBegin("num", 1, TType.I32)
        protocol.writeI32(2)
        protocol.writeFieldEnd()
        protocol.writeFieldBegin("invalid_ttype", 2, 84.toByte())
        protocol.writeString("shouldn't get here")
        protocol.writeFieldEnd()
        protocol.writeFieldStop()
        protocol.writeStructEnd()
        try {
            ProtocolUtil.skip(protocol, TType.STRUCT)
            fail("Expected a ProtocolException but nothing was thrown")
        } catch (ignored: ProtocolException) {
            ignored shouldHaveMessage "Unrecognized TType value: 84"
        }
    }

    @Test
    fun skipsBools() {
        protocol.writeBool(true)
        ProtocolUtil.skip(protocol, TType.BOOL)
        buffer.size shouldBe 0L
    }

    @Test
    fun skipsBytes() {
        protocol.writeByte(32)
        ProtocolUtil.skip(protocol, TType.BYTE)
        buffer.size shouldBe 0L
    }

    @Test
    fun skipsShorts() {
        protocol.writeI16(16)
        buffer.size shouldNotBe 0
        ProtocolUtil.skip(protocol, TType.I16)
        buffer.size shouldBe 0
    }

    @Test
    fun skipsInts() {
        protocol.writeI32(32)
        buffer.size shouldNotBe 0
        ProtocolUtil.skip(protocol, TType.I32)
        buffer.size shouldBe 0
    }

    @Test
    fun skipsLongs() {
        protocol.writeI64(64)
        buffer.size shouldNotBe 0
        ProtocolUtil.skip(protocol, TType.I64)
        buffer.size shouldBe 0
    }

    @Test
    fun skipsDoubles() {
        protocol.writeDouble(2.5)
        buffer.size shouldNotBe 0
        ProtocolUtil.skip(protocol, TType.DOUBLE)
        buffer.size shouldBe 0
    }

    @Test
    fun skipsStrings() {
        protocol.writeString("skip me")
        buffer.size shouldNotBe 0
        ProtocolUtil.skip(protocol, TType.STRING)
        buffer.size shouldBe 0
    }
}
