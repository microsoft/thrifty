package com.bendb.thrifty.protocol;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;

import java.io.IOException;

public class TDebugProtocol extends TProtocol {
    protected TDebugProtocol(BufferedSource source, BufferedSink sink) {
        super(source, sink);
    }

    @Override
    public void writeMessageBegin(String name, byte typeId, int seqId) throws IOException {
        writeText("MESSAGE BEGIN: name=%s tid=%d sid=%s", name, typeId, seqId);
    }

    @Override
    public void writeMessageEnd() throws IOException {
        writeText("MESSAGE END");
    }

    @Override
    public void writeStructBegin(String structName) throws IOException {
        writeText("STRUCT BEGIN: name=%s", structName);
    }

    @Override
    public void writeStructEnd() throws IOException {
        writeText("STRUCT END");
    }

    @Override
    public void writeFieldBegin(String fieldName, int fieldId, byte typeId) throws IOException {
        writeText("FIELD BEGIN: name=%s fid=%d tid=%d", fieldName, fieldId, typeId);
    }

    @Override
    public void writeFieldEnd() throws IOException {
        writeText("FIELD END");
    }

    @Override
    public void writeFieldStop() throws IOException {
        writeText("FIELD STOP");
    }

    @Override
    public void writeMapBegin(byte keyTypeId, byte valueTypeId, int mapSize) throws IOException {
        writeText("MAP BEGIN: ktid=%d vtid=%d size=%d", keyTypeId, valueTypeId, mapSize);
    }

    @Override
    public void writeMapEnd() throws IOException {
        writeText("MAP END");
    }

    @Override
    public void writeListBegin(byte elementTypeId, int listSize) throws IOException {
        writeText("LIST BEGIN: tid=%d size=%d", elementTypeId, listSize);
    }

    @Override
    public void writeListEnd() throws IOException {
        writeText("LIST END");
    }

    @Override
    public void writeSetBegin(byte elementTypeId, int setSize) throws IOException {
        writeText("SET BEGIN: tid=%d size=%d", elementTypeId, setSize);
    }

    @Override
    public void writeSetEnd() throws IOException {
        writeText("SET END");
    }

    @Override
    public void writeBool(boolean b) throws IOException {
        writeText("<bool> %b", b);
    }

    @Override
    public void writeByte(byte b) throws IOException {
        writeText("<byte> %d", b);
    }

    @Override
    public void writeI16(short i16) throws IOException {
        writeText("<i16> %d", i16);
    }

    @Override
    public void writeI32(int i32) throws IOException {
        writeText("<i32> %d", i32);
    }

    @Override
    public void writeI64(long i64) throws IOException {
        writeText("<i64> %d", i64);
    }

    @Override
    public void writeDouble(double dub) throws IOException {
        writeText("<double> %d", dub);
    }

    @Override
    public void writeString(String str) throws IOException {
        writeText("<string> %s", str);
    }

    @Override
    public void writeBinary(ByteString buf) throws IOException {
        writeText("<binary> %s", buf.hex());
    }

    private void writeText(String format, Object... args) throws IOException {
        String text = String.format(format, args);
        sink.writeUtf8(text);
    }
}
