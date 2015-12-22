package com.bendb.thrifty.service;

import com.bendb.thrifty.protocol.Protocol;

/**
 *
 */
public interface Receiver {
    void receive(Protocol protocol);
}
