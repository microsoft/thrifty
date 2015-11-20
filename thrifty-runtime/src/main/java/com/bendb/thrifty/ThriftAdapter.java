package com.bendb.thrifty;

import com.bendb.thrifty.protocol.Protocol;

import java.net.ProtocolException;

public abstract class ThriftAdapter<T> {
    abstract T read(Protocol protocol) throws ProtocolException;
    abstract void write(Protocol protocol, T struct) throws ProtocolException;
}
