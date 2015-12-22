package com.bendb.thrifty.service;

import com.bendb.thrifty.protocol.Protocol;

import java.io.IOException;

/**
 * A Sender is an object that can write service-method arguments to a {@link Protocol}.
 *
 * <p>An implementation is generated for each method.
 */
public interface Sender {
    void send(Protocol protocol) throws IOException;
}
