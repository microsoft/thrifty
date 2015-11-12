/*
 * Copyright 2015 Ben Bader
 * Copyright Apache Thrift Authors
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

import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public class TSimpleJsonProtocol extends TProtocol {

    private static final byte[] COMMA = { ',' };
    private static final byte[] COLON = { ':' };
    private static final byte[] LBRACE = { '{' };
    private static final byte[] RBRACE = { '}' };
    private static final byte[] LBRACKET = { '[' };
    private static final byte[] RBRACKET = { ']' };

    private final Context BASE_CONTEXT = new Context();
    private final Deque<Context> contextStack = new ArrayDeque<>();

    private Context writeContext = BASE_CONTEXT;

    public TSimpleJsonProtocol(BufferedSource source, BufferedSink sink) {
        super(source, sink);
    }

    private void pushWriteContext(Context context) {
        contextStack.push(writeContext);
        writeContext = context;
    }

    private void popWriteContext() {
        writeContext = contextStack.pop();
    }

    @Override
    public void writeMessageBegin(String name, byte typeId, int seqId) throws IOException {
        sink.write(LBRACKET);
        pushWriteContext(new ListContext());
        writeString(name);
        writeByte(typeId);
        writeI32(seqId);
    }

    @Override
    public void writeMessageEnd() throws IOException {
        popWriteContext();
        sink.write(RBRACKET);
    }

    @Override
    public void writeStructBegin(String structName) throws IOException {
        writeContext.write();
        sink.write(LBRACE);
        pushWriteContext(new StructContext());
    }

    @Override
    public void writeStructEnd() throws IOException {
        popWriteContext();
        sink.write(RBRACE);
    }

    @Override
    public void writeFieldBegin(String fieldName, int fieldId, byte typeId) throws IOException {
        writeString(fieldName);
    }

    @Override
    public void writeFieldEnd() throws IOException {
        // nothing
    }

    @Override
    public void writeFieldStop() throws IOException {
        // nothing
    }

    @Override
    public void writeMapBegin(byte keyTypeId, byte valueTypeId, int mapSize) throws IOException {
        writeContext.write();
        sink.write(LBRACE);
        pushWriteContext(new MapContext());
    }

    @Override
    public void writeMapEnd() throws IOException {
        popWriteContext();
        sink.write(RBRACE);
    }

    @Override
    public void writeListBegin(byte elementTypeId, int listSize) throws IOException {
        writeContext.write();
        sink.write(LBRACKET);
        pushWriteContext(new ListContext());
    }

    @Override
    public void writeListEnd() throws IOException {
        popWriteContext();
        sink.write(RBRACKET);
    }

    @Override
    public void writeSetBegin(byte elementTypeId, int setSize) throws IOException {
        writeContext.write();
        sink.write(LBRACKET);
        pushWriteContext(new ListContext());
    }

    @Override
    public void writeSetEnd() throws IOException {
        popWriteContext();
        sink.write(RBRACKET);
    }

    @Override
    public void writeBool(boolean b) throws IOException {
        writeByte(b ? (byte) 1 : (byte) 0);
    }

    @Override
    public void writeByte(byte b) throws IOException {
        writeI32(b);
    }

    @Override
    public void writeI16(short i16) throws IOException {
        writeI32(i16);
    }

    @Override
    public void writeI32(int i32) throws IOException {
        String text = Integer.toString(i32);
        writeText(text);
    }

    @Override
    public void writeI64(long i64) throws IOException {
        String text = Long.toString(i64);
        writeText(text);
    }

    @Override
    public void writeDouble(double dub) throws IOException {
        String text = Double.toString(dub);
        writeText(text);
    }

    @Override
    public void writeString(String str) throws IOException {
        writeContext.write();
        int len = str.length();
        StringBuilder sb = new StringBuilder(len);
        sb.append('"');
        for (int i = 0; i < len; ++i) {
            char c = str.charAt(i);
            switch (c) {
                case '"':
                case '\\':
                    sb.append('\\');
                    sb.append(c);
                    break;
                case '\b':
                    sb.append('\\');
                    sb.append('b');
                    break;
                case '\f':
                    sb.append('\\');
                    sb.append('f');
                    break;
                case '\n':
                    sb.append('\\');
                    sb.append('n');
                    break;
                case '\r':
                    sb.append('\\');
                    sb.append('r');
                    break;
                case '\t':
                    sb.append('\\');
                    sb.append('t');
                    break;
                default:
                    if (c < ' ') {
                        sb.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int j = 4; j < hex.length(); --j) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        sb.append('"');
        writeText(sb.toString());
    }

    @Override
    public void writeBinary(ByteString buf) throws IOException {
        writeString(buf.hex());
    }

    private void writeText(String text) throws IOException {
        if (writeContext.isMapKey()) {
            writeString(text);
        } else {
            writeContext.write();
            sink.writeUtf8(text);
        }
    }

    private class Context {
        protected void write() throws IOException {}

        protected boolean isMapKey() {
            return false;
        }
    }

    private class ListContext extends Context {
        private boolean first = true;

        @Override
        protected void write() throws IOException {
            if (first) {
                first = false;
            } else {
                sink.write(COMMA);
            }
        }
    }

    private class StructContext extends Context {
        private boolean first = true;
        private boolean colon = true;

        @Override
        protected void write() throws IOException {
            if (first) {
                first = false;
            } else {
                sink.write(colon ? COLON : COMMA);
                colon = !colon;
            }
        }
    }

    private class MapContext extends StructContext {
        private boolean isKey = true;

        @Override
        protected void write() throws IOException {
            super.write();
            isKey = !isKey;
        }

        @Override
        protected boolean isMapKey() {
            return isKey;
        }
    }
}
