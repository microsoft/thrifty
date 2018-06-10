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

/*
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

package com.microsoft.thrifty.protocol;

import com.microsoft.thrifty.TType;
import com.microsoft.thrifty.transport.Transport;
import okio.Buffer;
import okio.ByteString;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

/**
 * Json protocol implementation for thrift.
 * <p>
 * This is a full-featured protocol supporting write and read.
 */
public class JsonProtocol extends Protocol {

    private static final byte[] COMMA = new byte[]{','};
    private static final byte[] COLON = new byte[]{':'};
    private static final byte[] LBRACE = new byte[]{'{'};
    private static final byte[] RBRACE = new byte[]{'}'};
    private static final byte[] LBRACKET = new byte[]{'['};
    private static final byte[] RBRACKET = new byte[]{']'};
    private static final byte[] QUOTE = new byte[]{'"'};
    private static final byte[] BACKSLASH = new byte[]{'\\'};

    private static final byte[] ESCSEQ = new byte[]{'\\', 'u', '0', '0'};

    private static final long VERSION = 1;

    private static final byte[] JSON_CHAR_TABLE = {
            /*  0 1 2 3 4 5 6 7 8 9 A B C D E F */
            0, 0, 0, 0, 0, 0, 0, 0, 'b', 't', 'n', 0, 'f', 'r', 0, 0, // 0
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 1
            1, 1, '"', 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 2
    };

    private static final String ESCAPE_CHARS = "\"\\/bfnrt";

    private static final byte[] ESCAPE_CHAR_VALS = {
            '"', '\\', '/', '\b', '\f', '\n', '\r', '\t',
    };

    // Stack of nested contexts that we may be in
    private Stack<JsonBaseContext> contextStack = new Stack<>();

    // Current context that we are in
    private JsonBaseContext context = new JsonBaseContext();

    // Reader that manages a 1-byte buffer
    private LookaheadReader reader = new LookaheadReader();

    // Write out the TField names as a string instead of the default integer value
    private boolean fieldNamesAsString = false;

    // Push a new Json context onto the stack.
    private void pushContext(JsonBaseContext c) {
        contextStack.push(context);
        context = c;
    }

    // Pop the last Json context off the stack
    private void popContext() {
        context = contextStack.pop();
    }

    // Reset the context stack to its initial state
    private void resetContext() {
        while (!contextStack.isEmpty()) {
            popContext();
        }
    }

    public JsonProtocol(Transport transport) {
        super(transport);
    }

    public JsonProtocol(Transport transport, boolean fieldNamesAsString) {
        super(transport);
        this.fieldNamesAsString = fieldNamesAsString;
    }

    @Override
    public void reset() {
        contextStack.clear();
        context = new JsonBaseContext();
        reader = new LookaheadReader();
    }

    // Temporary buffer used by several methods
    private byte[] tmpbuf = new byte[4];

    // Read a byte that must match b[0]; otherwise an exception is thrown.
    // Marked protected to avoid synthetic accessor in JsonListContext.read
    // and JsonPairContext.read
    protected void readJsonSyntaxChar(byte[] b) throws IOException {
        byte ch = reader.read();
        if (ch != b[0]) {
            throw new ProtocolException("Unexpected character:" + (char) ch);
        }
    }

    // Convert a byte containing a hex char ('0'-'9' or 'a'-'f') into its
    // corresponding hex value
    private static byte hexVal(byte ch) throws IOException {
        if ((ch >= '0') && (ch <= '9')) {
            return (byte) ((char) ch - '0');
        } else if ((ch >= 'a') && (ch <= 'f')) {
            return (byte) ((char) ch - 'a' + 10);
        } else {
            throw new ProtocolException("Expected hex character");
        }
    }

    // Convert a byte containing a hex value to its corresponding hex character
    private static byte hexChar(byte val) {
        val = (byte) (val & 0x0F);
        if (val < 10) {
            return (byte) ((char) val + '0');
        } else {
            return (byte) ((char) (val - 10) + 'a');
        }
    }

    // Write the bytes in array buf as a Json characters, escaping as needed
    private void writeJsonString(byte[] b) throws IOException {
        context.write();
        transport.write(QUOTE);
        int len = b.length;
        for (int i = 0; i < len; i++) {
            if ((b[i] & 0x00FF) >= 0x30) {
                if (b[i] == BACKSLASH[0]) {
                    transport.write(BACKSLASH);
                    transport.write(BACKSLASH);
                } else {
                    transport.write(b, i, 1);
                }
            } else {
                tmpbuf[0] = JSON_CHAR_TABLE[b[i]];
                if (tmpbuf[0] == 1) {
                    transport.write(b, i, 1);
                } else if (tmpbuf[0] > 1) {
                    transport.write(BACKSLASH);
                    transport.write(tmpbuf, 0, 1);
                } else {
                    transport.write(ESCSEQ);
                    tmpbuf[0] = hexChar((byte) (b[i] >> 4));
                    tmpbuf[1] = hexChar(b[i]);
                    transport.write(tmpbuf, 0, 2);
                }
            }
        }
        transport.write(QUOTE);
    }

    // Write out number as a Json value. If the context dictates so, it will be
    // wrapped in quotes to output as a Json string.
    private void writeJsonInteger(long num) throws IOException {
        context.write();
        String str = Long.toString(num);
        boolean escapeNum = context.escapeNum();
        if (escapeNum) {
            transport.write(QUOTE);
        }
        try {
            byte[] buf = str.getBytes("UTF-8");
            transport.write(buf);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        if (escapeNum) {
            transport.write(QUOTE);
        }
    }

    // Write out a double as a Json value. If it is NaN or infinity or if the
    // context dictates escaping, write out as Json string.
    private void writeJsonDouble(double num) throws IOException {
        context.write();
        String str = Double.toString(num);
        boolean special = false;
        switch (str.charAt(0)) {
            case 'N': // NaN
            case 'I': // Infinity
                special = true;
                break;
            case '-':
                if (str.charAt(1) == 'I') { // -Infinity
                    special = true;
                }
                break;
            default:
                break;
        }

        boolean escapeNum = special || context.escapeNum();
        if (escapeNum) {
            transport.write(QUOTE);
        }
        try {
            byte[] b = str.getBytes("UTF-8");
            transport.write(b, 0, b.length);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        if (escapeNum) {
            transport.write(QUOTE);
        }
    }

    private void writeJsonObjectStart() throws IOException {
        context.write();
        transport.write(LBRACE);
        pushContext(new JsonPairContext());
    }

    private void writeJsonObjectEnd() throws IOException {
        popContext();
        transport.write(RBRACE);
    }

    private void writeJsonArrayStart() throws IOException {
        context.write();
        transport.write(LBRACKET);
        pushContext(new JsonListContext());
    }

    private void writeJsonArrayEnd() throws IOException {
        popContext();
        transport.write(RBRACKET);
    }

    @Override
    public void writeMessageBegin(String name, byte typeId, int seqId) throws IOException {
        resetContext(); // THRIFT-3743
        writeJsonArrayStart();
        writeJsonInteger(VERSION);
        try {
            byte[] b = name.getBytes("UTF-8");
            writeJsonString(b);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        writeJsonInteger(typeId);
        writeJsonInteger(seqId);
    }

    @Override
    public void writeMessageEnd() throws IOException {
        writeJsonArrayEnd();
    }

    @Override
    public void writeStructBegin(String structName) throws IOException {
        writeJsonObjectStart();
    }

    @Override
    public void writeStructEnd() throws IOException {
        writeJsonObjectEnd();
    }

    @Override
    public void writeFieldBegin(String fieldName, int fieldId, byte typeId) throws IOException {
        if (fieldNamesAsString) {
            writeString(fieldName);
        } else {
            writeJsonInteger(fieldId);
        }
        writeJsonObjectStart();
        writeJsonString(JsonTypes.ttypeToJson(typeId));
    }

    @Override
    public void writeFieldEnd() throws IOException {
        writeJsonObjectEnd();
    }

    @Override
    public void writeFieldStop() {
    }

    @Override
    public void writeMapBegin(byte keyTypeId, byte valueTypeId, int mapSize) throws IOException {
        writeJsonArrayStart();
        writeJsonString(JsonTypes.ttypeToJson(keyTypeId));
        writeJsonString(JsonTypes.ttypeToJson(valueTypeId));
        writeJsonInteger(mapSize);
        writeJsonObjectStart();
    }

    @Override
    public void writeMapEnd() throws IOException {
        writeJsonObjectEnd();
        writeJsonArrayEnd();
    }

    @Override
    public void writeListBegin(byte elementTypeId, int listSize) throws IOException {
        writeJsonArrayStart();
        writeJsonString(JsonTypes.ttypeToJson(elementTypeId));
        writeJsonInteger(listSize);
    }

    @Override
    public void writeListEnd() throws IOException {
        writeJsonArrayEnd();
    }

    @Override
    public void writeSetBegin(byte elementTypeId, int setSize) throws IOException {
        writeJsonArrayStart();
        writeJsonString(JsonTypes.ttypeToJson(elementTypeId));
        writeJsonInteger(setSize);
    }

    @Override
    public void writeSetEnd() throws IOException {
        writeJsonArrayEnd();
    }

    @Override
    public void writeBool(boolean b) throws IOException {
        writeJsonInteger(b ? (long) 1 : (long) 0);
    }

    @Override
    public void writeByte(byte b) throws IOException {
        writeJsonInteger((long) b);
    }

    @Override
    public void writeI16(short i16) throws IOException {
        writeJsonInteger((long) i16);
    }

    @Override
    public void writeI32(int i32) throws IOException {
        writeJsonInteger((long) i32);
    }

    @Override
    public void writeI64(long i64) throws IOException {
        writeJsonInteger(i64);
    }

    @Override
    public void writeDouble(double dub) throws IOException {
        writeJsonDouble(dub);
    }

    @Override
    public void writeString(String str) throws IOException {
        try {
            byte[] b = str.getBytes("UTF-8");
            writeJsonString(b);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void writeBinary(ByteString buf) throws IOException {
        writeString(buf.base64());
    }

    /**
     * Reading methods.
     */

    // Read in a Json string, unescaping as appropriate.. Skip reading from the
    // context if skipContext is true.
    private ByteString readJsonString(boolean skipContext)
            throws IOException {
        Buffer buffer = new Buffer();
        ArrayList<Character> codeunits = new ArrayList<>();
        if (!skipContext) {
            context.read();
        }
        readJsonSyntaxChar(QUOTE);
        while (true) {
            byte ch = reader.read();
            if (ch == QUOTE[0]) {
                break;
            }
            if (ch == ESCSEQ[0]) {
                ch = reader.read();
                if (ch == ESCSEQ[1]) {
                    transport.read(tmpbuf, 0, 4);
                    short cu = (short) (
                            ((short) hexVal(tmpbuf[0]) << 12)
                                    + ((short) hexVal(tmpbuf[1]) << 8)
                                    + ((short) hexVal(tmpbuf[2]) << 4)
                                    + (short) hexVal(tmpbuf[3]));
                    try {
                        if (Character.isHighSurrogate((char) cu)) {
                            if (codeunits.size() > 0) {
                                throw new ProtocolException("Expected low surrogate char");
                            }
                            codeunits.add((char) cu);
                        } else if (Character.isLowSurrogate((char) cu)) {
                            if (codeunits.size() == 0) {
                                throw new ProtocolException("Expected high surrogate char");
                            }

                            codeunits.add((char) cu);
                            buffer.write((new String(new int[]{codeunits.get(0), codeunits.get(1)}, 0, 2))
                                    .getBytes("UTF-8"));
                            codeunits.clear();
                        } else {
                            buffer.write((new String(new int[]{cu}, 0, 1)).getBytes("UTF-8"));
                        }
                        continue;
                    } catch (UnsupportedEncodingException e) {
                        throw new AssertionError(e);
                    } catch (IOException ex) {
                        throw new ProtocolException("Invalid unicode sequence");
                    }
                } else {
                    int off = ESCAPE_CHARS.indexOf(ch);
                    if (off == -1) {
                        throw new ProtocolException("Expected control char");
                    }
                    ch = ESCAPE_CHAR_VALS[off];
                }
            }
            buffer.write(new byte[]{ch});
        }
        return buffer.readByteString();
    }

    // Return true if the given byte could be a valid part of a Json number.
    private boolean isJsonNumeric(byte b) {
        switch (b) {
            case '+':
            case '-':
            case '.':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case 'E':
            case 'e':
                return true;
        }
        return false;
    }

    // Read in a sequence of characters that are all valid in Json numbers. Does
    // not do a complete regex check to validate that this is actually a number.
    private String readJsonNumericChars() throws IOException {
        StringBuilder strbld = new StringBuilder();
        while (true) {
            byte ch = reader.peek();
            if (!isJsonNumeric(ch)) {
                break;
            }
            strbld.append((char) reader.read());
        }
        return strbld.toString();
    }

    // Read in a Json number. If the context dictates, read in enclosing quotes.
    private long readJsonInteger() throws IOException {
        context.read();
        if (context.escapeNum()) {
            readJsonSyntaxChar(QUOTE);
        }
        String str = readJsonNumericChars();
        if (context.escapeNum()) {
            readJsonSyntaxChar(QUOTE);
        }
        try {
            return Long.valueOf(str);
        } catch (NumberFormatException ex) {
            throw new ProtocolException("Bad data encountered in numeric data");
        }
    }

    // Read in a Json double value. Throw if the value is not wrapped in quotes
    // when expected or if wrapped in quotes when not expected.
    private double readJsonDouble() throws IOException {
        context.read();
        if (reader.peek() == QUOTE[0]) {
            ByteString str = readJsonString(true);
            double dub = Double.valueOf(str.utf8());
            if (!context.escapeNum() && !Double.isNaN(dub)
                    && !Double.isInfinite(dub)) {
                // Throw exception -- we should not be in a string in this case
                throw new ProtocolException("Numeric data unexpectedly quoted");
            }
            return dub;
        } else {
            if (context.escapeNum()) {
                // This will throw - we should have had a quote if escapeNum == true
                readJsonSyntaxChar(QUOTE);
            }
            try {
                return Double.valueOf(readJsonNumericChars());
            } catch (NumberFormatException ex) {
                throw new ProtocolException("Bad data encountered in numeric data");
            }
        }
    }

    // Read in a Json string containing base-64 encoded data and decode it.
    private ByteString readJsonBase64() throws IOException {
        ByteString str = readJsonString(false);
        return ByteString.decodeBase64(str.utf8());
    }

    private void readJsonObjectStart() throws IOException {
        context.read();
        readJsonSyntaxChar(LBRACE);
        pushContext(new JsonPairContext());
    }

    private void readJsonObjectEnd() throws IOException {
        readJsonSyntaxChar(RBRACE);
        popContext();
    }

    private void readJsonArrayStart() throws IOException {
        context.read();
        readJsonSyntaxChar(LBRACKET);
        pushContext(new JsonListContext());
    }

    private void readJsonArrayEnd() throws IOException {
        readJsonSyntaxChar(RBRACKET);
        popContext();
    }

    @Override
    public MessageMetadata readMessageBegin() throws IOException {
        resetContext(); // THRIFT-3743
        readJsonArrayStart();
        if (readJsonInteger() != VERSION) {
            throw new ProtocolException("Message contained bad version.");
        }
        String name;
        try {
            name = readJsonString(false).utf8();
        } catch (UnsupportedEncodingException ex) {
            throw new AssertionError(ex);
        }
        byte type = (byte) readJsonInteger();
        int seqid = (int) readJsonInteger();
        return new MessageMetadata(name, type, seqid);
    }

    @Override
    public void readMessageEnd() throws IOException {
        readJsonArrayEnd();
    }

    @Override
    public StructMetadata readStructBegin() throws IOException {
        readJsonObjectStart();
        return new StructMetadata("");
    }

    @Override
    public void readStructEnd() throws IOException {
        readJsonObjectEnd();
    }

    @Override
    public FieldMetadata readFieldBegin() throws IOException {
        byte ch = reader.peek();
        byte type;
        short id = 0;
        if (ch == RBRACE[0]) {
            type = TType.STOP;
        } else {
            id = (short) readJsonInteger();
            readJsonObjectStart();
            type = JsonTypes.jsonToTtype(readJsonString(false).toByteArray());
        }
        return new FieldMetadata("", type, id);
    }

    @Override
    public void readFieldEnd() throws IOException {
        readJsonObjectEnd();
    }

    @Override
    public MapMetadata readMapBegin() throws IOException {
        readJsonArrayStart();
        byte keyType = JsonTypes.jsonToTtype(readJsonString(false).toByteArray());
        byte valueType = JsonTypes.jsonToTtype(readJsonString(false).toByteArray());
        int size = (int) readJsonInteger();
        readJsonObjectStart();
        return new MapMetadata(keyType, valueType, size);
    }

    @Override
    public void readMapEnd() throws IOException {
        readJsonObjectEnd();
        readJsonArrayEnd();
    }

    @Override
    public ListMetadata readListBegin() throws IOException {
        readJsonArrayStart();
        byte elemType = JsonTypes.jsonToTtype(readJsonString(false).toByteArray());
        int size = (int) readJsonInteger();
        return new ListMetadata(elemType, size);
    }

    @Override
    public void readListEnd() throws IOException {
        readJsonArrayEnd();
    }

    @Override
    public SetMetadata readSetBegin() throws IOException {
        readJsonArrayStart();
        byte elemType = JsonTypes.jsonToTtype(readJsonString(false).toByteArray());
        int size = (int) readJsonInteger();
        return new SetMetadata(elemType, size);
    }

    @Override
    public void readSetEnd() throws IOException {
        readJsonArrayEnd();
    }

    @Override
    public boolean readBool() throws IOException {
        return (readJsonInteger() == 0 ? false : true);
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) readJsonInteger();
    }

    @Override
    public short readI16() throws IOException {
        return (short) readJsonInteger();
    }

    @Override
    public int readI32() throws IOException {
        return (int) readJsonInteger();
    }

    @Override
    public long readI64() throws IOException {
        return readJsonInteger();
    }

    @Override
    public double readDouble() throws IOException {
        return readJsonDouble();
    }

    @Override
    public String readString() throws IOException {
        return readJsonString(false).utf8();
    }

    @Override
    public ByteString readBinary() throws IOException {
        return readJsonBase64();
    }

    // Holds up to one byte from the transport
    protected class LookaheadReader {

        private boolean hasData;
        private byte[] data = new byte[1];

        // Return and consume the next byte to be read, either taking it from the
        // data buffer if present or getting it from the transport otherwise.
        protected byte read() throws IOException {
            if (hasData) {
                hasData = false;
            } else {
                transport.read(data, 0, 1);
            }
            return data[0];
        }

        // Return the next byte to be read without consuming, filling the data
        // buffer if it has not been filled already.
        protected byte peek() throws IOException {
            if (!hasData) {
                transport.read(data, 0, 1);
            }
            hasData = true;
            return data[0];
        }
    }

    private static final class JsonTypes {
        static final byte[] BOOLEAN = new byte[]{'t', 'f'};
        static final byte[] BYTE = new byte[]{'i', '8'};
        static final byte[] I16 = new byte[]{'i', '1', '6'};
        static final byte[] I32 = new byte[]{'i', '3', '2'};
        static final byte[] I64 = new byte[]{'i', '6', '4'};
        static final byte[] DOUBLE = new byte[]{'d', 'b', 'l'};
        static final byte[] STRUCT = new byte[]{'r', 'e', 'c'};
        static final byte[] STRING = new byte[]{'s', 't', 'r'};
        static final byte[] MAP = new byte[]{'m', 'a', 'p'};
        static final byte[] LIST = new byte[]{'l', 's', 't'};
        static final byte[] SET = new byte[]{'s', 'e', 't'};


        static byte[] ttypeToJson(byte typeId) {
            switch (typeId) {
                case TType.STOP:
                    throw new IllegalArgumentException("Unexpected STOP type");
                case TType.VOID:
                    throw new IllegalArgumentException("Unexpected VOID type");
                case TType.BOOL:
                    return BOOLEAN;
                case TType.BYTE:
                    return BYTE;
                case TType.DOUBLE:
                    return DOUBLE;
                case TType.I16:
                    return I16;
                case TType.I32:
                    return I32;
                case TType.I64:
                    return I64;
                case TType.STRING:
                    return STRING;
                case TType.STRUCT:
                    return STRUCT;
                case TType.MAP:
                    return MAP;
                case TType.SET:
                    return SET;
                case TType.LIST:
                    return LIST;
                default:
                    throw new IllegalArgumentException(
                            "Unknown TType ID: " + typeId);
            }
        }

        static byte jsonToTtype(byte[] jsonId) {
            byte result = TType.STOP;
            if (jsonId.length > 1) {
                switch (jsonId[0]) {
                    case 'd':
                        result = TType.DOUBLE;
                        break;
                    case 'i':
                        switch (jsonId[1]) {
                            case '8':
                                result = TType.BYTE;
                                break;
                            case '1':
                                result = TType.I16;
                                break;
                            case '3':
                                result = TType.I32;
                                break;
                            case '6':
                                result = TType.I64;
                                break;
                        }
                        break;
                    case 'l':
                        result = TType.LIST;
                        break;
                    case 'm':
                        result = TType.MAP;
                        break;
                    case 'r':
                        result = TType.STRUCT;
                        break;
                    case 's':
                        if (jsonId[1] == 't') {
                            result = TType.STRING;
                        } else {
                            result = TType.SET;
                        }
                        break;
                    case 't':
                        result = TType.BOOL;
                        break;
                }
            }
            if (result == TType.STOP) {
                throw new IllegalArgumentException(
                        "Unknown json type ID: " + Arrays.toString(jsonId));
            }
            return result;
        }

        private JsonTypes() {
            throw new AssertionError("no instances");
        }
    }

    // Base class for tracking Json contexts that may require inserting/reading
    // additional Json syntax characters
    // This base context does nothing.
    protected static class JsonBaseContext {
        protected void write() throws IOException {
        }

        protected void read() throws IOException {
        }

        protected boolean escapeNum() {
            return false;
        }
    }

    // Context for Json lists. Will insert/read commas before each item except
    // for the first one
    protected class JsonListContext extends JsonBaseContext {
        private boolean first = true;

        @Override
        protected void write() throws IOException {
            if (first) {
                first = false;
            } else {
                transport.write(COMMA);
            }
        }

        @Override
        protected void read() throws IOException {
            if (first) {
                first = false;
            } else {
                readJsonSyntaxChar(COMMA);
            }
        }
    }

    // Context for Json records. Will insert/read colons before the value portion
    // of each record pair, and commas before each key except the first. In
    // addition, will indicate that numbers in the key position need to be
    // escaped in quotes (since Json keys must be strings).
    protected class JsonPairContext extends JsonBaseContext {
        private boolean first = true;
        private boolean colon = true;

        @Override
        protected void write() throws IOException {
            if (first) {
                first = false;
                colon = true;
            } else {
                transport.write(colon ? COLON : COMMA);
                colon = !colon;
            }
        }

        @Override
        protected void read() throws IOException {
            if (first) {
                first = false;
                colon = true;
            } else {
                readJsonSyntaxChar(colon ? COLON : COMMA);
                colon = !colon;
            }
        }

        @Override
        protected boolean escapeNum() {
            return colon;
        }
    }
}
