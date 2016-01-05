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
import java.net.SocketAddress;

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

        TServer.Args args = new TServer.Args(serverSocket)
                .protocolFactory(factory)
                .processor(new ThriftTest.Processor<>(new ThriftTestHandler(System.out)));

        server = new TSimpleServer(args);

        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                server.serve();
            }
        });

        serverThread.start();
    }

    public TestServer() {
        this(ServerProtocol.BINARY);
    }

    public TestServer(ServerProtocol protocol) {
        this.protocol = protocol;
    }

    public SocketAddress address() {
        return serverSocket.getServerSocket().getLocalSocketAddress();
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
        InetAddress localhost = InetAddress.getLocalHost();
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
