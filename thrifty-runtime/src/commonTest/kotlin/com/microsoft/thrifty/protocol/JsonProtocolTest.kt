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
import io.kotest.matchers.shouldBe
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import kotlin.math.PI
import kotlin.test.Test

class JsonProtocolTest {
    private val buffer = Buffer()
    private var protocol = JsonProtocol(BufferTransport(buffer))

    @Test
    fun emptyJsonString() {
        protocol.writeString("")
        buffer.readUtf8() shouldBe "\"\""
    }

    @Test
    fun escapesNamedControlChars() {
        protocol.writeString("\b\u000C\r\n\t")
        buffer.readUtf8() shouldBe "\"\\b\\f\\r\\n\\t\""
    }

    @Test
    fun escapesQuotes() {
        protocol.writeString("\"")
        buffer.readUtf8() shouldBe "\"\\\"\"" // or, in other words, "\""
    }

    @Test
    fun normalStringIsQuoted() {
        protocol.writeString("y u no quote me?")
        buffer.readUtf8() shouldBe "\"y u no quote me?\""
    }

    @Test
    fun emptyList() {
        protocol.writeListBegin(TType.STRING, 0)
        protocol.writeListEnd()
        buffer.readUtf8() shouldBe "[\"str\",0]"
    }

    @Test
    fun listWithOneElement() {
        protocol.writeListBegin(TType.STRING, 1)
        protocol.writeString("foo")
        protocol.writeListEnd()
        buffer.readUtf8() shouldBe "[\"str\",1,\"foo\"]"
    }

    @Test
    fun listWithTwoElements() {
        protocol.writeListBegin(TType.STRING, 2)
        protocol.writeString("foo")
        protocol.writeString("bar")
        protocol.writeListEnd()
        buffer.readUtf8() shouldBe "[\"str\",2,\"foo\",\"bar\"]"
    }

    @Test
    fun emptyMap() {
        protocol.writeMapBegin(TType.STRING, TType.I32, 0)
        protocol.writeMapEnd()
        buffer.readUtf8() shouldBe "[\"str\",\"i32\",0,{}]"
    }

    @Test
    fun mapWithSingleElement() {
        protocol.writeMapBegin(TType.STRING, TType.I32, 1)
        protocol.writeString("key1")
        protocol.writeI32(1)
        protocol.writeMapEnd()
        buffer.readUtf8() shouldBe "[\"str\",\"i32\",1,{\"key1\":1}]"
    }

    @Test
    fun mapWithTwoElements() {
        protocol.writeMapBegin(TType.STRING, TType.I32, 2)
        protocol.writeString("key1")
        protocol.writeI32(1)
        protocol.writeString("key2")
        protocol.writeI32(2)
        protocol.writeMapEnd()
        buffer.readUtf8() shouldBe "[\"str\",\"i32\",2,{\"key1\":1,\"key2\":2}]"
    }

    @Test
    fun listOfMaps() {
        protocol.writeListBegin(TType.MAP, 2)
        protocol.writeMapBegin(TType.STRING, TType.I32, 1)
        protocol.writeString("1")
        protocol.writeI32(10)
        protocol.writeMapEnd()
        protocol.writeMapBegin(TType.STRING, TType.I32, 1)
        protocol.writeString("2")
        protocol.writeI32(20)
        protocol.writeMapEnd()
        protocol.writeListEnd()
        buffer.readUtf8() shouldBe "[\"map\",2,[\"str\",\"i32\",1,{\"1\":10}],[\"str\",\"i32\",1,{\"2\":20}]]"
    }

    @Test
    fun structs() {
        val xtruct = Xtruct.Builder()
                .byte_thing(1.toByte())
                .double_thing(2.0)
                .i32_thing(3)
                .i64_thing(4L)
                .string_thing("five")
                .build()
        Xtruct.ADAPTER.write(protocol, xtruct)
        buffer.readUtf8() shouldBe "" +
                "{" +
                "\"1\":{\"str\":\"five\"}," +
                "\"4\":{\"i8\":1}," +
                "\"9\":{\"i32\":3}," +
                "\"11\":{\"i64\":4}," +
                "\"13\":{\"dbl\":2.0}" +
                "}"
    }

    @Test
    fun binary() {
        protocol.writeBinary("foobar".encodeUtf8())
        buffer.readUtf8() shouldBe "\"Zm9vYmFy\""
    }

    @Test
    fun roundtrip() {
        val xtruct = Xtruct.Builder()
                .byte_thing(254.toByte())
                .i32_thing(0xFFFF)
                .i64_thing(0xFFFFFFFFL)
                .string_thing("foo")
                .double_thing(PI)
                .bool_thing(true)
                .build()
        val buffer = Buffer()
        val transport = BufferTransport(buffer)
        val proto = JsonProtocol(transport)
        Xtruct.ADAPTER.write(proto, xtruct)
        Xtruct.ADAPTER.read(JsonProtocol(transport)) shouldBe xtruct
    }
}
