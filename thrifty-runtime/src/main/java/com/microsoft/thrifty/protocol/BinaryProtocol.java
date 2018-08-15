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
import com.microsoft.thrifty.transport.Transport;
import okio.ByteString;

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;

/**
 * An implementation of the simple Thrift binary protocol.
 * <p>
 * <p>Instances of this class are <em>not</em> threadsafe.
 */
public class BinaryProtocol extends Protocol {
    private static final int VERSION_MASK = 0xffff0000;
    private static final int VERSION_1 = 0x80010000;

    private static final StructMetadata NO_STRUCT = new StructMetadata("");

    /**
     * The maximum number of bytes to read from the transport for variable-length
     * fields (strings or binary), or -1 for unlimited.
     */
    private final long stringLengthLimit;

    /**
     * The maximum number of elements to read from the network for containers
     * (maps, lists, sets).
     */
    private final long containerLengthLimit;

    /**
     * A shared buffer for writing.
     */
    private final byte[] buffer = new byte[8];

    private boolean strictRead;
    private boolean strictWrite;

    public static class Builder {

        private Transport transport;
        private int stringLengthLimit = -1;
        private int containerLengthLimit = -1;
        private boolean strictRead;
        private boolean strictWrite;

        public Builder(Transport transport) {
            this.transport = transport;
        }

        public Builder setStringLengthLimit(int stringLengthLimit) {
            this.stringLengthLimit = stringLengthLimit;
            return this;
        }

        public Builder setContainerLengthLimit(int containerLengthLimit) {
            this.containerLengthLimit = containerLengthLimit;
            return this;
        }

        public Builder setStrictRead(boolean strictRead) {
            this.strictRead = strictRead;
            return this;
        }

        public Builder setStrictWrite(boolean strictWrite) {
            this.strictWrite = strictWrite;
            return this;
        }

        public BinaryProtocol build() {
            return new BinaryProtocol(this);
        }
    }

    BinaryProtocol(Builder builder) {
        super(builder.transport);
        this.stringLengthLimit = builder.stringLengthLimit;
        this.containerLengthLimit = builder.containerLengthLimit;
        this.strictRead = builder.strictRead;
        this.strictWrite = builder.strictWrite;
    }

    @Override
    public void writeMessageBegin(String name, byte typeId, int seqId) throws IOException {
        if (strictWrite) {
            int version = VERSION_1 | (typeId & 0xFF);
            writeI32(version);
            writeString(name);
            writeI32(seqId);
        } else {
            writeString(name);
            writeByte(typeId);
            writeI32(seqId);
        }
    }

    @Override
    public void writeMessageEnd() throws IOException {
    }

    @Override
    public void writeStructBegin(String structName) throws IOException {
    }

    @Override
    public void writeStructEnd() throws IOException {
    }

    @Override
    public void writeFieldBegin(String fieldName, int fieldId, byte typeId) throws IOException {
        writeByte(typeId);
        writeI16((short) fieldId);
    }

    @Override
    public void writeFieldEnd() throws IOException {
    }

    @Override
    public void writeFieldStop() throws IOException {
        writeByte(TType.STOP);
    }

    @Override
    public void writeMapBegin(byte keyTypeId, byte valueTypeId, int mapSize) throws IOException {
        writeByte(keyTypeId);
        writeByte(valueTypeId);
        writeI32(mapSize);
    }

    @Override
    public void writeMapEnd() throws IOException {
    }

    @Override
    public void writeListBegin(byte elementTypeId, int listSize) throws IOException {
        writeByte(elementTypeId);
        writeI32(listSize);
    }

    @Override
    public void writeListEnd() throws IOException {
    }

    @Override
    public void writeSetBegin(byte elementTypeId, int setSize) throws IOException {
        writeByte(elementTypeId);
        writeI32(setSize);
    }

    @Override
    public void writeSetEnd() throws IOException {
    }

    @Override
    public void writeBool(boolean b) throws IOException {
        writeByte(b ? (byte) 1 : (byte) 0);
    }

    @Override
    public void writeByte(byte b) throws IOException {
        buffer[0] = b;

        transport.write(buffer, 0, 1);
    }

    @Override
    public void writeI16(short i16) throws IOException {
        buffer[0] = (byte) ((i16 >> 8) & 0xFF);
        buffer[1] = (byte) (i16 & 0xFF);

        transport.write(buffer, 0, 2);
    }

    @Override
    public void writeI32(int i32) throws IOException {
        buffer[0] = (byte) ((i32 >> 24) & 0xFF);
        buffer[1] = (byte) ((i32 >> 16) & 0xFF);
        buffer[2] = (byte) ((i32 >> 8) & 0xFF);
        buffer[3] = (byte) (i32 & 0xFF);

        transport.write(buffer, 0, 4);
    }

    @Override
    public void writeI64(long i64) throws IOException {
        buffer[0] = (byte) ((i64 >> 56) & 0xFF);
        buffer[1] = (byte) ((i64 >> 48) & 0xFF);
        buffer[2] = (byte) ((i64 >> 40) & 0xFF);
        buffer[3] = (byte) ((i64 >> 32) & 0xFF);
        buffer[4] = (byte) ((i64 >> 24) & 0xFF);
        buffer[5] = (byte) ((i64 >> 16) & 0xFF);
        buffer[6] = (byte) ((i64 >> 8) & 0xFF);
        buffer[7] = (byte) (i64 & 0xFF);

        transport.write(buffer, 0, 8);
    }

    @Override
    public void writeDouble(double dub) throws IOException {
        writeI64(Double.doubleToLongBits(dub));
    }

    @Override
    public void writeString(String str) throws IOException {
        try {
            byte[] bs = str.getBytes("UTF-8");
            writeI32(bs.length);
            transport.write(bs);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void writeBinary(ByteString buf) throws IOException {
        writeI32(buf.size());
        transport.write(buf.toByteArray());
    }

    //////////////////////

    @Override
    public MessageMetadata readMessageBegin() throws IOException {
        int size = readI32();
        if (size < 0) {
            int version = size & VERSION_MASK;
            if (version != VERSION_1) {
                throw new ProtocolException("Bad version in readMessageBegin");
            }
            return new MessageMetadata(readString(), (byte) (size & 0xff), readI32());
        } else {
            if (strictRead) {
                throw new ProtocolException("Missing version in readMessageBegin");
            }
            return new MessageMetadata(readStringWithSize(size), readByte(), readI32());
        }
    }

    @Override
    public void readMessageEnd() throws IOException {
    }

    @Override
    public StructMetadata readStructBegin() throws IOException {
        return NO_STRUCT;
    }

    @Override
    public void readStructEnd() throws IOException {

    }

    @Override
    public FieldMetadata readFieldBegin() throws IOException {
        byte typeId = readByte();
        short fieldId = typeId == TType.STOP ? 0 : readI16();
        return new FieldMetadata("", typeId, fieldId);
    }

    @Override
    public void readFieldEnd() throws IOException {
    }

    @Override
    public MapMetadata readMapBegin() throws IOException {
        byte keyTypeId = readByte();
        byte valueTypeId = readByte();
        int size = readI32();
        if (containerLengthLimit != -1 && size > containerLengthLimit) {
            throw new ProtocolException("Container size limit exceeded");
        }
        return new MapMetadata(keyTypeId, valueTypeId, size);
    }

    @Override
    public void readMapEnd() throws IOException {
    }

    @Override
    public ListMetadata readListBegin() throws IOException {
        byte elementTypeId = readByte();
        int size = readI32();
        if (containerLengthLimit != -1 && size > containerLengthLimit) {
            throw new ProtocolException("Container size limit exceeded");
        }
        return new ListMetadata(elementTypeId, size);
    }

    @Override
    public void readListEnd() throws IOException {
    }

    @Override
    public SetMetadata readSetBegin() throws IOException {
        byte elementTypeId = readByte();
        int size = readI32();
        if (containerLengthLimit != -1 && size > containerLengthLimit) {
            throw new ProtocolException("Container size limit exceeded");
        }
        return new SetMetadata(elementTypeId, size);
    }

    @Override
    public void readSetEnd() throws IOException {
    }

    @Override
    public boolean readBool() throws IOException {
        return readByte() == 1;
    }

    @Override
    public byte readByte() throws IOException {
        readFully(buffer, 1);
        return buffer[0];
    }

    @Override
    public short readI16() throws IOException {
        readFully(buffer, 2);
        return (short) (((buffer[0] & 0xFF) << 8)
                | (buffer[1] & 0xFF));
    }

    @Override
    public int readI32() throws IOException {
        readFully(buffer, 4);

        return ((buffer[0] & 0xFF) << 24)
                | ((buffer[1] & 0xFF) << 16)
                | ((buffer[2] & 0xFF) << 8)
                | (buffer[3] & 0xFF);
    }

    @Override
    public long readI64() throws IOException {
        readFully(buffer, 8);

        return ((buffer[0] & 0xFFL) << 56)
                | ((buffer[1] & 0xFFL) << 48)
                | ((buffer[2] & 0xFFL) << 40)
                | ((buffer[3] & 0xFFL) << 32)
                | ((buffer[4] & 0xFFL) << 24)
                | ((buffer[5] & 0xFFL) << 16)
                | ((buffer[6] & 0xFFL) << 8)
                | (buffer[7] & 0xFFL);
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readI64());
    }

    @Override
    public String readString() throws IOException {
        int sizeInBytes = readI32();
        if (stringLengthLimit != -1 && sizeInBytes > stringLengthLimit) {
            throw new ProtocolException("String size limit exceeded");
        }

        return readStringWithSize(sizeInBytes);
    }

    @Override
    public ByteString readBinary() throws IOException {
        int sizeInBytes = readI32();
        if (stringLengthLimit != -1 && sizeInBytes > stringLengthLimit) {
            throw new ProtocolException("Binary size limit exceeded");
        }
        byte[] data = new byte[sizeInBytes];
        readFully(data, data.length);
        return ByteString.of(data);
    }

    private String readStringWithSize(int size) throws IOException {
        byte[] encoded = new byte[size];
        readFully(encoded, size);
        return new String(encoded, "UTF-8");
    }

    private void readFully(byte[] buffer, int count) throws IOException {
        int toRead = count;
        int offset = 0;
        while (toRead > 0) {
            int read = transport.read(buffer, offset, toRead);
            if (read == -1) {
                throw new EOFException("Expected " + count + " bytes; got " + offset);
            }
            toRead -= read;
            offset += read;
        }
    }
}
