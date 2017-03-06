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
import com.microsoft.thrifty.util.UnsafeByteArrayOutputStream;
import okio.ByteString;

import java.io.IOException;
import java.net.ProtocolException;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A protocol that maps Thrift data to idiomatic JSON.
 *
 * <p>"Idiomatic" here means that structs map to JSON maps, with field names
 * for keys.  Field tags are not included, and precise type information is not
 * preserved.  For this reason, SimpleJsonProtocol <em>does not support round-
 * tripping</em> - it is write-only.
 *
 * <p>Note that, as of the initial release, this Protocol does not guarantee
 * that all emitted data is strictly valid JSON.  In particular, map keys are
 * not guaranteed to to be strings.
 */
public class SimpleJsonProtocol extends Protocol {
    /**
     * Indicates how {@linkplain ByteString binary} data is serialized when
     * written as JSON.
     */
    public enum BinaryOutputMode {
        /**
         * Write binary data as a hex-encoded string.
         */
        HEX,

        /**
         * Write binary data as a base-64-encoded string.
         */
        BASE_64,

        /**
         * Write binary data using Unicode escape syntax.
         */
        UNICODE,
    }

    private static class WriteContext {
        void beforeWrite() throws IOException {

        }

        void onPop() throws IOException {
            // Fine
        }
    }

    private class ListWriteContext extends WriteContext {
        private boolean hasWritten = false;

        @Override
        void beforeWrite() throws IOException {
            if (hasWritten) {
                transport.write(COMMA);
            } else {
                hasWritten = true;
            }
        }
    }

    private class MapWriteContext extends WriteContext {
        private static final boolean MODE_KEY = false;
        private static final boolean MODE_VALUE = true;

        private boolean hasWritten = false;
        private boolean mode = MODE_KEY;

        @Override
        void beforeWrite() throws IOException {
            if (hasWritten) {
                if (mode == MODE_KEY) {
                    transport.write(COMMA);
                } else {
                    transport.write(COLON);
                }
            } else {
                hasWritten = true;
            }

            mode = !mode;
        }

        @Override
        void onPop() throws IOException {
            if (mode == MODE_VALUE) {
                throw new ProtocolException("Incomplete JSON map, expected a value");
            }
        }
    }

    private static final byte[][] ESCAPES;
    private static final Charset UTF8;

    private static final byte[] TRUE = { 't', 'r', 'u', 'e' };
    private static final byte[] FALSE = { 'f', 'a', 'l', 's', 'e' };
    private static final byte[] COMMA = { ',' };
    private static final byte[] COMMA_SPACE = { ',', ' ' };
    private static final byte[] COLON = { ':' };
    private static final byte[] LBRACKET = { '[' };
    private static final byte[] RBRACKET = { ']' };
    private static final byte[] LBRACE = { '{' };
    private static final byte[] RBRACE = { '}' };

    static {
        ESCAPES = new byte[128][];

        UTF8 = Charset.forName("UTF-8");
        for (int i = 0; i < 32; ++i) {
            // Control chars must be escaped
            ESCAPES[i] = String.format("\\u%04x", i).getBytes(UTF8);
        }

        ESCAPES['\\'] = new byte[] { '\\', '\\' };
        ESCAPES['\"'] = new byte[] { '\\', '"' };
        ESCAPES['\b'] = new byte[] { '\\', 'b' };
        ESCAPES['\f'] = new byte[] { '\\', 'f' };
        ESCAPES['\r'] = new byte[] { '\\', 'r' };
        ESCAPES['\n'] = new byte[] { '\\', 'n' };
        ESCAPES['\t'] = new byte[] { '\\', 't' };
    }

    private final WriteContext defaultWriteContext = new WriteContext() {
        @Override
        void beforeWrite() throws IOException {
            // nothing
        }
    };

    private Deque<WriteContext> writeStack = new ArrayDeque<>();

    private BinaryOutputMode binaryOutputMode = BinaryOutputMode.HEX;

    public SimpleJsonProtocol(Transport transport) {
        super(transport);
    }

    public SimpleJsonProtocol withBinaryOutputMode(BinaryOutputMode mode) {
        binaryOutputMode = mode;
        return this;
    }

    @Override
    public void writeMessageBegin(String name, byte typeId, int seqId) throws IOException {
        writeMapBegin(typeId, typeId, 0); // values are ignored here
        writeString("name");
        writeString(name);

        writeString("value");
    }

    @Override
    public void writeMessageEnd() throws IOException {
        writeMapEnd();
    }

    @Override
    public void writeStructBegin(String structName) throws IOException {
        writeContext().beforeWrite();
        pushWriteContext(new MapWriteContext());
        transport.write(LBRACE);

        writeString("__thriftStruct");
        writeString(structName);
    }

    @Override
    public void writeStructEnd() throws IOException {
        transport.write(RBRACE);
        popWriteContext();
    }

    @Override
    public void writeFieldBegin(String fieldName, int fieldId, byte typeId) throws IOException {
        // TODO: assert that we're in map context
        writeString(fieldName);
    }

    @Override
    public void writeFieldEnd() throws IOException {

    }

    @Override
    public void writeFieldStop() throws IOException {

    }

    @Override
    public void writeMapBegin(byte keyTypeId, byte valueTypeId, int mapSize) throws IOException {
        writeContext().beforeWrite();
        pushWriteContext(new MapWriteContext());
        transport.write(LBRACE);
    }

    @Override
    public void writeMapEnd() throws IOException {
        transport.write(RBRACE);
        popWriteContext();
    }

    @Override
    public void writeListBegin(byte elementTypeId, int listSize) throws IOException {
        writeContext().beforeWrite();
        pushWriteContext(new ListWriteContext());
        transport.write(LBRACKET);
    }

    @Override
    public void writeListEnd() throws IOException {
        transport.write(RBRACKET);
        popWriteContext();
    }

    @Override
    public void writeSetBegin(byte elementTypeId, int setSize) throws IOException {
        writeContext().beforeWrite();
        pushWriteContext(new ListWriteContext());
        transport.write(LBRACKET);
    }

    @Override
    public void writeSetEnd() throws IOException {
        transport.write(RBRACKET);
        popWriteContext();
    }

    @Override
    public void writeBool(boolean b) throws IOException {
        writeContext().beforeWrite();
        transport.write(b ? TRUE : FALSE);
    }

    @Override
    public void writeByte(byte b) throws IOException {
        writeContext().beforeWrite();
        byte[] toWrite = String.valueOf(b).getBytes(UTF8);
        transport.write(toWrite);
    }

    @Override
    public void writeI16(short i16) throws IOException {
        writeContext().beforeWrite();
        transport.write(String.valueOf(i16).getBytes(UTF8));
    }

    @Override
    public void writeI32(int i32) throws IOException {
        writeContext().beforeWrite();
        transport.write(String.valueOf(i32).getBytes(UTF8));
    }

    @Override
    public void writeI64(long i64) throws IOException {
        writeContext().beforeWrite();
        transport.write(String.valueOf(i64).getBytes(UTF8));
    }

    @Override
    public void writeDouble(double dub) throws IOException {
        writeContext().beforeWrite();
        transport.write(String.valueOf(dub).getBytes(UTF8));
    }

    @Override
    public void writeString(String str) throws IOException {
        writeContext().beforeWrite();

        int len = str.length();
        UnsafeByteArrayOutputStream baos = new UnsafeByteArrayOutputStream(len);

        baos.write('"');
        for (int i = 0; i < len; ++i) {
            char c = str.charAt(i);
            if (c < 128) {
                byte[] maybeEscape = ESCAPES[c];
                if (maybeEscape != null) {
                    baos.write(maybeEscape);
                } else {
                    baos.write(c);
                }
            } else {
                baos.write(c);
            }
        }
        baos.write('"');

        transport.write(baos.getBuffer(), 0, baos.size());
    }

    @Override
    public void writeBinary(ByteString buf) throws IOException {
        String out;
        switch (binaryOutputMode) {
            case HEX:     out = buf.hex();    break;
            case BASE_64: out = buf.base64(); break;
            case UNICODE: out = buf.utf8();   break;
            default:
                throw new AssertionError("Unexpected BinaryOutputMode value: " + binaryOutputMode);
        }
        writeString(out);
    }

    private void pushWriteContext(WriteContext context) {
        writeStack.push(context);
    }

    private WriteContext writeContext() {
        WriteContext top = writeStack.peek();
        if (top == null) {
            top = defaultWriteContext;
        }
        return top;
    }

    private void popWriteContext() throws IOException {
        WriteContext context = writeStack.pollFirst();
        if (context == null) {
            throw new ProtocolException("stack underflow");
        } else {
            context.onPop();
        }
    }

    @Override
    public MessageMetadata readMessageBegin() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readMessageEnd() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public StructMetadata readStructBegin() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readStructEnd() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FieldMetadata readFieldBegin() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readFieldEnd() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MapMetadata readMapBegin() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readMapEnd() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListMetadata readListBegin() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readListEnd() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SetMetadata readSetBegin() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readSetEnd() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean readBool() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte readByte() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public short readI16() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readI32() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long readI64() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public double readDouble() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String readString() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteString readBinary() throws IOException {
        throw new UnsupportedOperationException();
    }
}
