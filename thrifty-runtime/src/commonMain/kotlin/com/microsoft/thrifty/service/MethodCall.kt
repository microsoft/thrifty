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
package com.microsoft.thrifty.service

import com.microsoft.thrifty.protocol.MessageMetadata
import com.microsoft.thrifty.protocol.Protocol
import okio.IOException
import kotlin.jvm.JvmField

/**
 * A closure capturing all data necessary to send and receive an asynchronous
 * service method call.
 */
abstract class MethodCall<T>(
        @JvmField val name: String,
        @JvmField val callTypeId: Byte,
        @JvmField val callback: ServiceMethodCallback<T>?,
) {

    @Throws(IOException::class)
    abstract fun send(protocol: Protocol)

    @Throws(Exception::class)
    abstract fun receive(protocol: Protocol, metadata: MessageMetadata): T

    init {
        require(callTypeId == TMessageType.CALL || callTypeId == TMessageType.ONEWAY) {
            "Unexpected call type: $callTypeId"
        }
        require(callback != null || callTypeId == TMessageType.ONEWAY) {
            "callback is required"
        }
    }
}
