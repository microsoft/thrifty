/*
 * Thrifty
 *
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED ON AN  *AS IS* BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING
 * WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE,
 * FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
 *
 * See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */
package com.microsoft.thrifty.testing;

import com.microsoft.thrifty.test.gen.ThriftTest;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestServer implements Extension,
        BeforeEachCallback,
        AfterEachCallback {
    private final ServerProtocol protocol;
    private final ServerTransport transport;

    private TServerTransport serverTransport;
    private TServer server;
    private Thread serverThread;

    public void run() {
        ThriftTestHandler handler = new ThriftTestHandler(System.out);
        ThriftTest.Processor<ThriftTestHandler> processor = new ThriftTest.Processor<>(handler);

        TProtocolFactory factory = getProtocolFactory();

        serverTransport = getServerTransport();
        server = startServer(processor, factory);

        final CountDownLatch latch = new CountDownLatch(1);
        serverThread = new Thread(() -> {
            latch.countDown();
            server.serve();
        });

        serverThread.start();

        try {
            latch.await(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
            // continue
        }
    }

    public TestServer() {
        this(ServerProtocol.BINARY, ServerTransport.BLOCKING);
    }

    public TestServer(ServerProtocol protocol, ServerTransport transport) {
        this.protocol = protocol;
        this.transport = transport;
    }

    public int port() {
        if (serverTransport instanceof TServerSocket) {
            return ((TServerSocket) serverTransport).getServerSocket().getLocalPort();
        } else if (serverTransport instanceof TNonblockingServerSocket) {
            TNonblockingServerSocket sock = (TNonblockingServerSocket) serverTransport;
            return sock.getPort();
        } else {
            throw new AssertionError("Unexpected server transport type: " + serverTransport.getClass());
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        run();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        cleanupServer();
    }

    public void close() {
        cleanupServer();
    }

    private void cleanupServer() {
        if (serverTransport != null) {
            serverTransport.close();
            serverTransport = null;
        }

        if (server != null) {
            server.stop();
            server = null;
        }

        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }
    }

    private TServerTransport getServerTransport() {
        switch (transport) {
            case BLOCKING: return getBlockingServerTransport();
            case NON_BLOCKING: return getNonBlockingServerTransport();
            default:
                throw new AssertionError("Invalid transport type: " + transport);
        }
    }

    private TServerTransport getBlockingServerTransport() {
        try {
            InetAddress localhost = InetAddress.getByName("localhost");
            InetSocketAddress socketAddress = new InetSocketAddress(localhost, 0);
            TServerSocket.ServerSocketTransportArgs args = new TServerSocket.ServerSocketTransportArgs()
                    .bindAddr(socketAddress);

            return new TServerSocket(args);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private TServerTransport getNonBlockingServerTransport() {
        try {
            InetAddress localhost = InetAddress.getByName("localhost");
            InetSocketAddress socketAddress = new InetSocketAddress(localhost, 0);

            return new TNonblockingServerSocket(socketAddress);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private TProtocolFactory getProtocolFactory() {
        switch (protocol) {
            case BINARY: return new TBinaryProtocol.Factory();
            case COMPACT: return new TCompactProtocol.Factory();
            case JSON: return new TJSONProtocol.Factory();
            default:
                throw new AssertionError("Invalid protocol value: " + protocol);
        }
    }

    private TServer startServer(TProcessor processor, TProtocolFactory protocolFactory) {
        switch (transport) {
            case BLOCKING: return startBlockingServer(processor, protocolFactory);
            case NON_BLOCKING: return startNonblockingServer(processor, protocolFactory);
            default:
                throw new AssertionError("Invalid transport type: " + transport);
        }
    }

    private TServer startBlockingServer(TProcessor processor, TProtocolFactory protocolFactory) {
        TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverTransport)
                .processor(processor)
                .protocolFactory(protocolFactory);

        return new TThreadPoolServer(args);
    }

    private TServer startNonblockingServer(TProcessor processor, TProtocolFactory protocolFactory) {
        TNonblockingServerTransport nonblockingTransport = (TNonblockingServerTransport) serverTransport;
        TNonblockingServer.Args args = new TNonblockingServer.Args(nonblockingTransport)
                .processor(processor)
                .protocolFactory(protocolFactory);

        return new TNonblockingServer(args);
    }
}
