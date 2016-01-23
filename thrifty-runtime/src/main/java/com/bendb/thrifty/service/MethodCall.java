/*
 * Copyright (C) 2015-2016 Benjamin Bader
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
package com.bendb.thrifty.service;

import com.bendb.thrifty.protocol.MessageMetadata;
import com.bendb.thrifty.protocol.Protocol;

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
