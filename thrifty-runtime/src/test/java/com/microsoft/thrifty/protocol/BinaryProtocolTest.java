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
import com.microsoft.thrifty.util.ProtocolUtil;
import okio.Buffer;
import okio.ByteString;
import org.junit.Test;

import java.io.IOException;
import java.net.ProtocolException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class BinaryProtocolTest {
    @Test
    public void readString() throws Exception {
        Buffer buffer = new Buffer();
        buffer.writeInt(3);
        buffer.writeUtf8("foo");

        BinaryProtocol proto = new BinaryProtocol.Builder(new BufferTransport(buffer)).build();
        assertThat(proto.readString(), is("foo"));
    }

    @Test
    public void readStringGreaterThanLimit() throws Exception {
        Buffer buffer = new Buffer();
        buffer.writeInt(13);
        buffer.writeUtf8("foobarbazquux");

        BinaryProtocol proto = new BinaryProtocol.Builder(new BufferTransport(buffer)).setStringLengthLimit(12).build();

        try {
            proto.readString();
            fail();
        } catch (ProtocolException e) {
            assertThat(e.getMessage(), containsString("String size limit exceeded"));
        }
    }

    @Test
    public void readBinary() throws Exception {
        Buffer buffer = new Buffer();
        buffer.writeInt(4);
        buffer.writeUtf8("abcd");

        BinaryProtocol proto = new BinaryProtocol.Builder(new BufferTransport(buffer)).build();
        assertThat(proto.readBinary(), equalTo(ByteString.encodeUtf8("abcd")));
    }

    @Test
    public void readBinaryGreaterThanLimit() throws Exception {
        Buffer buffer = new Buffer();
        buffer.writeInt(6);
        buffer.writeUtf8("kaboom");

        BinaryProtocol proto = new BinaryProtocol.Builder(new BufferTransport(buffer)).setStringLengthLimit(4).build();
        try {
            proto.readBinary();
            fail();
        } catch (ProtocolException e) {
            assertThat(e.getMessage(), containsString("Binary size limit exceeded"));
        }
    }

    @Test
    public void writeByte() throws Exception {
        Buffer buffer = new Buffer();
        BinaryProtocol proto = new BinaryProtocol.Builder(new BufferTransport(buffer)).build();

        proto.writeByte((byte) 127);
        assertThat(buffer.readByte(), equalTo((byte) 127));
    }

    @Test
    public void writeI16() throws Exception {
        Buffer buffer = new Buffer();
        BinaryProtocol proto = new BinaryProtocol.Builder(new BufferTransport(buffer)).build();

        proto.writeI16(Short.MAX_VALUE);
        assertThat(buffer.readShort(), equalTo(Short.MAX_VALUE));

        // Make sure it's written big-endian
        buffer.clear();
        proto.writeI16((short) 0xFF00);
        assertThat(buffer.readShort(), equalTo((short) 0xFF00));
    }

    @Test
    public void writeI32() throws Exception {
        Buffer buffer = new Buffer();
        BinaryProtocol proto = new BinaryProtocol.Builder(new BufferTransport(buffer)).build();

        proto.writeI32(0xFF0F00FF);
        assertThat(buffer.readInt(), equalTo(0xFF0F00FF));
    }

    @Test
    public void writeI64() throws Exception {
        Buffer buffer = new Buffer();
        BinaryProtocol proto = new BinaryProtocol.Builder(new BufferTransport(buffer)).build();

        proto.writeI64(0x12345678);
        assertThat(buffer.readLong(), equalTo(0x12345678L));
    }

    @Test
    public void writeDouble() throws Exception {
        Buffer buffer = new Buffer();
        BinaryProtocol proto = new BinaryProtocol.Builder(new BufferTransport(buffer)).build();

        // Doubles go on the wire as the 8-byte blobs from
        // Double#doubleToLongBits().
        proto.writeDouble(Math.PI);
        assertThat(buffer.readLong(), equalTo(Double.doubleToLongBits(Math.PI)));
    }

    @Test
    public void writeString() throws Exception {
        Buffer buffer = new Buffer();
        BinaryProtocol proto = new BinaryProtocol.Builder(new BufferTransport(buffer)).build();

        proto.writeString("here is a string");
        assertThat(buffer.readInt(), equalTo(16));
        assertThat(buffer.readUtf8(), equalTo("here is a string"));
    }

    @Test
    public void adapterTest() throws Exception {
        // This test case comes from actual data, and is intended
        // to ensure in particular that readers don't grab more data than
        // they are supposed to.
        String payload =
                "030001000600" +
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
                "62333300000000";

        ByteString binaryData = ByteString.decodeHex(payload);
        Buffer buffer = new Buffer();
        buffer.write(binaryData);
        BinaryProtocol protocol = new BinaryProtocol.Builder(new BufferTransport(buffer)).build();
        read(protocol);
    }

    public void read(Protocol protocol) throws IOException {
        protocol.readStructBegin();
        while (true) {
            FieldMetadata field = protocol.readFieldBegin();
            if (field.typeId == TType.STOP) {
                break;
            }
            switch (field.fieldId) {
                case 1: {
                    if (field.typeId == TType.BYTE) {
                        byte value = protocol.readByte();
                    } else {
                        ProtocolUtil.skip(protocol, field.typeId);
                    }
                }
                break;
                case 2: {
                    if (field.typeId == TType.I16) {
                        short value = protocol.readI16();
                    } else {
                        ProtocolUtil.skip(protocol, field.typeId);
                    }
                }
                break;
                case 3: {
                    if (field.typeId == TType.I16) {
                        short value = protocol.readI16();
                    } else {
                        ProtocolUtil.skip(protocol, field.typeId);
                    }
                }
                break;
                case 4: {
                    if (field.typeId == TType.STRING) {
                        ByteString value = protocol.readBinary();
                    } else {
                        ProtocolUtil.skip(protocol, field.typeId);
                    }
                }
                break;
                default: {
                    ProtocolUtil.skip(protocol, field.typeId);
                }
                break;
            }
            protocol.readFieldEnd();
        }
    }
}