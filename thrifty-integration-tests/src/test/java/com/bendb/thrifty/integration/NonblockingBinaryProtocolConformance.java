package com.bendb.thrifty.integration;

import com.bendb.thrifty.protocol.BinaryProtocol;
import com.bendb.thrifty.protocol.Protocol;
import com.bendb.thrifty.testing.ServerProtocol;
import com.bendb.thrifty.testing.ServerTransport;
import com.bendb.thrifty.transport.FramedTransport;
import com.bendb.thrifty.transport.Transport;

public class NonblockingBinaryProtocolConformance extends ConformanceBase {
    @Override
    protected ServerTransport getServerTransport() {
        return ServerTransport.NON_BLOCKING;
    }

    @Override
    protected ServerProtocol getServerProtocol() {
        return ServerProtocol.BINARY;
    }

    @Override
    protected Transport decorate(Transport transport) {
        // non-blocking servers require framing
        return new FramedTransport(transport);
    }

    @Override
    protected Protocol createProtocol(Transport transport) {
        return new BinaryProtocol(transport);
    }
}
