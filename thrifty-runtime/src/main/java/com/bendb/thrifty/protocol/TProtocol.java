package com.bendb.thrifty.protocol;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;

import java.io.IOException;

public abstract class TProtocol {
    protected final BufferedSource source;
    protected final BufferedSink sink;

    protected TProtocol(BufferedSource source, BufferedSink sink) {
        if (source == null) throw new NullPointerException("source");
        if (sink == null) throw new NullPointerException("sink");
        this.source = source;
        this.sink = sink;
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
}
