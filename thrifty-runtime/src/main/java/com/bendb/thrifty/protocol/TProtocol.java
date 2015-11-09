package com.bendb.thrifty.protocol;

import com.bendb.thrifty.TException;
import okio.*;

public abstract class TProtocol {
    protected final BufferedSource source;
    protected final BufferedSink sink;

    protected TProtocol(Source source, Sink sink) {
        if (source == null) throw new NullPointerException("source");
        if (sink == null) throw new NullPointerException("sink");
        this.source = Okio.buffer(source);
        this.sink = Okio.buffer(sink);
    }

    public abstract void writeMessageBegin(TMessageMetadata message) throws TException;

    public abstract void writeMessageEnd() throws TException;

    public abstract void writeStructBegin(String structName) throws TException;

    public abstract void writeStructEnd() throws TException;

    public abstract void writeFieldBegin(String fieldName, int fieldId, byte typeId) throws TException;

    public abstract void writeFieldEnd() throws TException;

    public abstract void writeFieldStop() throws TException;

    public abstract void writeMapBegin(byte keyTypeId, byte valueTypeId, int mapSize) throws TException;

    public abstract void writeMapEnd() throws TException;

    public abstract void writeListBegin(byte elementTypeId, int listSize) throws TException;

    public abstract void writeListEnd() throws TException;

    public abstract void writeSetBegin(byte elementTypeId, int setSize) throws TException;

    public abstract void writeSetEnd() throws TException;

    public abstract void writeBool(boolean b) throws TException;

    public abstract void writeByte(byte b) throws TException;

    public abstract void writeI16(short i16) throws TException;

    public abstract void writeI32(int i32) throws TException;

    public abstract void writeI64(long i64) throws TException;

    public abstract void writeDouble(double dub) throws TException;

    public abstract void writeString(String str) throws TException;

    public abstract void writeBinary(ByteString buf) throws TException;
}
