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
package com.microsoft.thrifty.transport;

import javax.net.SocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketTransport extends Transport {
    private final String host;
    private final int port;
    private final int readTimeout;
    private final int connectTimeout;
    private final SocketFactory socketFactory;

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public static class Builder {
        private final String host;
        private final int port;
        private int readTimeout;
        private int connectTimeout;
        private SocketFactory socketFactory;

        public Builder(String host, int port) {
            if (host == null || host.length() == 0) {
                throw new NullPointerException("host");
            }

            if (port < 0 || port > 0xFFFF) {
                throw new IllegalStateException("Invalid port number: " + port);
            }

            this.host = host;
            this.port = port;
        }

        public Builder readTimeout(int readTimeout) {
            if (readTimeout < 0) {
                throw new IllegalArgumentException("readTimeout cannot be negative");
            }
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            if (connectTimeout < 0) {
                throw new IllegalArgumentException("connectTimeout cannot be negative");
            }
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder socketFactory(SocketFactory socketFactory) {
            if (socketFactory == null) {
                throw new NullPointerException("socketFactory");
            }

            this.socketFactory = socketFactory;
            return this;
        }

        public SocketTransport build() {
            return new SocketTransport(this);
        }
    }

    SocketTransport(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.readTimeout = builder.readTimeout;
        this.connectTimeout = builder.connectTimeout;
        this.socketFactory = builder.socketFactory == null
                ? SocketFactory.getDefault()
                : builder.socketFactory;
    }

    public boolean isConnected() {
        Socket s = socket;
        return s != null && s.isConnected() && !s.isClosed();
    }

    @Override
    public int read(byte[] buffer, int offset, int count) throws IOException {
        return inputStream.read(buffer, offset, count);
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        outputStream.write(buffer, offset, count);
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    public void connect() throws IOException {
        if (socket == null) {
            socket = socketFactory.createSocket();
        }

        socket.setTcpNoDelay(true);
        socket.setSoLinger(false, 0);
        socket.setKeepAlive(true);
        socket.setSoTimeout(readTimeout);

        socket.connect(new InetSocketAddress(host, port), connectTimeout);

        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    @Override
    public void close() {
        Socket socket = this.socket;
        InputStream in = this.inputStream;
        OutputStream out = this.outputStream;

        this.socket = null;

        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) { }
        }

        if (out != null) {
            try {
                out.close();
            } catch (IOException ignored) { }
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) { }
        }
    }
}
