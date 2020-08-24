package com.microsoft.thrifty.protocol

import com.microsoft.thrifty.transport.Transport
import java.io.IOException

abstract class BaseProtocol(
        @JvmField
        protected val transport: Transport
) : Protocol {
    override fun close() {
        transport.close()
    }

    @Throws(IOException::class)
    override fun flush() {
        transport.flush()
    }
}