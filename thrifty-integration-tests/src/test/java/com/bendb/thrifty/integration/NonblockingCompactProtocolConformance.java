package com.bendb.thrifty.integration;

import com.bendb.thrifty.protocol.CompactProtocol;
import com.bendb.thrifty.protocol.Protocol;
import com.bendb.thrifty.testing.ServerProtocol;
import com.bendb.thrifty.testing.ServerTransport;
import com.bendb.thrifty.transport.FramedTransport;
import com.bendb.thrifty.transport.Transport;

public class NonblockingCompactProtocolConformance extends ConformanceBase {
    @Override
    protected ServerTransport getServerTransport() {
        return ServerTransport.NON_BLOCKING;
    }

    @Override
    protected ServerProtocol getServerProtocol() {
        return ServerProtocol.COMPACT;
    }

    @Override
    protected Transport decorate(Transport transport) {
        return new FramedTransport(transport);
    }

    @Override
    protected Protocol createProtocol(Transport transport) {
        return new CompactProtocol(transport);
    }
}
