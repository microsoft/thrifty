/*
 * Copyright (C) 2015-2016 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.thrifty.transport;

import java.io.IOException;
import java.net.ProtocolException;

import okio.Buffer;

/**
 * A transport decorator that reads from and writes to the underlying transport
 * in length-prefixed frames.  Used when the server is using a non-blocking
 * implementation, which currently requires such framing.
 */
public class FramedTransport extends Transport {
    private final Transport inner;

    // Read state
    private int remainingBytes = -1;

    // Write state
    private Buffer pendingWrite;

    public FramedTransport(Transport inner) {
        this.inner = inner;
    }

    @Override
    public void close() throws IOException {
        inner.close();
    }

    @Override
    public int read(byte[] buffer, int offset, int count) throws IOException {
        while (remainingBytes <= 0) {
            readHeader();
        }

        int toRead = Math.min(count, remainingBytes);
        return inner.read(buffer, offset, toRead);
    }

    private void readHeader() throws IOException {
        byte[] headerBytes = new byte[4];
        inner.read(headerBytes, 0, headerBytes.length);

        remainingBytes = ((headerBytes[0] & 0xFF) << 24)
                       | ((headerBytes[1] & 0xFF) << 16)
                       | ((headerBytes[2] & 0xFF) <<  8)
                       | ((headerBytes[3] & 0xFF));
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        if (pendingWrite == null) {
            pendingWrite = new Buffer();
        }

        pendingWrite.write(buffer, offset, count);
    }

    @Override
    public void flush() throws IOException {
        if (pendingWrite == null) {
            return;
        }

        long size = pendingWrite.size();
        if (size > Integer.MAX_VALUE) {
            throw new ProtocolException("Cannot write more than Integer.MAX_VALUE bytes in a single frame");
        }

        byte[] headerBytes = new byte[4];
        headerBytes[0] = (byte) ((size >> 24) & 0xFF);
        headerBytes[1] = (byte) ((size >> 16) & 0xFF);
        headerBytes[2] = (byte) ((size >>  8) & 0xFF);
        headerBytes[3] = (byte)  (size        & 0xFF);

        inner.write(headerBytes);
        inner.write(pendingWrite.readByteArray());
    }
}
