package com.bendb.thrifty.protocol;

import okio.Buffer;
import okio.ByteString;
import org.junit.Test;

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

        BinaryProtocol proto = new BinaryProtocol(buffer, buffer);
        assertThat(proto.readString(), is("foo"));
    }

    @Test
    public void readStringGreaterThanLimit() throws Exception {
        Buffer buffer = new Buffer();
        buffer.writeInt(13);
        buffer.writeUtf8("foobarbazquux");

        BinaryProtocol proto = new BinaryProtocol(buffer, buffer, 12);

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

        BinaryProtocol proto = new BinaryProtocol(buffer, buffer);
        assertThat(proto.readBinary(), equalTo(ByteString.encodeUtf8("abcd")));
    }

    @Test
    public void readBinaryGreaterThanLimit() throws Exception {
        Buffer buffer = new Buffer();
        buffer.writeInt(6);
        buffer.writeUtf8("kaboom");

        BinaryProtocol proto = new BinaryProtocol(buffer, buffer, 4);
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
        BinaryProtocol proto = new BinaryProtocol(buffer, buffer);

        proto.writeByte((byte) 127);
        assertThat(buffer.readByte(), equalTo((byte) 127));
    }

    @Test
    public void writeI16() throws Exception {
        Buffer buffer = new Buffer();
        BinaryProtocol proto = new BinaryProtocol(buffer, buffer);

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
        BinaryProtocol proto = new BinaryProtocol(buffer, buffer);

        proto.writeI32(0xFF0F00FF);
        assertThat(buffer.readInt(), equalTo(0xFF0F00FF));
    }

    @Test
    public void writeI64() throws Exception {
        Buffer buffer = new Buffer();
        BinaryProtocol proto = new BinaryProtocol(buffer, buffer);

        proto.writeI64(0x12345678);
        assertThat(buffer.readLong(), equalTo(0x12345678L));
    }

    @Test
    public void writeDouble() throws Exception {
        Buffer buffer = new Buffer();
        BinaryProtocol proto = new BinaryProtocol(buffer, buffer);

        // Doubles go on the wire as the 8-byte blobs from
        // Double#doubleToLongBits().
        proto.writeDouble(Math.PI);
        assertThat(buffer.readLong(), equalTo(Double.doubleToLongBits(Math.PI)));
    }

    @Test
    public void writeString() throws Exception {
        Buffer buffer = new Buffer();
        BinaryProtocol proto = new BinaryProtocol(buffer, buffer);

        proto.writeString("here is a string");
        assertThat(buffer.readInt(), equalTo(16));
        assertThat(buffer.readUtf8(), equalTo("here is a string"));
    }
}