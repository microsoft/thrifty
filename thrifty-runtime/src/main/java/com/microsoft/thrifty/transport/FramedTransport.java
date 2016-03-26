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
package com.microsoft.thrifty.transport;

import com.microsoft.thrifty.util.UnsafeByteArrayOutputStream;

import java.io.IOException;

/**
 * A transport decorator that reads from and writes to the underlying transport
 * in length-prefixed frames.  Used when the server is using a non-blocking
 * implementation, which currently requires such framing.
 */
public class FramedTransport extends Transport {
    private final Transport inner;

    // Read state
    private int remainingBytes;

    // Write state
    private UnsafeByteArrayOutputStream pendingWrite;

    public FramedTransport(Transport inner) {
        this.inner = inner;
    }

    @Override
    public void close() throws IOException {
        inner.close();
        pendingWrite = null;
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
            pendingWrite = new UnsafeByteArrayOutputStream(Math.max(count, 32));
        }

        pendingWrite.write(buffer, offset, count);
    }

    @Override
    public void flush() throws IOException {
        int size = pendingWrite == null ? 0 : pendingWrite.size();

        byte[] headerBytes = new byte[4];
        headerBytes[0] = (byte) ((size >> 24) & 0xFF);
        headerBytes[1] = (byte) ((size >> 16) & 0xFF);
        headerBytes[2] = (byte) ((size >>  8) & 0xFF);
        headerBytes[3] = (byte)  (size        & 0xFF);

        inner.write(headerBytes);

        if (size > 0) {
            inner.write(pendingWrite.getBuffer(), 0, size);
            pendingWrite.reset();
        }
    }
}
