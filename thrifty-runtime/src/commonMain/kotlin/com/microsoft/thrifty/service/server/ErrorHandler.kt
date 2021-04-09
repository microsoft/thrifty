package com.microsoft.thrifty.service.server

import com.microsoft.thrifty.ThriftException
import com.microsoft.thrifty.protocol.MessageMetadata
import com.microsoft.thrifty.protocol.Protocol
import com.microsoft.thrifty.service.TMessageType

interface ErrorHandler {
    suspend fun onError(e: Throwable, msg: MessageMetadata, input: Protocol, output: Protocol, oneWay: Boolean)
}

object DefaultErrorHandler : ErrorHandler {
    override suspend fun onError(
        e: Throwable,
        msg: MessageMetadata,
        input: Protocol,
        output: Protocol,
        oneWay: Boolean
    ) {
        if (!oneWay) {
            msg.reply(output, TMessageType.EXCEPTION) {
                val err = ThriftException(
                    ThriftException.Kind.INTERNAL_ERROR,
                    "Internal error processing ${msg.name}"
                )
                err.write(output)
            }
        }
    }
}