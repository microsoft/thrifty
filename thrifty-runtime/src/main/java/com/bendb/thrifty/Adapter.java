package com.bendb.thrifty;

import com.bendb.thrifty.protocol.Protocol;

import java.io.IOException;

public interface Adapter<T, B extends StructBuilder<T>> {
    T read(Protocol protocol) throws IOException;
    T read(Protocol protocol, B builder) throws IOException;
    void write(Protocol protocol, T struct) throws IOException;
}
