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

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

/**
 * A transport decorator that reads from and writes to the underlying transport
 * in length-prefixed frames.  Used when the server is using a non-blocking
 * implementation, which currently requires such framing.
 */
public class FramedTransport extends Transport {
    private final Transport inner;
    private final BufferedSource source;
    private final BufferedSink sink;

    public FramedTransport(Transport inner) {
        this.inner = inner;
        source = Okio.buffer(new LengthPrefixingSource(inner.source()));
        sink = Okio.buffer(new LengthPrefixingSink(inner.sink()));
    }

    @Override
    public BufferedSource source() {
        return source;
    }

    @Override
    public BufferedSink sink() {
        return sink;
    }

    @Override
    public void close() throws IOException {
        inner.close();
        source.close();
        sink.close();
    }
}
