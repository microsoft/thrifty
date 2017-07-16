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

/**
 * A closure capturing all data necessary to send and receive an asynchronous
 * service method call.
 */
public abstract class AsyncMethodCall<T> extends MethodCall<T> {
    protected final ServiceMethodCallback<T> callback;

    public AsyncMethodCall(
            String name,
            byte callTypeId,
            ServiceMethodCallback<T> callback) {
        super(name, callTypeId);

        if (callback == null && callTypeId != TMessageType.ONEWAY) {
            throw new NullPointerException("callback");
        }

        this.callback = callback;
    }
}
