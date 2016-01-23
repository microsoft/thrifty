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

import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;

import okio.Buffer;
import okio.ForwardingSource;
import okio.Source;

/**
 * A {@link Source} decorator that reads length-prefixed frames of data from an
 * underlying source.  The prefix is a four-byte big-endian integer.
 */
class LengthPrefixingSource extends ForwardingSource {
    private Buffer buffer = new Buffer();

    public LengthPrefixingSource(Source delegate) {
        super(delegate);
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
        if (buffer.size() > 0) {
            long read = buffer.read(sink, byteCount);
            if (read > 0) {
                return read;
            }
        }

        readFrame();

        return buffer.read(sink, byteCount);
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    private void readFrame() throws IOException {
        long toRead = 4;
        while (toRead > 0) {
            long read = delegate().read(buffer, toRead);
            if (read == -1) {
                throw new EOFException();
            }
            toRead -= read;
        }

        toRead = buffer.readInt();
        if (toRead < 0) {
            throw new ProtocolException(
                    "Read negative length - is the other endpoint using framing?");
        }

        while (toRead > 0) {
            long read = delegate().read(buffer, toRead);
            if (read == -1) {
                throw new EOFException();
            }
            toRead -= read;
        }
    }
}
