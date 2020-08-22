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
package com.microsoft.thrifty.protocol;

import com.microsoft.thrifty.TType;
import com.microsoft.thrifty.transport.BufferTransport;
import okio.Buffer;
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JsonProtocolTest {
    private Buffer buffer;
    private JsonProtocol protocol;

    @Before
    public void setup() {
        buffer = new Buffer();
        BufferTransport transport = new BufferTransport(buffer);
        protocol = new JsonProtocol(transport);
    }

    @Test
    public void emptyJsonString() throws Exception {
        protocol.writeString("");
        assertThat(buffer.readUtf8(), is("\"\""));
    }

    @Test
    public void escapesNamedControlChars() throws Exception {
        protocol.writeString("\b\f\r\n\t");
        assertThat(buffer.readUtf8(), is("\"\\b\\f\\r\\n\\t\""));
    }

    @Test
    public void escapesQuotes() throws Exception {
        protocol.writeString("\"");
        assertThat(buffer.readUtf8(), is("\"\\\"\"")); // or, in other words, "\""
    }

    @Test
    public void normalStringIsQuoted() throws Exception {
        protocol.writeString("y u no quote me?");
        assertThat(buffer.readUtf8(), is("\"y u no quote me?\""));
    }

    @Test
    public void emptyList() throws Exception {
        protocol.writeListBegin(TType.STRING, 0);
        protocol.writeListEnd();

        assertThat(buffer.readUtf8(), is("[\"str\",0]"));
    }

    @Test
    public void listWithOneElement() throws Exception {
        protocol.writeListBegin(TType.STRING, 1);
        protocol.writeString("foo");
        protocol.writeListEnd();

        assertThat(buffer.readUtf8(), is("[\"str\",1,\"foo\"]"));
    }

    @Test
    public void listWithTwoElements() throws Exception {
        protocol.writeListBegin(TType.STRING, 2);
        protocol.writeString("foo");
        protocol.writeString("bar");
        protocol.writeListEnd();

        assertThat(buffer.readUtf8(), is("[\"str\",2,\"foo\",\"bar\"]"));
    }

    @Test
    public void emptyMap() throws Exception {
        protocol.writeMapBegin(TType.STRING, TType.I32, 0);
        protocol.writeMapEnd();

        assertThat(buffer.readUtf8(), is("[\"str\",\"i32\",0,{}]"));
    }

    @Test
    public void mapWithSingleElement() throws Exception {
        protocol.writeMapBegin(TType.STRING, TType.I32, 1);
        protocol.writeString("key1");
        protocol.writeI32(1);
        protocol.writeMapEnd();

        assertThat(buffer.readUtf8(), is("[\"str\",\"i32\",1,{\"key1\":1}]"));
    }

    @Test
    public void mapWithTwoElements() throws Exception {
        protocol.writeMapBegin(TType.STRING, TType.I32, 2);
        protocol.writeString("key1");
        protocol.writeI32(1);
        protocol.writeString("key2");
        protocol.writeI32(2);
        protocol.writeMapEnd();

        assertThat(buffer.readUtf8(), is("[\"str\",\"i32\",2,{\"key1\":1,\"key2\":2}]"));
    }

    @Test
    public void listOfMaps() throws Exception {
        protocol.writeListBegin(TType.MAP, 2);

        protocol.writeMapBegin(TType.STRING, TType.I32, 1);
        protocol.writeString("1");
        protocol.writeI32(10);
        protocol.writeMapEnd();

        protocol.writeMapBegin(TType.STRING, TType.I32, 1);
        protocol.writeString("2");
        protocol.writeI32(20);
        protocol.writeMapEnd();

        protocol.writeListEnd();

        assertThat(buffer.readUtf8(), is("[\"map\",2,[\"str\",\"i32\",1,{\"1\":10}],[\"str\",\"i32\",1,{\"2\":20}]]"));
    }

    @Test
    public void structs() throws Exception {
        Xtruct xtruct = new Xtruct.Builder()
                .byte_thing((byte) 1)
                .double_thing(2.0)
                .i32_thing(3)
                .i64_thing(4L)
                .string_thing("five")
                .build();

        Xtruct.ADAPTER.write(protocol, xtruct);

        assertThat(buffer.readUtf8(), is("" +
                "{" +
                "\"1\":{\"str\":\"five\"}," +
                "\"4\":{\"i8\":1}," +
                "\"9\":{\"i32\":3}," +
                "\"11\":{\"i64\":4}," +
                "\"13\":{\"dbl\":2.0}" +
                "}"));
    }

    @Test
    public void binary() throws Exception {
        protocol.writeBinary(ByteString.encodeUtf8("foobar"));

        assertThat(buffer.readUtf8(), is("\"Zm9vYmFy\""));
    }

    @Test
    public void roundtrip() throws Exception {
        Xtruct xtruct = new Xtruct.Builder()
                .byte_thing((byte) 254)
                .i32_thing(0xFFFF)
                .i64_thing(0xFFFFFFFFL)
                .string_thing("foo")
                .double_thing(Math.PI)
                .bool_thing(true)
                .build();

        Buffer buffer = new Buffer();
        BufferTransport transport = new BufferTransport(buffer);
        JsonProtocol proto = new JsonProtocol(transport);

        Xtruct.ADAPTER.write(proto, xtruct);

        Xtruct read = Xtruct.ADAPTER.read(new JsonProtocol(transport));

        assertThat(read, is(xtruct));
    }
}