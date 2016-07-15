package com.microsoft.thrifty;

import com.microsoft.thrifty.protocol.Protocol;

import java.io.IOException;

/**
 * An interface that Thrift struct objects should implement
 */
public interface Struct {

    /**
     * Writes this {@link Struct} instance to the given {@code protocol}
     * @param protocol the protocol to which to write the struct
     * @throws IOException if writing fails
     */
    void write(Protocol protocol) throws IOException;
}
