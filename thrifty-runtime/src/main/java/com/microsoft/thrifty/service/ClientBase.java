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
package com.microsoft.thrifty.service;

import com.microsoft.thrifty.ThriftException;
import com.microsoft.thrifty.protocol.MessageMetadata;
import com.microsoft.thrifty.protocol.Protocol;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements a basic service client that executes methods synchronously.
 *
 * <p>Unlike the Apache implementation, there is no presumption made here about framed encoding
 * at the transport level.  If your backend requires framing, be sure to
 * configure your {@link Protocol} and {@link com.microsoft.thrifty.transport.Transport}
 * objects appropriately.
 */
public class ClientBase implements Closeable {

    /**
     * A sequence ID generator; contains the most-recently-used
     * sequence ID (or zero, if no calls have been made).
     */
    private final AtomicInteger seqId = new AtomicInteger(0);

    /**
     * A flag indicating whether the client is active and connected.
     */
    final AtomicBoolean running = new AtomicBoolean(true);

    private final Protocol protocol;

    protected ClientBase(Protocol protocol) {
        this.protocol = protocol;
    }

    /**
     * When invoked by a derived instance, sends the given call to the server.
     *
     * @param methodCall the remote method call to be invoked
     * @return the result of the method call
     */
    protected final Object execute(MethodCall<?> methodCall) throws Exception {
        if (!running.get()) {
            throw new IllegalStateException("Cannot write to a closed service client");
        }

        try {
            return invokeRequest(methodCall);
        } catch (ServerException e) {
            throw e.thriftException;
        }
    }

    /**
     * Closes this service client and the underlying protocol.
     *
     * Subclasses that override this method need to set {@link #running} to false and call {@link #closeProtocol()}.
     */
    @Override
    public void close() throws IOException {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        closeProtocol();
    }

    void closeProtocol() {
        try {
            protocol.close();
        } catch (IOException ignored) {
            // nope
        }
    }

    /**
     * Send the given call to the server.
     *
     * @param call the remote method call to be invoked
     * @return the result of the method call
     * @throws ServerException wrapper around {@link ThriftException}. Callers should catch and unwrap this.
     * @throws IOException from the protocol
     * @throws Exception exception received from server implements {@link com.microsoft.thrifty.Struct}
     */
    final Object invokeRequest(MethodCall<?> call) throws Exception {
        boolean isOneWay = call.callTypeId == TMessageType.ONEWAY;
        int sid = seqId.incrementAndGet();

        protocol.writeMessageBegin(call.name, call.callTypeId, sid);
        call.send(protocol);
        protocol.writeMessageEnd();
        protocol.flush();

        if (isOneWay) {
            // No response will be received
            return null;
        }

        MessageMetadata metadata = protocol.readMessageBegin();

        if (metadata.seqId != sid) {
            throw new ThriftException(
                    ThriftException.Kind.BAD_SEQUENCE_ID,
                    "Unrecognized sequence ID");
        }

        if (metadata.type == TMessageType.EXCEPTION) {
            ThriftException e = ThriftException.read(protocol);
            protocol.readMessageEnd();
            throw new ServerException(e);
        } else if (metadata.type != TMessageType.REPLY) {
            throw new ThriftException(
                    ThriftException.Kind.INVALID_MESSAGE_TYPE,
                    "Invalid message type: " + metadata.type);
        }

        if (metadata.seqId != seqId.get()) {
            throw new ThriftException(
                    ThriftException.Kind.BAD_SEQUENCE_ID,
                    "Out-of-order response");
        }

        if (!metadata.name.equals(call.name)) {
            throw new ThriftException(
                    ThriftException.Kind.WRONG_METHOD_NAME,
                    "Unexpected method name in reply; expected " + call.name
                            + " but received " + metadata.name);
        }

        return call.receive(protocol, metadata);
    }

    static class ServerException extends Exception {
        final ThriftException thriftException;

        ServerException(ThriftException thriftException) {
            this.thriftException = thriftException;
        }
    }
}
