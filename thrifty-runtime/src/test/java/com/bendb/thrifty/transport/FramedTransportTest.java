package com.bendb.thrifty.transport;

import okio.Buffer;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class FramedTransportTest {
    @Test
    public void sinkWritesFrameLength() throws Exception {
        Buffer buffer = new Buffer();
        BufferTransport bufferTranport = new BufferTransport(buffer);
        FramedTransport transport = new FramedTransport(bufferTranport);

        transport.sink().writeUtf8("abcde");
        transport.sink().flush();

        assertThat(buffer.readInt(), is(5));
        assertThat(buffer.readUtf8(), is("abcde"));
    }

    @Test
    public void sourceReadsFrameLength() throws Exception {
        Buffer buffer = new Buffer();
        buffer.writeInt(5);
        buffer.writeUtf8("abcdefghij"); // buffer.size() is now 14

        FramedTransport transport = new FramedTransport(new BufferTransport(buffer));

        assertThat(transport.source().readUtf8(5), is("abcde"));
        assertThat(buffer.size(), is(5L)); // 4 bytes of header plus 5 bytes of frame data were read
    }
}