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

import com.microsoft.thrifty.protocol.MessageMetadata;
import com.microsoft.thrifty.protocol.Protocol;

import java.io.IOException;

/**
 * A closure capturing all data necessary to send and receive an asynchronous
 * service method call.
 */
public abstract class MethodCall<T> {
    protected final String name;
    protected final byte callTypeId;
    protected final ServiceMethodCallback<T> callback;

    public MethodCall(
            String name,
            byte callTypeId,
            ServiceMethodCallback<T> callback) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (callback == null && callTypeId != TMessageType.ONEWAY) {
            throw new NullPointerException("callback");
        }
        if (callTypeId != TMessageType.CALL && callTypeId != TMessageType.ONEWAY) {
            throw new IllegalArgumentException("Unexpected call type: " + callTypeId);
        }

        this.name = name;
        this.callTypeId = callTypeId;
        this.callback = callback;
    }

    protected abstract void send(Protocol protocol) throws IOException;

    protected abstract T receive(Protocol protocol, MessageMetadata metadata) throws Exception;
}
