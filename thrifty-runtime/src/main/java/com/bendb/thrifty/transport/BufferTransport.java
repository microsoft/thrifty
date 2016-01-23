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
