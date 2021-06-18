package com.microsoft.thrifty.transport

class TTransportException : Throwable {
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
}
