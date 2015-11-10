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

    private final Deque<Context> contextStack = new ArrayDeque<>();

    public TSimpleJsonProtocol(BufferedSource source, BufferedSink sink) {
        super(source, sink);
    }

    @Override
    public void writeMessageBegin(String name, byte typeId, int seqId) throws IOException {

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

    }

    @Override
    public void writeFieldEnd() throws IOException {

    }

    @Override
    public void writeFieldStop() throws IOException {

    }

    @Override
    public void writeMapBegin(byte keyTypeId, byte valueTypeId, int mapSize) throws IOException {

    }

    @Override
    public void writeMapEnd() throws IOException {

    }

    @Override
    public void writeListBegin(byte elementTypeId, int listSize) throws IOException {

    }

    @Override
    public void writeListEnd() throws IOException {

    }

    @Override
    public void writeSetBegin(byte elementTypeId, int setSize) throws IOException {

    }

    @Override
    public void writeSetEnd() throws IOException {

    }

    @Override
    public void writeBool(boolean b) throws IOException {

    }

    @Override
    public void writeByte(byte b) throws IOException {

    }

    @Override
    public void writeI16(short i16) throws IOException {

    }

    @Override
    public void writeI32(int i32) throws IOException {

    }

    @Override
    public void writeI64(long i64) throws IOException {

    }

    @Override
    public void writeDouble(double dub) throws IOException {

    }

    @Override
    public void writeString(String str) throws IOException {

    }

    @Override
    public void writeBinary(ByteString buf) throws IOException {

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
