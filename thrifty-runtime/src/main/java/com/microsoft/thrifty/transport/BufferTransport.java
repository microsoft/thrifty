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

import okio.Buffer;

import java.io.IOException;

public class BufferTransport extends Transport {
    public final Buffer b;

    public BufferTransport() {
        this(new Buffer());
    }

    public BufferTransport(Buffer buffer) {
        this.b = buffer;
    }

    @Override
    public int read(byte[] buffer, int offset, int count) throws IOException {
        return b.read(buffer, offset, count);
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        b.write(buffer, offset, count);
    }

    @Override
    public void flush() throws IOException {
        b.flush();
    }

    @Override
    public void close() throws IOException {
        b.close();
    }
}
