package com.bendb.thrifty;

import com.bendb.thrifty.protocol.TProtocol;

public abstract class ThriftAdapter<T> {
    abstract T read(TProtocol protocol);
    abstract void write(TProtocol protocol, T struct);
}
