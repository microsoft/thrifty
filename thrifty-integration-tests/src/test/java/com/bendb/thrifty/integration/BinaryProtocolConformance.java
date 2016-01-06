package com.bendb.thrifty.integration;

import com.bendb.thrifty.protocol.BinaryProtocol;
import com.bendb.thrifty.protocol.Protocol;
import com.bendb.thrifty.testing.ServerProtocol;
import com.bendb.thrifty.testing.ServerTransport;
import com.bendb.thrifty.transport.Transport;

public class BinaryProtocolConformance extends ConformanceBase {
    @Override
    protected ServerTransport getServerTransport() {
        return ServerTransport.BLOCKING;
    }

    @Override
    protected ServerProtocol getServerProtocol() {
        return ServerProtocol.BINARY;
    }

    @Override
    protected Protocol createProtocol(Transport transport) {
        return new BinaryProtocol(transport);
    }
}
