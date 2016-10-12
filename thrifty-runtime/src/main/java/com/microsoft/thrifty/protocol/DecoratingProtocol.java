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

import okio.ByteString;
import java.io.IOException;

/**
 * A protocol that decorates another protocol.
 */
public abstract class DecoratingProtocol extends Protocol {

    private final Protocol concreteProtocol;

    public DecoratingProtocol(Protocol protocol) {
        super(protocol.transport);
        concreteProtocol = protocol;
    }

    @Override
    public void writeMessageBegin(String name, byte typeId, int seqId) throws IOException {
        concreteProtocol.writeMessageBegin(name, typeId, seqId);
    }

    @Override
    public void writeMessageEnd() throws IOException {
        concreteProtocol.writeMessageEnd();
    }

    @Override
    public void writeStructBegin(String structName) throws IOException {
        concreteProtocol.writeStructBegin(structName);
    }

    @Override
    public void writeStructEnd() throws IOException {
        concreteProtocol.writeStructEnd();
    }

    @Override
    public void writeFieldBegin(String fieldName, int fieldId, byte typeId) throws IOException {
        concreteProtocol.writeFieldBegin(fieldName, fieldId, typeId);
    }

    @Override
    public void writeFieldEnd() throws IOException {
        concreteProtocol.writeFieldEnd();
    }

    @Override
    public void writeFieldStop() throws IOException {
        concreteProtocol.writeFieldStop();
    }

    @Override
    public void writeMapBegin(byte keyTypeId, byte valueTypeId, int mapSize) throws IOException {
        concreteProtocol.writeMapBegin(keyTypeId, valueTypeId, mapSize);
    }

    @Override
    public void writeMapEnd() throws IOException {
        concreteProtocol.writeMapEnd();
    }

    @Override
    public void writeListBegin(byte elementTypeId, int listSize) throws IOException {
        concreteProtocol.writeListBegin(elementTypeId, listSize);
    }

    @Override
    public void writeListEnd() throws IOException {
        concreteProtocol.writeListEnd();
    }

    @Override
    public void writeSetBegin(byte elementTypeId, int setSize) throws IOException {
        concreteProtocol.writeSetBegin(elementTypeId, setSize);
    }

    @Override
    public void writeSetEnd() throws IOException {
        concreteProtocol.writeSetEnd();
    }

    @Override
    public void writeBool(boolean b) throws IOException {
        concreteProtocol.writeBool(b);
    }

    @Override
    public void writeByte(byte b) throws IOException {
        concreteProtocol.writeByte(b);
    }

    @Override
    public void writeI16(short i16) throws IOException {
        concreteProtocol.writeI16(i16);
    }

    @Override
    public void writeI32(int i32) throws IOException {
        concreteProtocol.writeI32(i32);
    }

    @Override
    public void writeI64(long i64) throws IOException {
        concreteProtocol.writeI64(i64);
    }

    @Override
    public void writeDouble(double dub) throws IOException {
        concreteProtocol.writeDouble(dub);
    }

    @Override
    public void writeString(String str) throws IOException {
        concreteProtocol.writeString(str);
    }

    @Override
    public void writeBinary(ByteString buf) throws IOException {
        concreteProtocol.writeBinary(buf);
    }

    @Override
    public MessageMetadata readMessageBegin() throws IOException {
        return concreteProtocol.readMessageBegin();
    }

    @Override
    public void readMessageEnd() throws IOException {
        concreteProtocol.readMessageEnd();
    }

    @Override
    public StructMetadata readStructBegin() throws IOException {
        return concreteProtocol.readStructBegin();
    }

    @Override
    public void readStructEnd() throws IOException {
        concreteProtocol.readStructEnd();
    }

    @Override
    public FieldMetadata readFieldBegin() throws IOException {
        return concreteProtocol.readFieldBegin();
    }

    @Override
    public void readFieldEnd() throws IOException {
        concreteProtocol.readFieldEnd();
    }

    @Override
    public MapMetadata readMapBegin() throws IOException {
        return concreteProtocol.readMapBegin();
    }

    @Override
    public void readMapEnd() throws IOException {
        concreteProtocol.readMapEnd();
    }

    @Override
    public ListMetadata readListBegin() throws IOException {
        return concreteProtocol.readListBegin();
    }

    @Override
    public void readListEnd() throws IOException {
        concreteProtocol.readListEnd();
    }

    @Override
    public SetMetadata readSetBegin() throws IOException {
        return concreteProtocol.readSetBegin();
    }

    @Override
    public void readSetEnd() throws IOException {
        concreteProtocol.readSetEnd();
    }

    @Override
    public boolean readBool() throws IOException {
        return concreteProtocol.readBool();
    }

    @Override
    public byte readByte() throws IOException {
        return concreteProtocol.readByte();
    }

    @Override
    public short readI16() throws IOException {
        return concreteProtocol.readI16();
    }

    @Override
    public int readI32() throws IOException {
        return concreteProtocol.readI32();
    }

    @Override
    public long readI64() throws IOException {
        return concreteProtocol.readI64();
    }

    @Override
    public double readDouble() throws IOException {
        return concreteProtocol.readDouble();
    }

    @Override
    public String readString() throws IOException {
        return concreteProtocol.readString();
    }

    @Override
    public ByteString readBinary() throws IOException {
        return concreteProtocol.readBinary();
    }

    @Override
    public void flush() throws IOException {
        concreteProtocol.flush();
    }

    @Override
    public void reset() {
        concreteProtocol.reset();
    }

    @Override
    public void close() throws IOException {
        concreteProtocol.close();
    }
}
