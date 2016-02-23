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

import com.microsoft.thrifty.transport.Transport;
import okio.ByteString;

import java.io.Closeable;
import java.io.IOException;

public abstract class Protocol implements Closeable {
    protected final Transport transport;

    protected Protocol(Transport transport) {
        if (transport == null) {
            throw new NullPointerException("transport");
        }
        this.transport = transport;
    }

    public abstract void writeMessageBegin(String name, byte typeId, int seqId) throws IOException;

    public abstract void writeMessageEnd() throws IOException;

    public abstract void writeStructBegin(String structName) throws IOException;

    public abstract void writeStructEnd() throws IOException;

    public abstract void writeFieldBegin(String fieldName, int fieldId, byte typeId) throws IOException;

    public abstract void writeFieldEnd() throws IOException;

    public abstract void writeFieldStop() throws IOException;

    public abstract void writeMapBegin(byte keyTypeId, byte valueTypeId, int mapSize) throws IOException;

    public abstract void writeMapEnd() throws IOException;

    public abstract void writeListBegin(byte elementTypeId, int listSize) throws IOException;

    public abstract void writeListEnd() throws IOException;

    public abstract void writeSetBegin(byte elementTypeId, int setSize) throws IOException;

    public abstract void writeSetEnd() throws IOException;

    public abstract void writeBool(boolean b) throws IOException;

    public abstract void writeByte(byte b) throws IOException;

    public abstract void writeI16(short i16) throws IOException;

    public abstract void writeI32(int i32) throws IOException;

    public abstract void writeI64(long i64) throws IOException;

    public abstract void writeDouble(double dub) throws IOException;

    public abstract void writeString(String str) throws IOException;

    public abstract void writeBinary(ByteString buf) throws IOException;

    ////////

    public abstract MessageMetadata readMessageBegin() throws IOException;

    public abstract void readMessageEnd() throws IOException;

    public abstract StructMetadata readStructBegin() throws IOException;

    public abstract void readStructEnd() throws IOException;

    public abstract FieldMetadata readFieldBegin() throws IOException;

    public abstract void readFieldEnd() throws IOException;

    public abstract MapMetadata readMapBegin() throws IOException;

    public abstract void readMapEnd() throws IOException;

    public abstract ListMetadata readListBegin() throws IOException;

    public abstract void readListEnd() throws IOException;

    public abstract SetMetadata readSetBegin() throws IOException;

    public abstract void readSetEnd() throws IOException;

    public abstract boolean readBool() throws IOException;

    public abstract byte readByte() throws IOException;

    public abstract short readI16() throws IOException;

    public abstract int readI32() throws IOException;

    public abstract long readI64() throws IOException;

    public abstract double readDouble() throws IOException;

    public abstract String readString() throws IOException;

    public abstract ByteString readBinary() throws IOException;

    //////////////

    public void flush() throws IOException {
        transport.flush();
    }

    public void reset() {
        // to be implemented by children as needed
    }

    @Override
    public void close() throws IOException {
        this.transport.close();
    }
}
