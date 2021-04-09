package com.microsoft.thrifty.service.server

import com.microsoft.thrifty.protocol.MessageMetadata
import com.microsoft.thrifty.protocol.Protocol
import com.microsoft.thrifty.service.TMessageType

suspend fun MessageMetadata.reply(
    output: Protocol,
    type: Byte = TMessageType.REPLY,
    block: suspend Protocol.() -> Unit
) {
    output.writeMessageBegin(name, type, seqId)
    block(output)
    output.writeMessageEnd()
}

suspend fun Protocol.readMessage(block: suspend Protocol.(MessageMetadata) -> Unit) {
    val msg = readMessageBegin()
    block(msg)
    readMessageEnd()
}