/*
 * Copyright (C) 2015 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.thrifty.testing;

import com.bendb.thrifty.test.gen.ThriftTest;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestServer implements TestRule {
    private final ServerProtocol protocol;

    private TServerSocket serverSocket;
    private TSimpleServer server;
    private Thread serverThread;

    public void run(ServerProtocol protocol) {
        TProtocolFactory factory;
        switch (protocol) {
            case BINARY: factory = new TBinaryProtocol.Factory(); break;
            default:
                throw new AssertionError("Invalid protocol value: " + protocol);
        }

        ThriftTestHandler handler = new ThriftTestHandler(System.out);
        ThriftTest.Processor<ThriftTestHandler> processor = new ThriftTest.Processor<>(handler);
        TServer.Args args = new TServer.Args(serverSocket)
                .protocolFactory(factory)
                .processor(processor);

        server = new TSimpleServer(args);

        final CountDownLatch latch = new CountDownLatch(1);
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
                server.serve();
            }
        });

        serverThread.start();

        try {
            latch.await(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
            // continue
        }
    }

    public TestServer() {
        this(ServerProtocol.BINARY);
    }

    public TestServer(ServerProtocol protocol) {
        this.protocol = protocol;
    }

    public String host() {
        return serverSocket.getServerSocket().getInetAddress().getHostName();
    }

    public int port() {
        return serverSocket.getServerSocket().getLocalPort();
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                initSocket();
                try {
                    run(protocol);
                    base.evaluate();
                } finally {
                    cleanupServer();
                    cleanupSocket();
                }
            }
        };
    }

    private void initSocket() throws Exception {
        InetAddress localhost = InetAddress.getByName("localhost");
        InetSocketAddress socketAddress = new InetSocketAddress(localhost, 0);
        TServerSocket.ServerSocketTransportArgs args = new TServerSocket.ServerSocketTransportArgs()
                .bindAddr(socketAddress);

        serverSocket = new TServerSocket(args);
    }

    private void cleanupSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Throwable ignored) {
            // nothing
        } finally {
            serverSocket = null;
        }
    }

    private void cleanupServer() {
        if (server != null) {
            server.stop();
            server = null;
        }

        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }
    }
}
