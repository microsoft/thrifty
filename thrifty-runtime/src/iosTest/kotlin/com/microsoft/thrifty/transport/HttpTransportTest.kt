package com.microsoft.thrifty.transport

import com.microsoft.thrifty.runtime.kgen.coro.Insanity
import com.microsoft.thrifty.runtime.kgen.coro.Numberz
import com.microsoft.thrifty.runtime.kgen.coro.ThriftTestClient
import com.microsoft.thrifty.runtime.kgen.coro.UserId
import com.microsoft.thrifty.protocol.BinaryProtocol
import com.microsoft.thrifty.service.AsyncClientBase
import com.microsoft.thrifty.service.ServiceMethodCallback
import io.kotest.matchers.maps.beEmpty
import io.kotest.matchers.shouldNot
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSProcessInfo
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.fail

class HttpTransportTest {
    private var port: Int = -1

    @BeforeTest
    fun setUp() {
        val portVar = NSProcessInfo.processInfo.environment["THRIFTY_HTTP_SERVER_PORT"]
        requireNotNull(portVar)
        port = (portVar as String).toInt()
    }

    @Test
    fun testHttpTransport() = runBlocking {
        val transport = HttpTransport("http://localhost:$port/test/service")
        val protocol = BinaryProtocol(transport)
        val client = ThriftTestClient(protocol, object : AsyncClientBase.Listener {
            override fun onTransportClosed() {
                println("transport closed")
            }

            override fun onError(error: Throwable) {
                fail("error: $error")
            }
        })

        val insanity = Insanity.Builder()
            .build()

        val result = client.testInsanity(insanity)

        result shouldNot beEmpty()

        transport.close()
    }
}
