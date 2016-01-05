package com.bendb.thrifty.integration;

import com.bendb.thrifty.protocol.BinaryProtocol;
import com.bendb.thrifty.protocol.Protocol;
import com.bendb.thrifty.transport.Transport;

public class BinaryProtocolConformance extends ConformanceBase {
    @Override
    protected Transport decorate(Transport transport) {
        return transport;
    }

    @Override
    protected Protocol createProtocol(Transport transport) {
        return new BinaryProtocol(transport);
    }
}
