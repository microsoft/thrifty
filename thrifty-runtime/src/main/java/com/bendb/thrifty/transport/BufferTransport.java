package com.bendb.thrifty.transport;

import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;

import java.io.IOException;

public class BufferTransport extends Transport {
    public final Buffer buffer;

    public BufferTransport() {
        this(new Buffer());
    }

    public BufferTransport(Buffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public BufferedSource source() {
        return buffer;
    }

    @Override
    public BufferedSink sink() {
        return buffer;
    }

    @Override
    public void close() throws IOException {
        buffer.close();
    }
}
