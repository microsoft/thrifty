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

import com.microsoft.thrifty.transport.BufferTransport;
import okio.Buffer;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class CompactProtocolTest {
    @Test
    public void varint32() throws IOException {
        Buffer buffer = new Buffer();
        BufferTransport transport = new BufferTransport(buffer);
        CompactProtocol protocol = new CompactProtocol(transport);

        protocol.writeI32(0);
        assertThat(buffer.readByteArray(), equalTo(new byte[] { 0 }));

        protocol.writeI32(1);
        assertThat(buffer.readByteArray(), equalTo(new byte[] { 2 }));

        protocol.writeI32(7);
        assertThat(buffer.readByteArray(), equalTo(new byte[] { 14 }));

        protocol.writeI32(150);
        assertThat(buffer.readByteArray(), equalTo(new byte[] { (byte) 172, 2 }));

        protocol.writeI32(15000);
        assertThat(buffer.readByteArray(), equalTo(new byte[] { (byte) 176, (byte) 234, 1 }));

        protocol.writeI32(0xFFFF);
        assertThat(buffer.readByteArray(), equalTo(new byte[] { (byte) 254, (byte) 255, 7 }));

        protocol.writeI32(0xFFFFFF);
        assertThat(buffer.readByteArray(), equalTo(new byte[] { (byte) 254, (byte) 255, (byte) 255, 15 }));

        protocol.writeI32(-1);
        assertThat(buffer.readByteArray(), equalTo(new byte[] { 1 }));

        protocol.writeI32(-7);
        assertThat(buffer.readByteArray(), equalTo(new byte[] { 13 }));

        protocol.writeI32(-150);
        assertThat(buffer.readByteArray(), equalTo(new byte[] { (byte) 171, 2 }));

        protocol.writeI32(-15000);
        assertThat(buffer.readByteArray(), equalTo(new byte[] { (byte) 175, (byte) 234, 1 }));

        protocol.writeI32(-0xFFFF);
        assertThat(buffer.readByteArray(), equalTo(new byte[] { (byte) 253, (byte) 255, 7 }));

        protocol.writeI32(-0xFFFFFF);
        assertThat(buffer.readByteArray(), equalTo(new byte[] { (byte) 253, (byte) 255, (byte) 255, 15 }));
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
        CompactProtocol proto = new CompactProtocol(transport);

        Xtruct.ADAPTER.write(proto, xtruct);

        Xtruct read = Xtruct.ADAPTER.read(new CompactProtocol(transport));

        assertThat(read, equalTo(xtruct));
    }
}