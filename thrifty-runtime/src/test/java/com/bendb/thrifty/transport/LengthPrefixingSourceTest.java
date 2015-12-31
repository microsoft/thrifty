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

import org.junit.Test;

import java.net.ProtocolException;

import okio.Buffer;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class LengthPrefixingSourceTest {
    @Test
    public void readsWholeFrames() throws Exception {
        Buffer buffer = new Buffer();
        buffer.writeInt(3);
        buffer.writeUtf8("foo");

        LengthPrefixingSource source = new LengthPrefixingSource(buffer);

        source.read(new Buffer(), 3);

        assertThat(buffer.size(), is(0L));
    }

    @Test
    public void returnValueDoesNotIncludePrefixByteCount() throws Exception {
        Buffer buffer = new Buffer();
        buffer.writeInt(3);
        buffer.writeUtf8("bar");

        LengthPrefixingSource source = new LengthPrefixingSource(buffer);
        Buffer sink = new Buffer();

        long read = source.read(sink, 3);

        assertThat(read, is(3L));
        assertThat(sink.readUtf8(), is("bar"));
    }

    @Test
    public void failsOnNegativeLength() throws Exception {
        Buffer buffer = new Buffer();
        buffer.writeInt(-3);

        LengthPrefixingSource source = new LengthPrefixingSource(buffer);
        try {
            source.read(new Buffer(), 1);
            fail();
        } catch (ProtocolException expected) {
            assertThat(expected.getMessage(), containsString("Read negative length"));
        }
    }
}
