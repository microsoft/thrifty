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

import org.junit.Test;

import java.net.ProtocolException;

import okio.Buffer;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

public class LengthPrefixingSinkTest {
    @Test
    public void doesNotEncodeNegativeLength() throws Exception {
        assumeThat(
                "Don't run this in Travis-CI; it angers the oomkiller",
                System.getenv("TRAVIS"), not(equalTo("true")));

        byte[] data = new byte[1024 * 1024];
        int iterations = Integer.MAX_VALUE / data.length + 1;

        Buffer buffer = new Buffer();
        LengthPrefixingSink sink = new LengthPrefixingSink(buffer);

        try {
            Buffer b = new Buffer();
            for (int i = 0; i < iterations; ++i) {
                b.write(data);
                sink.write(b, data.length);
            }
        } catch (OutOfMemoryError ignored) {
            // Failed to run - need moar RAM!
            return;
        }

        try {
            sink.flush();
            fail();
        } catch (ProtocolException expected) {
            assertThat(
                    expected.getMessage(),
                    containsString("Cannot write more than Integer.MAX_VALUE"));
        }
    }

    @Test
    public void flushedDataBeginsWithFrameLength() throws Exception {
        Buffer target = new Buffer();
        Buffer source = new Buffer();
        LengthPrefixingSink sink = new LengthPrefixingSink(target);

        source.writeUtf8("this text contains thirty-seven bytes");

        sink.write(source, source.size());
        sink.flush();

        assertThat(target.size(), is(41L));
        assertThat(target.readInt(), is(37));
        assertThat(target.readUtf8(), is("this text contains thirty-seven bytes"));
    }
}
