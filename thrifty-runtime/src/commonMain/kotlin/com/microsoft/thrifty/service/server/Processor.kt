package com.microsoft.thrifty.service.server

import com.microsoft.thrifty.protocol.Protocol

interface Processor {
    suspend fun process(input: Protocol, output: Protocol)
}
