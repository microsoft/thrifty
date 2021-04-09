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

