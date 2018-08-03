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
package com.microsoft.thrifty.integration.conformance;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import com.microsoft.thrifty.protocol.DecoratingProtocol;
import com.microsoft.thrifty.protocol.FieldMetadata;
import com.microsoft.thrifty.protocol.JsonProtocol;
import com.microsoft.thrifty.protocol.ListMetadata;
import com.microsoft.thrifty.protocol.MapMetadata;
import com.microsoft.thrifty.protocol.MessageMetadata;
import com.microsoft.thrifty.protocol.Protocol;
import com.microsoft.thrifty.protocol.SetMetadata;
import com.microsoft.thrifty.protocol.StructMetadata;
import com.microsoft.thrifty.testing.ServerProtocol;
import com.microsoft.thrifty.testing.ServerTransport;
import com.microsoft.thrifty.transport.Transport;
import okio.ByteString;

import java.io.IOException;

public class JsonProtocolConformance extends ConformanceBase {
    @Override
    protected ServerTransport getServerTransport() {
        return ServerTransport.BLOCKING;
    }

    @Override
    protected ServerProtocol getServerProtocol() {
        return ServerProtocol.JSON;
    }

    @Override
    protected Protocol createProtocol(Transport transport) {
        //return new JsonProtocol(transport);
        return new LoggingProtocol(new JsonProtocol(transport));
    }
}

class LoggingProtocol extends DecoratingProtocol {
    LoggingProtocol(Protocol protocol) {
        super(protocol);
    }

    @FormatMethod
    private void print(@FormatString String message, Object... args) {
        System.out.println(String.format(message, args));
    }

    @Override
    public void writeMessageBegin(String name, byte typeId, int seqId) throws IOException {
        print("writeMessageBegin(%s, %d, %d)", name, typeId, seqId);
        super.writeMessageBegin(name, typeId, seqId);
    }

    @Override
    public void writeMessageEnd() throws IOException {
        print("writeMessageEnd()");
        super.writeMessageEnd();
    }

    @Override
    public void writeStructBegin(String structName) throws IOException {
        print("writeStructBegin(%s)", structName);
        super.writeStructBegin(structName);
    }

    @Override
    public void writeStructEnd() throws IOException {
        print("writeStructEnd()");
        super.writeStructEnd();
    }

    @Override
    public void writeFieldBegin(String fieldName, int fieldId, byte typeId) throws IOException {
        print("writeFieldBegin(%s, %d, %d)", fieldName, fieldId, typeId);
        super.writeFieldBegin(fieldName, fieldId, typeId);
    }

    @Override
    public void writeFieldEnd() throws IOException {
        print("writeFieldEnd()");
        super.writeFieldEnd();
    }

    @Override
    public void writeFieldStop() throws IOException {
        print("writeFieldStop()");
        super.writeFieldStop();
    }

    @Override
    public void writeMapBegin(byte keyTypeId, byte valueTypeId, int mapSize) throws IOException {
        print("writeMapBegin(%d, %d, %d)", keyTypeId, valueTypeId, mapSize);
        super.writeMapBegin(keyTypeId, valueTypeId, mapSize);
    }

    @Override
    public void writeMapEnd() throws IOException {
        print("writeMapEnd()");
        super.writeMapEnd();
    }

    @Override
    public void writeListBegin(byte elementTypeId, int listSize) throws IOException {
        print("writeListBegin(%d, %d)", elementTypeId, listSize);
        super.writeListBegin(elementTypeId, listSize);
    }

    @Override
    public void writeListEnd() throws IOException {
        print("writeListEnd()");
        super.writeListEnd();
    }

    @Override
    public void writeSetBegin(byte elementTypeId, int setSize) throws IOException {
        print("writeSetBegin(%d, %d)", elementTypeId, setSize);
        super.writeSetBegin(elementTypeId, setSize);
    }

    @Override
    public void writeSetEnd() throws IOException {
        print("writeSetEnd()");
        super.writeSetEnd();
    }

    @Override
    public void writeBool(boolean b) throws IOException {
        print("writeBool(%s)", b);
        super.writeBool(b);
    }

    @Override
    public void writeByte(byte b) throws IOException {
        print("writeByte(%s)", b);
        super.writeByte(b);
    }

    @Override
    public void writeI16(short i16) throws IOException {
        print("writeI16(%s)", i16);
        super.writeI16(i16);
    }

    @Override
    public void writeI32(int i32) throws IOException {
        print("writeI32(%s)", i32);
        super.writeI32(i32);
    }

    @Override
    public void writeI64(long i64) throws IOException {
        print("writeI64(%s)", i64);
        super.writeI64(i64);
    }

    @Override
    public void writeDouble(double dub) throws IOException {
        print("writeDouble(%s)", dub);
        super.writeDouble(dub);
    }

    @Override
    public void writeString(String str) throws IOException {
        print("writeString(%s)", str);
        super.writeString(str);
    }

    @Override
    public void writeBinary(ByteString buf) throws IOException {
        print("writeBinary(%s)", buf.hex());
        super.writeBinary(buf);
    }

    @Override
    public MessageMetadata readMessageBegin() throws IOException {
        print("readMessageBegin()");
        return super.readMessageBegin();
    }

    @Override
    public void readMessageEnd() throws IOException {
        print("readMessageEnd()");
        super.readMessageEnd();
    }

    @Override
    public StructMetadata readStructBegin() throws IOException {
        print("readStructBegin()");
        return super.readStructBegin();
    }

    @Override
    public void readStructEnd() throws IOException {
        print("readStructEnd()");
        super.readStructEnd();
    }

    @Override
    public FieldMetadata readFieldBegin() throws IOException {
        print("readFieldBegin()");
        return super.readFieldBegin();
    }

    @Override
    public void readFieldEnd() throws IOException {
        print("readFieldEnd()");
        super.readFieldEnd();
    }

    @Override
    public MapMetadata readMapBegin() throws IOException {
        print("readMapBegin()");
        return super.readMapBegin();
    }

    @Override
    public void readMapEnd() throws IOException {
        print("readMapEnd()");
        super.readMapEnd();
    }

    @Override
    public ListMetadata readListBegin() throws IOException {
        print("readListBegin()");
        return super.readListBegin();
    }

    @Override
    public void readListEnd() throws IOException {
        print("readListEnd()");
        super.readListEnd();
    }

    @Override
    public SetMetadata readSetBegin() throws IOException {
        print("readSetBegin()");
        return super.readSetBegin();
    }

    @Override
    public void readSetEnd() throws IOException {
        print("readSetEnd()");
        super.readSetEnd();
    }

    @Override
    public boolean readBool() throws IOException {
        print("readBool()");
        return super.readBool();
    }

    @Override
    public byte readByte() throws IOException {
        print("readByte()");
        return super.readByte();
    }

    @Override
    public short readI16() throws IOException {
        print("readI16()");
        return super.readI16();
    }

    @Override
    public int readI32() throws IOException {
        print("readI32()");
        return super.readI32();
    }

    @Override
    public long readI64() throws IOException {
        print("readI64()");
        return super.readI64();
    }

    @Override
    public double readDouble() throws IOException {
        print("readDouble()");
        return super.readDouble();
    }

    @Override
    public String readString() throws IOException {
        print("readString()");
        return super.readString();
    }

    @Override
    public ByteString readBinary() throws IOException {
        print("readBinary()");
        return super.readBinary();
    }
}
