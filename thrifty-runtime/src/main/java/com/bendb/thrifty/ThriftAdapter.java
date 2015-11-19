package com.bendb.thrifty;

import com.bendb.thrifty.protocol.TProtocol;

import java.net.ProtocolException;

public abstract class ThriftAdapter<T> {
    abstract T read(TProtocol protocol) throws ProtocolException;
    abstract void write(TProtocol protocol, T struct) throws ProtocolException;
}
