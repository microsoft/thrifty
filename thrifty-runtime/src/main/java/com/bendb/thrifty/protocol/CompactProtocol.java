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
 *
 * This file is derived from the file TCompactProtocol.java, in the Apache
 * Thrift implementation.  The original license header is reproduced
 * below:
 */

/*
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
import com.bendb.thrifty.transport.Transport;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;

import java.io.IOException;
import java.net.ProtocolException;

public class CompactProtocol extends Protocol {

    // Constants, as defined in TCompactProtocol.java

    private static final byte PROTOCOL_ID = (byte) 0x82;
    private static final byte VERSION = 1;
    private static final byte VERSION_MASK = 0x1F;
    private static final byte TYPE_MASK = (byte) 0xE0;
    private static final byte TYPE_BITS = 0x07;
    private static final int  TYPE_SHIFT_AMOUNT = 5;

    private static final StructMetadata NO_STRUCT = new StructMetadata("");
    private static final FieldMetadata END_FIELDS = new FieldMetadata("", TType.STOP, (short) 0);

    // Boolean fields get special treatment - their value is encoded
    // directly in the field header.  As such, when a boolean field
    // header is written, we cache it here until we get the value from
    // the subsequent `writeBool` call.
    private int booleanFieldId = -1;

    // Similarly, we cache the value read from a field header until
    // the `readBool` call.
    private byte booleanFieldType = -1;

    // Keep track of the most-recently-written fields,
    // used for delta-encoding.
    private ShortStack writingFields = new ShortStack();
    private short lastWritingField;

    private ShortStack readingFields = new ShortStack();
    private short lastReadingField;

    public CompactProtocol(Transport transport) {
        super(transport);
    }

    public CompactProtocol(BufferedSource source, BufferedSink sink) {
        super(source, sink);
    }

    @Override
    public void writeMessageBegin(String name, byte typeId, int seqId) throws IOException {
        sink.writeByte(PROTOCOL_ID);
        sink.writeByte((VERSION & VERSION_MASK) | ((typeId << TYPE_SHIFT_AMOUNT) & TYPE_MASK));
        writeVarint32(seqId);
        writeString(name);
    }

    @Override
    public void writeMessageEnd() throws IOException {
        // no wire representation
    }

    @Override
    public void writeStructBegin(String structName) throws IOException {
        writingFields.push(lastWritingField);
        lastWritingField = 0;
    }

    @Override
    public void writeStructEnd() throws IOException {
        lastWritingField = writingFields.pop();
    }

    @Override
    public void writeFieldBegin(String fieldName, int fieldId, byte typeId) throws IOException {
        if (typeId == TType.BOOL) {
            if (booleanFieldId != -1) {
                throw new IllegalStateException("wtf");
            }
            booleanFieldId = fieldId;
        } else {
            writeFieldBegin(fieldId, CompactTypes.ttypeToCompact(typeId));
        }
    }

    private void writeFieldBegin(int fieldId, byte compactTypeId) throws IOException {
        // Can we delta-encode the field ID?
        if (fieldId > lastWritingField && fieldId - lastWritingField <= 15) {
            sink.writeByte((fieldId - lastWritingField) << 4 | compactTypeId);
        } else {
            writeByte(compactTypeId);
            writeI16((short) fieldId);
        }

        lastWritingField = (short) fieldId;
    }

    @Override
    public void writeFieldEnd() throws IOException {
        // no wire representation
    }

    @Override
    public void writeFieldStop() throws IOException {
        writeByte(TType.STOP);
    }

    @Override
    public void writeMapBegin(byte keyTypeId, byte valueTypeId, int mapSize) throws IOException {
        if (mapSize == 0) {
            writeByte((byte) 0);
        } else {
            byte compactKeyType = CompactTypes.ttypeToCompact(keyTypeId);
            byte compactValueType = CompactTypes.ttypeToCompact(valueTypeId);

            writeVarint32(mapSize);
            writeByte((byte) ((compactKeyType << 4) | compactValueType));
        }
    }

    @Override
    public void writeMapEnd() throws IOException {
        // no wire representation
    }

    @Override
    public void writeListBegin(byte elementTypeId, int listSize) throws IOException {
        writeVectorBegin(elementTypeId, listSize);
    }

    @Override
    public void writeListEnd() throws IOException {
        // no wire representation
    }

    @Override
    public void writeSetBegin(byte elementTypeId, int setSize) throws IOException {
        writeVectorBegin(elementTypeId, setSize);
    }

    @Override
    public void writeSetEnd() throws IOException {
        // no wire representation
    }

    @Override
    public void writeBool(boolean b) throws IOException {
        byte compactValue = b ? CompactTypes.BOOLEAN_TRUE : CompactTypes.BOOLEAN_FALSE;
        if (booleanFieldId != -1) {
            // We are writing a boolean field, and need to write the
            // deferred field header.  In this case we encode the value
            // directly in the header's type field.
            writeFieldBegin(booleanFieldId, compactValue);
            booleanFieldId = -1;
        } else {
            // We are not writing a field - just write the value directly.
            sink.writeByte(compactValue);
        }
    }

    @Override
    public void writeByte(byte b) throws IOException {
        sink.writeByte(b);
    }

    @Override
    public void writeI16(short i16) throws IOException {
        writeVarint32(intToZigZag(i16));
    }

    @Override
    public void writeI32(int i32) throws IOException {
        writeVarint32(intToZigZag(i32));
    }

    @Override
    public void writeI64(long i64) throws IOException {
        writeVarint64(longToZigZag(i64));
    }

    @Override
    public void writeDouble(double dub) throws IOException {
        long doubleBits = Double.doubleToLongBits(dub);
        sink.writeLongLe(doubleBits);
    }

    @Override
    public void writeString(String str) throws IOException {
        Buffer buffer = new Buffer();
        buffer.writeUtf8(str);

        if (buffer.size() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("How did you even get a string this big");
        }

        writeVarint32((int) buffer.size());
        sink.writeAll(buffer);
    }

    @Override
    public void writeBinary(ByteString buf) throws IOException {
        writeVarint32(buf.size());
        sink.write(buf);
    }

    private void writeVectorBegin(byte typeId, int size) throws IOException {
        byte compactId = CompactTypes.ttypeToCompact(typeId);
        if (size <= 14) {
            sink.writeByte((size << 4) | compactId);
        } else {
            sink.writeByte(0xF0 | compactId);
            writeVarint32(size);
        }
    }

    private void writeVarint32(int n) throws IOException {
        while (true) {
            if ((n & ~0x7F) == 0x00) {
                sink.writeByte(n);
                return;
            } else {
                sink.writeByte((n & 0x7F) | 0x80);
                n >>>= 7;
            }
        }
    }

    private void writeVarint64(long n) throws IOException {
        while (true) {
            if ((n & ~0x7FL) == 0x00L) {
                sink.writeByte((int) n);
                return;
            } else {
                sink.writeByte((byte) ((n & 0x7F) | 0x80));
                n >>>= 7;
            }
        }
    }

    /**
     * Convert a twos-complement int to zigzag encoding,
     * allowing negative values to be written as varints.
     */
    private static int intToZigZag(int n) {
        return (n << 1) ^ (n >> 31);
    }

    /**
     * Convert a twos-complement long to zigzag encoding,
     * allowing negative values to be written as varints.
     */
    private static long longToZigZag(long n) {
        return (n << 1) ^ (n >> 63);
    }

    @Override
    public MessageMetadata readMessageBegin() throws IOException {
        byte protocolId = readByte();
        if (protocolId != PROTOCOL_ID) {
            throw new ProtocolException(
                    "Expected protocol ID " + Integer.toHexString(PROTOCOL_ID)
                    + " but got " + Integer.toHexString(protocolId));
        }

        byte versionAndType = readByte();
        byte version = (byte) (VERSION_MASK & versionAndType);
        if (version != VERSION) {
            throw new ProtocolException(
                    "Version mismatch; expected version " + VERSION
                    + " but got " + version);
        }

        byte typeId = (byte) ((versionAndType >> TYPE_SHIFT_AMOUNT) & TYPE_BITS);
        int seqId = readVarint32();
        String name = readString();

        return new MessageMetadata(name, typeId, seqId);
    }

    @Override
    public void readMessageEnd() throws IOException {

    }

    @Override
    public StructMetadata readStructBegin() throws IOException {
        readingFields.push(lastReadingField);
        lastReadingField = 0;
        return NO_STRUCT;
    }

    @Override
    public void readStructEnd() throws IOException {
        lastReadingField = readingFields.pop();
    }

    @Override
    public FieldMetadata readFieldBegin() throws IOException {
        byte compactId = readByte();
        byte typeId = CompactTypes.compactToTtype((byte) (compactId & 0x0F));
        if (compactId == TType.STOP) {
            return END_FIELDS;
        }

        short fieldId;
        short modifier = (short) ((compactId & 0xF0) >> 4);
        if (modifier == 0) {
            // This is not a field-ID delta - read the entire ID.
            fieldId = readI16();
        } else {
            fieldId = (short) (lastReadingField + modifier);
        }

        if (typeId == TType.BOOL) {
            booleanFieldType = compactId;
        }

        lastReadingField = fieldId;

        return new FieldMetadata("", typeId, fieldId);
    }

    @Override
    public void readFieldEnd() throws IOException {

    }

    @Override
    public MapMetadata readMapBegin() throws IOException {
        int size = readVarint32();
        byte keyAndValueTypes = size == 0 ? 0 : readByte();

        byte keyType = CompactTypes.compactToTtype((byte) ((keyAndValueTypes >> 4) & 0x0F));
        byte valueType = CompactTypes.compactToTtype((byte) (keyAndValueTypes & 0x0F));

        return new MapMetadata(keyType, valueType, size);
    }

    @Override
    public void readMapEnd() throws IOException {
        // Nothing on the wire
    }

    @Override
    public ListMetadata readListBegin() throws IOException {
        byte sizeAndType = readByte();
        int size = (sizeAndType >> 4) & 0x0F;
        if (size == 0x0F) {
            size = readVarint32();
        }
        byte compactType = (byte) (sizeAndType & 0x0F);
        byte ttype = CompactTypes.compactToTtype(compactType);
        return new ListMetadata(ttype, size);
    }

    @Override
    public void readListEnd() throws IOException {
        // Nothing on the wire
    }

    @Override
    public SetMetadata readSetBegin() throws IOException {
        byte sizeAndType = readByte();
        int size = (sizeAndType >> 4) & 0x0F;
        if (size == 0x0F) {
            size = readVarint32();
        }
        byte compactType = (byte) (sizeAndType & 0x0F);
        byte ttype = CompactTypes.compactToTtype(compactType);
        return new SetMetadata(ttype, size);
    }

    @Override
    public void readSetEnd() throws IOException {
        // Nothing on the wire
    }

    @Override
    public boolean readBool() throws IOException {
        byte compactId;
        if (booleanFieldType != -1) {
            compactId = booleanFieldType;
            booleanFieldType = -1;
        } else {
            compactId = readByte();
        }

        return compactId == CompactTypes.BOOLEAN_TRUE;
    }

    @Override
    public byte readByte() throws IOException {
        return source.readByte();
    }

    @Override
    public short readI16() throws IOException {
        return (short) zigZagToInt(readVarint32());
    }

    @Override
    public int readI32() throws IOException {
        return zigZagToInt(readVarint32());
    }

    @Override
    public long readI64() throws IOException {
        return zigZagToLong(readVarint64());
    }

    @Override
    public double readDouble() throws IOException {
        long bits = source.readLongLe();
        return Double.longBitsToDouble(bits);
    }

    @Override
    public String readString() throws IOException {
        int length = readVarint32();
        return length == 0
                ? ""
                : source.readUtf8(length);
    }

    @Override
    public ByteString readBinary() throws IOException {
        int length = readVarint32();
        return length == 0
                ? ByteString.EMPTY
                : source.readByteString(length);
    }

    private int readVarint32() throws IOException {
        int result = 0;
        int shift = 0;
        while (true) {
            byte b = source.readByte();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) != 0x80) {
                return result;
            }
            shift += 7;
        }
    }

    private long readVarint64() throws IOException {
        long result = 0;
        int shift = 0;
        while (true) {
            byte b = source.readByte();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) != 0x80) {
                return result;
            }
            shift += 7;
        }
    }

    private static int zigZagToInt(int n) {
        return (n >>> 1) ^ -(n & 1);
    }

    private static long zigZagToLong(long n) {
        return (n >>> 1) ^ -(n & 1);
    }

    private static final class CompactTypes {
        public static final byte BOOLEAN_TRUE   = 0x01;
        public static final byte BOOLEAN_FALSE  = 0x02;
        public static final byte BYTE           = 0x03;
        public static final byte I16            = 0x04;
        public static final byte I32            = 0x05;
        public static final byte I64            = 0x06;
        public static final byte DOUBLE         = 0x07;
        public static final byte BINARY         = 0x08;
        public static final byte LIST           = 0x09;
        public static final byte SET            = 0x0A;
        public static final byte MAP            = 0x0B;
        public static final byte STRUCT         = 0x0C;

        public static byte ttypeToCompact(byte typeId) {
            switch (typeId) {
                case TType.STOP:   return TType.STOP;
                case TType.VOID:   throw new IllegalArgumentException("Unexpected VOID type");
                case TType.BOOL:   return BOOLEAN_TRUE;
                case TType.BYTE:   return BYTE;
                case TType.DOUBLE: return DOUBLE;
                case TType.I16:    return I16;
                case TType.I32:    return I32;
                case TType.I64:    return I64;
                case TType.STRING: return BINARY;
                case TType.STRUCT: return STRUCT;
                case TType.MAP:    return MAP;
                case TType.SET:    return SET;
                case TType.LIST:   return LIST;
                case TType.ENUM:   return I32;
                default:
                    throw new IllegalArgumentException(
                            "Unknown TType ID: " + typeId);
            }
        }

        public static byte compactToTtype(byte compactId) {
            switch (compactId) {
                case TType.STOP:    return TType.STOP;
                case BOOLEAN_TRUE:  return TType.BOOL;
                case BOOLEAN_FALSE: return TType.BOOL;
                case BYTE:          return TType.BYTE;
                case I16:           return TType.I16;
                case I32:           return TType.I32;
                case I64:           return TType.I64;
                case DOUBLE:        return TType.DOUBLE;
                case BINARY:        return TType.STRING;
                case LIST:          return TType.LIST;
                case SET:           return TType.SET;
                case MAP:           return TType.MAP;
                case STRUCT:        return TType.STRUCT;
                default:
                    throw new IllegalArgumentException(
                            "Unknown compact type ID: " + compactId);
            }
        }

        private CompactTypes() {
            throw new AssertionError("no instances");
        }
    }

    private static final class ShortStack {
        private short[] stack;
        private int top ;

        ShortStack() {
            stack = new short[16];
            top = -1;
        }

        void push(short value) {
            if (top + 1 == stack.length) {
                short[] biggerStack = new short[stack.length << 1];
                System.arraycopy(stack, 0, biggerStack, 0, stack.length);
                stack = biggerStack;
            }

            stack[++top] = value;
        }

        short pop() {
            return stack[top--];
        }
    }
}
