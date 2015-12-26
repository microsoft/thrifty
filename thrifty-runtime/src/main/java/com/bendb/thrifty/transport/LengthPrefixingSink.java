/*
 * Copyright (C) 2015 Benjamin Bader
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
import okio.ForwardingSink;
import okio.Sink;

/**
 * A {@link Sink} decorator that buffers written data until it is flushed, when
 * the buffered data will be written to the underlying Sink prepended with its
 * length as a big-endian 32-bit integer.
 */
class LengthPrefixingSink extends ForwardingSink {
    private final Buffer buffer = new Buffer();

    public LengthPrefixingSink(Sink delegate) {
        super(delegate);
    }

    @Override
    public void write(Buffer source, long byteCount) throws IOException {
        buffer.write(source, byteCount);
    }

    @Override
    public void flush() throws IOException {
        if (buffer.size() > Integer.MAX_VALUE) {
            // I suppose that this is technically a possibility, but good luck
            // allocating 2GB on an Android device (in 2015, at least)!
            throw new ProtocolException(
                    "Cannot write more than Integer.MAX_VALUE bytes in a single frame");
        }

        Buffer lengthBuffer = new Buffer();
        lengthBuffer.writeInt((int) buffer.size());

        delegate().write(lengthBuffer, lengthBuffer.size());
        delegate().write(buffer, buffer.size());
        delegate().flush();

        buffer.clear();
    }

    @Override
    public void close() throws IOException {
        super.close();
        buffer.close();
    }
}
