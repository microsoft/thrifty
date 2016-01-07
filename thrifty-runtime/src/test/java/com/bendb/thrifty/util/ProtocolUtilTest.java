package com.bendb.thrifty.util;

import com.bendb.thrifty.TType;
import com.bendb.thrifty.protocol.BinaryProtocol;
import com.bendb.thrifty.protocol.Xtruct;
import okio.Buffer;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ProtocolUtilTest {
    @Test
    public void skipConsumesStructs() throws Exception {
        Buffer buffer = new Buffer();
        BinaryProtocol proto = new BinaryProtocol(buffer, buffer);

        Xtruct struct = new Xtruct.Builder()
                .byte_thing((byte) 1)
                .i32_thing(3)
                .i64_thing(5L)
                .string_thing("testing")
                .build();

        Xtruct.ADAPTER.write(proto, struct);

        ProtocolUtil.skip(proto, TType.STRUCT);

        assertThat(buffer.size(), is(0L));
    }
}