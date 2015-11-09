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

import com.bendb.thrifty.TException;
import okio.ByteString;
import okio.Sink;
import okio.Source;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public class TSimpleJsonProtocol extends TProtocol {

    private static final byte[] COMMA = new byte[] { ',' };
    private static final byte[] COLON = new byte[] { ':' };

    private final Deque<Context> contextStack = new ArrayDeque<>();

    public TSimpleJsonProtocol(Source source, Sink sink) {
        super(source, sink);
    }

    @Override
    public void writeMessageBegin(TMessageMetadata message) throws TException {

    }

    @Override
    public void writeMessageEnd() throws TException {

    }

    @Override
    public void writeStructBegin(String structName) throws TException {

    }

    @Override
    public void writeStructEnd() throws TException {

    }

    @Override
    public void writeFieldBegin(String fieldName, int fieldId, byte typeId) throws TException {

    }

    @Override
    public void writeFieldEnd() throws TException {

    }

    @Override
    public void writeFieldStop() throws TException {

    }

    @Override
    public void writeMapBegin(byte keyTypeId, byte valueTypeId, int mapSize) throws TException {

    }

    @Override
    public void writeMapEnd() throws TException {

    }

    @Override
    public void writeListBegin(byte elementTypeId, int listSize) throws TException {

    }

    @Override
    public void writeListEnd() throws TException {

    }

    @Override
    public void writeSetBegin(byte elementTypeId, int setSize) throws TException {

    }

    @Override
    public void writeSetEnd() throws TException {

    }

    @Override
    public void writeBool(boolean b) throws TException {

    }

    @Override
    public void writeByte(byte b) throws TException {

    }

    @Override
    public void writeI16(short i16) throws TException {

    }

    @Override
    public void writeI32(int i32) throws TException {

    }

    @Override
    public void writeI64(long i64) throws TException {

    }

    @Override
    public void writeDouble(double dub) throws TException {

    }

    @Override
    public void writeString(String str) throws TException {

    }

    @Override
    public void writeBinary(ByteString buf) throws TException {

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
