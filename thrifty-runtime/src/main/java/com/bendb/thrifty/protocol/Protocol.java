package com.bendb.thrifty.protocol;

import com.bendb.thrifty.transport.Transport;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;

import java.io.Closeable;
import java.io.IOException;

public abstract class Protocol implements Closeable {
    private final Transport transport;
    protected final BufferedSource source;
    protected final BufferedSink sink;

    protected Protocol(Transport transport) {
        this.transport = transport;
        this.source = transport.source();
        this.sink = transport.sink();
    }

    protected Protocol(BufferedSource source, BufferedSink sink) {
        if (source == null) throw new NullPointerException("source");
        if (sink == null) throw new NullPointerException("sink");
        this.transport = null;
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
        sink.flush();
    }

    public void reset() {
        // to be implemented by children as needed
    }

    @Override
    public void close() throws IOException {
        if (this.transport != null) {
            this.transport.close();
        }

        this.source.close();
        this.sink.close();
    }
}
