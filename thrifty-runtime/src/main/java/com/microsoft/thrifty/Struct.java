package com.microsoft.thrifty;

import com.microsoft.thrifty.protocol.Protocol;

import java.io.IOException;

public interface Struct {
    void write(Protocol protocol) throws IOException;
}
