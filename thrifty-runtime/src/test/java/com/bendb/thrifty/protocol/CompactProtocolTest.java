/*
 * Copyright (C) 2015-2016 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.thrifty.protocol;

import okio.Buffer;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class CompactProtocolTest {
    @Test
    public void varint32() throws IOException {
        Buffer buffer = new Buffer();
        CompactProtocol protocol = new CompactProtocol(buffer, buffer);

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
                .build();

        Buffer buffer = new Buffer();
        CompactProtocol proto = new CompactProtocol(buffer, buffer);

        Xtruct.ADAPTER.write(proto, xtruct);

        Xtruct read = Xtruct.ADAPTER.read(new CompactProtocol(buffer, buffer));

        assertThat(read, equalTo(xtruct));
    }
}