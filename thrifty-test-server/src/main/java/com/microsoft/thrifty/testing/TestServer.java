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

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TestServer implements Extension,
        BeforeAllCallback,
        AfterAllCallback {
    private TestServerInterface serverImplementation;

    private ServerProtocol protocol;
    private ServerTransport transport;


    private Class<?> testClass;


    public ServerProtocol getProtocol() {
        return protocol;
    }

    public ServerTransport getTransport() {
        return transport;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        testClass = context.getRequiredTestClass();

        ServerConfig config = testClass.getDeclaredAnnotation(ServerConfig.class);
        protocol = config != null ? config.protocol() : ServerProtocol.BINARY;
        transport = config != null ? config.transport() : ServerTransport.BLOCKING;

        serverImplementation = getServerImplementation(transport);
        serverImplementation.run(protocol, transport);
    }

    private TestServerInterface getServerImplementation(ServerTransport transport) {
        switch (transport) {
            case BLOCKING:
            case NON_BLOCKING:
                return new SocketBasedServer();
            default:
                throw new AssertionError("Invalid transport type: " + transport);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        serverImplementation.close();
    }

    public int port() {
        return serverImplementation.port();
    }

    public void close() {
        serverImplementation.close();
    }

    public static TProtocolFactory getProtocolFactory(ServerProtocol protocol) {
        switch (protocol) {
            case BINARY: return new TBinaryProtocol.Factory();
            case COMPACT: return new TCompactProtocol.Factory();
            case JSON: return new TJSONProtocol.Factory();
            default:
                throw new AssertionError("Invalid protocol value: " + protocol);
        }
    }


}
