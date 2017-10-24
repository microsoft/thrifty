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
package com.microsoft.thrifty.util;

import com.microsoft.thrifty.TType;
import com.microsoft.thrifty.protocol.BinaryProtocol;
import com.microsoft.thrifty.protocol.Protocol;
import com.microsoft.thrifty.protocol.Xtruct;
import com.microsoft.thrifty.transport.BufferTransport;
import okio.Buffer;
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;

import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ProtocolUtilTest {
    private Buffer buffer;
    private BinaryProtocol protocol;

    private Protocol mockProtocol;

    @Before
    public void setup() {
        buffer = new Buffer();
        protocol = new BinaryProtocol.Builder(new BufferTransport(buffer)).build();
        mockProtocol = mock(Protocol.class);
    }

    @Test
    public void skipConsumesLists() throws Exception {
        List<String> strings = Arrays.asList("foo", "bar", "baz", "quux");

        protocol.writeListBegin(TType.STRING, strings.size());
        for (String string : strings) {
            protocol.writeString(string);
        }
        protocol.writeListEnd();

        ProtocolUtil.skip(protocol, TType.LIST);

        assertThat(buffer.size(), is(0L));
    }

    @Test
    public void skipSets() throws Exception{
        Set<ByteString> set = new LinkedHashSet<>();
        set.add(ByteString.encodeUtf8("hello there"));
        set.add(ByteString.encodeUtf8("here is some more test data"));
        set.add(ByteString.encodeUtf8("take it respectfully!"));

        protocol.writeSetBegin(TType.STRING, set.size());
        for (ByteString bytes : set) {
            protocol.writeBinary(bytes);
        }
        protocol.writeSetEnd();

        ProtocolUtil.skip(protocol, TType.SET);

        assertThat(buffer.size(), is(0L));
    }

    @Test
    public void skipConsumesMap() throws Exception {
        Map<Integer, Long> map = new LinkedHashMap<>();
        map.put(1, 10L);
        map.put(2, 20L);
        map.put(4, 30L);
        map.put(8, 40L);

        protocol.writeMapBegin(TType.I32, TType.I64, map.size());
        for (Map.Entry<Integer, Long> entry : map.entrySet()) {
            protocol.writeI32(entry.getKey());
            protocol.writeI64(entry.getValue());
        }
        protocol.writeMapEnd();

        ProtocolUtil.skip(protocol, TType.MAP);

        assertThat(buffer.size(), is(0L));
    }

    @Test
    public void skipConsumesStructs() throws Exception {
        Xtruct struct = new Xtruct.Builder()
                .byte_thing((byte) 1)
                .i32_thing(3)
                .i64_thing(5L)
                .string_thing("testing")
                .build();

        Xtruct.ADAPTER.write(protocol, struct);

        ProtocolUtil.skip(protocol, TType.STRUCT);

        assertThat(buffer.size(), is(0L));
    }

    @Test
    public void skipListOfStructs() throws Exception {
        List<Xtruct> structs = new ArrayList<>();

        structs.add(new Xtruct.Builder()
                .byte_thing((byte) 1)
                .i32_thing(1)
                .i64_thing(1L)
                .string_thing("one")
                .build());

        structs.add(new Xtruct.Builder()
                .byte_thing((byte) 2)
                .i32_thing(2)
                .i64_thing(2L)
                .string_thing("two")
                .build());

        structs.add(new Xtruct.Builder()
                .byte_thing((byte) 3)
                .i32_thing(3)
                .i64_thing(3L)
                .string_thing("three")
                .build());

        protocol.writeListBegin(TType.STRUCT, structs.size());
        for (Xtruct struct : structs) {
            Xtruct.ADAPTER.write(protocol, struct);
        }
        protocol.writeListEnd();

        ProtocolUtil.skip(protocol, TType.LIST);

        assertThat(buffer.size(), is(0L));
    }

    @Test
    public void throwsProtocolExceptionOnUnknownTTypeValue() throws Exception {
        Buffer buffer = new Buffer();
        BinaryProtocol protocol = new BinaryProtocol.Builder(new BufferTransport(buffer)).build();
        protocol.writeStructBegin("Test");
        protocol.writeFieldBegin("num", 1, TType.I32);
        protocol.writeI32(2);
        protocol.writeFieldEnd();
        protocol.writeFieldBegin("invalid_ttype", 2, (byte) 84);
        protocol.writeString("shouldn't get here");
        protocol.writeFieldEnd();
        protocol.writeFieldStop();
        protocol.writeStructEnd();

        try {
            ProtocolUtil.skip(protocol, TType.STRUCT);
            fail();
        } catch (ProtocolException ignored) {
            assertThat(ignored.getMessage(), equalTo("Unrecognized TType value: 84"));
        }
    }

    @Test
    public void skipsBools() throws Exception {
        ProtocolUtil.skip(mockProtocol, TType.BOOL);
        verify(mockProtocol).readBool();
        verifyNoMoreInteractions(mockProtocol);
    }

    @Test
    public void skipsBytes() throws Exception {
        ProtocolUtil.skip(mockProtocol, TType.BYTE);
        verify(mockProtocol).readByte();
        verifyNoMoreInteractions(mockProtocol);
    }

    @Test
    public void skipsShorts() throws Exception {
        ProtocolUtil.skip(mockProtocol, TType.I16);
        verify(mockProtocol).readI16();
        verifyNoMoreInteractions(mockProtocol);
    }

    @Test
    public void skipsInts() throws Exception {
        ProtocolUtil.skip(mockProtocol, TType.I32);
        verify(mockProtocol).readI32();
        verifyNoMoreInteractions(mockProtocol);
    }

    @Test
    public void skipsLongs() throws Exception {
        ProtocolUtil.skip(mockProtocol, TType.I64);
        verify(mockProtocol).readI64();
        verifyNoMoreInteractions(mockProtocol);
    }

    @Test
    public void skipsDoubles() throws Exception {
        ProtocolUtil.skip(mockProtocol, TType.DOUBLE);
        verify(mockProtocol).readDouble();
        verifyNoMoreInteractions(mockProtocol);
    }

    @Test
    public void skipsStrings() throws Exception {
        ProtocolUtil.skip(mockProtocol, TType.STRING);
        verify(mockProtocol).readString();
        verifyNoMoreInteractions(mockProtocol);
    }
}