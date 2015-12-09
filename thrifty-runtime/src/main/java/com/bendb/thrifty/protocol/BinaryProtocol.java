/*
 * Copyright 2015 Benjamin Bader
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bendb.thrifty.protocol;

import com.bendb.thrifty.TType;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;

import java.io.IOException;
import java.net.ProtocolException;
import java.nio.ByteBuffer;

/**
 * An implementation of the simple Thrift binary protocol.
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

    private boolean strictRead;
    private boolean strictWrite;

    public BinaryProtocol(BufferedSource source, BufferedSink sink) {
        this(source, sink, -1);
    }

    public BinaryProtocol(BufferedSource source, BufferedSink sink, int stringLengthLimit) {
        this(source, sink, stringLengthLimit, -1);
    }

    public BinaryProtocol(BufferedSource source, BufferedSink sink, int stringLengthLimit, int containerLengthLimit) {
        super(source, sink);
        this.stringLengthLimit = stringLengthLimit;
        this.containerLengthLimit = containerLengthLimit;
    }

    @Override
    public void writeMessageBegin(String name, byte typeId, int seqId) throws IOException {
        if (strictWrite) {
            int version = VERSION_1 | typeId;
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
        sink.writeByte(b);
    }

    @Override
    public void writeI16(short i16) throws IOException {
        sink.writeShort(i16);
    }

    @Override
    public void writeI32(int i32) throws IOException {
        sink.writeInt(i32);
    }

    @Override
    public void writeI64(long i64) throws IOException {
        sink.writeLong(i64);
    }

    @Override
    public void writeDouble(double dub) throws IOException {
        writeI64(Double.doubleToLongBits(dub));
    }

    @Override
    public void writeString(String str) throws IOException {
        Buffer tmp = new Buffer();
        tmp.writeUtf8(str);
        writeI32((int) tmp.size());
        sink.write(tmp, tmp.size());
    }

    @Override
    public void writeBinary(ByteString buf) throws IOException {
        writeI32(buf.size());
        sink.write(buf);
    }

    @Override
    public void writeByteBuffer(ByteBuffer buf) throws IOException {
        int remaining = buf.hasRemaining() ? buf.remaining() : 0;
        writeI32(remaining);

        if (remaining == 0) return;

        byte[] tmp = new byte[Math.min(remaining, 4096)];
        while (remaining > 0) {
            buf.get(tmp);
            sink.write(tmp, 0, remaining);
            remaining = buf.remaining();
        }
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
        if (size > containerLengthLimit) throw new ProtocolException("Container size limit exceeded");
        return new MapMetadata(keyTypeId, valueTypeId, size);
    }

    @Override
    public void readMapEnd() throws IOException {
    }

    @Override
    public ListMetadata readListBegin() throws IOException {
        byte elementTypeId = readByte();
        int size = readI32();
        if (size > containerLengthLimit) throw new ProtocolException("Container size limit exceeded");
        return new ListMetadata(elementTypeId, size);
    }

    @Override
    public void readListEnd() throws IOException {
    }

    @Override
    public SetMetadata readSetBegin() throws IOException {
        byte elementTypeId = readByte();
        int size = readI32();
        if (size > containerLengthLimit) throw new ProtocolException("Container size limit exceeded");
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
        return source.readByte();
    }

    @Override
    public short readI16() throws IOException {
        return source.readShort();
    }

    @Override
    public int readI32() throws IOException {
        return source.readInt();
    }

    @Override
    public long readI64() throws IOException {
        return source.readLong();
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readI64());
    }

    @Override
    public String readString() throws IOException {
        int sizeInBytes = readI32();
        if (sizeInBytes > stringLengthLimit) throw new ProtocolException("String size limit exceeded");
        return source.readUtf8(sizeInBytes);
    }

    @Override
    public ByteString readBinary() throws IOException {
        int sizeInBytes = readI32();
        if (sizeInBytes > stringLengthLimit) throw new ProtocolException("Binary size limit exceeded");
        return source.readByteString();
    }

    @Override
    public ByteBuffer readByteBuffer() throws IOException {
        int sizeInBytes = readI32();
        if (sizeInBytes > stringLengthLimit) throw new ProtocolException("Binary size limit exceeded");
        byte[] byteArray = source.readByteArray(sizeInBytes);
        return ByteBuffer.wrap(byteArray);
    }

    private String readStringWithSize(int size) throws IOException {
        ByteString bytes = source.readByteString(size);
        return bytes.utf8();
    }
}
