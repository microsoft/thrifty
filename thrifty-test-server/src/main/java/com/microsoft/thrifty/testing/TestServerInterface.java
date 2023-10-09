package com.microsoft.thrifty.testing;

import com.microsoft.thrifty.test.gen.ThriftTest;

public interface TestServerInterface {
    void run(ServerProtocol protocol, ServerTransport transport);

    int port();

    void close();
}
