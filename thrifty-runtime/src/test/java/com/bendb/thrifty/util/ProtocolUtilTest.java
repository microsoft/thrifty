package com.bendb.thrifty.util;

import com.bendb.thrifty.TType;
import com.bendb.thrifty.protocol.BinaryProtocol;
import com.bendb.thrifty.protocol.Xtruct;
import okio.Buffer;
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ProtocolUtilTest {
    private Buffer buffer;
    private BinaryProtocol protocol;

    @Before
    public void setup() {
        buffer = new Buffer();
        protocol = new BinaryProtocol(buffer, buffer);
    }

    @Test
    public void skipConsumesLists() throws Exception {
        List<String> strings = Arrays.asList("foo", "bar", "baz", "quux");

        protocol.writeListBegin(TType.STRING, strings.size());
        for (String string : strings) {
            protocol.writeString(string);
        }
        protocol.writeListEnd();

        ProtocolUtil.skip(protocol, TType.LIST);

        assertThat(buffer.size(), is(0L));
    }

    @Test
    public void skipSets() throws Exception{
        Set<ByteString> set = new LinkedHashSet<>();
        set.add(ByteString.encodeUtf8("hello there"));
        set.add(ByteString.encodeUtf8("here is some more test data"));
        set.add(ByteString.encodeUtf8("take it respectfully!"));

        protocol.writeSetBegin(TType.STRING, set.size());
        for (ByteString bytes : set) {
            protocol.writeBinary(bytes);
        }
        protocol.writeSetEnd();

        ProtocolUtil.skip(protocol, TType.SET);

        assertThat(buffer.size(), is(0L));
    }

    @Test
    public void skipConsumesMap() throws Exception {
        Map<Integer, Long> map = new LinkedHashMap<>();
        map.put(1, 10L);
        map.put(2, 20L);
        map.put(4, 30L);
        map.put(8, 40L);

        protocol.writeMapBegin(TType.I32, TType.I64, map.size());
        for (Map.Entry<Integer, Long> entry : map.entrySet()) {
            protocol.writeI32(entry.getKey());
            protocol.writeI64(entry.getValue());
        }
        protocol.writeMapEnd();

        ProtocolUtil.skip(protocol, TType.MAP);

        assertThat(buffer.size(), is(0L));
    }

    @Test
    public void skipConsumesStructs() throws Exception {
        Xtruct struct = new Xtruct.Builder()
                .byte_thing((byte) 1)
                .i32_thing(3)
                .i64_thing(5L)
                .string_thing("testing")
                .build();

        Xtruct.ADAPTER.write(protocol, struct);

        ProtocolUtil.skip(protocol, TType.STRUCT);

        assertThat(buffer.size(), is(0L));
    }

    @Test
    public void skipListOfStructs() throws Exception {
        List<Xtruct> structs = new ArrayList<>();

        structs.add(new Xtruct.Builder()
                .byte_thing((byte) 1)
                .i32_thing(1)
                .i64_thing(1L)
                .string_thing("one")
                .build());

        structs.add(new Xtruct.Builder()
                .byte_thing((byte) 2)
                .i32_thing(2)
                .i64_thing(2L)
                .string_thing("two")
                .build());

        structs.add(new Xtruct.Builder()
                .byte_thing((byte) 3)
                .i32_thing(3)
                .i64_thing(3L)
                .string_thing("three")
                .build());

        protocol.writeListBegin(TType.STRUCT, structs.size());
        for (Xtruct struct : structs) {
            Xtruct.ADAPTER.write(protocol, struct);
        }
        protocol.writeListEnd();

        ProtocolUtil.skip(protocol, TType.LIST);

        assertThat(buffer.size(), is(0L));
    }
}