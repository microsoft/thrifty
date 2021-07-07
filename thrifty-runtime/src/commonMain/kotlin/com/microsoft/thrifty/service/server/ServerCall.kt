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
package com.microsoft.thrifty.service.server

import com.microsoft.thrifty.Struct
import com.microsoft.thrifty.protocol.MessageMetadata
import com.microsoft.thrifty.protocol.Protocol

interface ServerCall<TArgs, THandler> {
    val oneWay: Boolean

    object Empty : Struct {
        override fun write(protocol: Protocol) {
            protocol.writeStructBegin("void")
            protocol.writeFieldStop()
            protocol.writeStructEnd()
        }
    }

    suspend fun receive(protocol: Protocol): TArgs
    suspend fun getResult(args: TArgs, handler: THandler): Struct

    suspend fun process(
        msg: MessageMetadata,
        input: Protocol,
        output: Protocol,
        errorHandler: ErrorHandler,
        handler: THandler
    ) {
        val args = receive(input)
        try {
            val result = getResult(args, handler)
            if (!oneWay) {
                msg.reply(output) {
                    result.write(this)
                }
            }
        } catch (e: Exception) {
            errorHandler.onError(e, msg, input, output, oneWay)
        }
    }
}

