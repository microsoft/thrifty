package com.microsoft.thrifty.integration.conformance

import com.microsoft.thrifty.ThriftException
import com.microsoft.thrifty.integration.kgen.Insanity
import com.microsoft.thrifty.integration.kgen.Numberz
import com.microsoft.thrifty.integration.kgen.ThriftTestClient
import com.microsoft.thrifty.integration.kgen.Xception
import com.microsoft.thrifty.integration.kgen.Xception2
import com.microsoft.thrifty.integration.kgen.Xtruct
import com.microsoft.thrifty.integration.kgen.Xtruct2
import com.microsoft.thrifty.protocol.BinaryProtocol
import com.microsoft.thrifty.protocol.CompactProtocol
import com.microsoft.thrifty.protocol.JsonProtocol
import com.microsoft.thrifty.protocol.Protocol
import com.microsoft.thrifty.service.AsyncClientBase
import com.microsoft.thrifty.testing.ServerProtocol
import com.microsoft.thrifty.testing.ServerTransport
import com.microsoft.thrifty.testing.TestServer
import com.microsoft.thrifty.transport.FramedTransport
import com.microsoft.thrifty.transport.SocketTransport
import com.microsoft.thrifty.transport.Transport
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import okio.ByteString
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class KotlinBinaryProtocol : KotlinConformanceBase() {
    override val serverTransport: ServerTransport = ServerTransport.BLOCKING
    override val serverProtocol: ServerProtocol = ServerProtocol.BINARY
    override fun createProtocol(transport: Transport) = BinaryProtocol(transport)
}

class KotlinNonBlockingBinaryProtocol : KotlinConformanceBase() {
    override val serverTransport: ServerTransport = ServerTransport.NON_BLOCKING
    override val serverProtocol: ServerProtocol = ServerProtocol.BINARY
    override fun createProtocol(transport: Transport) = BinaryProtocol(transport)
}

class KotlinCompactProtocol : KotlinConformanceBase() {
    override val serverTransport: ServerTransport = ServerTransport.BLOCKING
    override val serverProtocol: ServerProtocol = ServerProtocol.COMPACT
    override fun createProtocol(transport: Transport) = CompactProtocol(transport)
}

class KotlinNonBlockingCompactProtocol : KotlinConformanceBase() {
    override val serverTransport: ServerTransport = ServerTransport.NON_BLOCKING
    override val serverProtocol: ServerProtocol = ServerProtocol.COMPACT
    override fun createProtocol(transport: Transport) = CompactProtocol(transport)
}

class KotlinJsonProtocol : KotlinConformanceBase() {
    override val serverTransport: ServerTransport = ServerTransport.BLOCKING
    override val serverProtocol: ServerProtocol = ServerProtocol.JSON
    override fun createProtocol(transport: Transport) = JsonProtocol(transport)
}

class KotlinNonBlockingJsonProtocol : KotlinConformanceBase() {
    override val serverTransport: ServerTransport = ServerTransport.NON_BLOCKING
    override val serverProtocol: ServerProtocol = ServerProtocol.JSON
    override fun createProtocol(transport: Transport) = JsonProtocol(transport)
}

/**
 * A test of auto-generated service code for the standard ThriftTest
 * service.
 *
 *
 * Conformance is checked by roundtripping requests to a local server that
 * is run on the official Apache Thrift Java codebase.  The test server has
 * an implementation of ThriftTest methods with semantics as described in the
 * .thrift file itself and in the Apache Thrift git repo, along with Java code
 * generated by their compiler.
 */
abstract class KotlinConformanceBase {
    /**
     * An Apache Thrift server that is started anew for each test.
     *
     * The server's transport and protocols are configured based
     * on values returned by the abstract methods
     * [.getServerProtocol] and [.getServerTransport].
     */
    lateinit var testServer: TestServer

    lateinit var transport: Transport
    lateinit var protocol: Protocol
    lateinit var client: ThriftTestClient

    /**
     * Specifies the kind of transport (blocking or non-blocking) for the
     * test server.
     */
    protected abstract val serverTransport: ServerTransport

    /**
     * Specifies which Thrift protocol the test server will use.
     */
    protected abstract val serverProtocol: ServerProtocol

    @Before
    fun setup() {
        testServer = TestServer(serverProtocol, serverTransport)
        testServer.run()

        val port = testServer.port()
        val transport = SocketTransport.Builder("localhost", port)
                .readTimeout(2000)
                .build()

        transport.connect()

        this.transport = decorateTransport(transport)
        this.protocol = createProtocol(this.transport)
        this.client = ThriftTestClient(protocol, object : AsyncClientBase.Listener {
            override fun onTransportClosed() {

            }

            override fun onError(error: Throwable) {
                throw AssertionError(error)
            }
        })
    }

    /**
     * When overridden in a derived class, wraps the given transport
     * in a decorator, e.g. a framed transport.
     */
    private fun decorateTransport(transport: Transport): Transport {
        return when (serverTransport) {
            ServerTransport.NON_BLOCKING -> FramedTransport(transport)
            else -> transport
        }
    }

    protected abstract fun createProtocol(transport: Transport): Protocol

    @After

    fun teardown() {
        client.close()
        protocol.close()
        transport.close()
        testServer.close()
    }

    @Test fun testVoid() {
        val callback = AssertingCallback<Unit>()
        client.testVoid(callback)

        callback.result shouldBe Unit
    }

    @Test fun testBool() {
        val callback = AssertingCallback<Boolean>()
        client.testBool(true, callback)

        callback.result shouldBe true
    }

    @Test fun testByte() {
        val callback = AssertingCallback<Byte>()
        client.testByte(200.toByte(), callback)

        callback.result shouldBe 200.toByte()
    }

    @Test fun testI32() {
        val callback = AssertingCallback<Int>()
        client.testI32(404, callback)

        callback.result shouldBe 404
    }

    @Test fun testI64() {
        val callback = AssertingCallback<Long>()
        client.testI64(Long.MAX_VALUE, callback)

        callback.result shouldBe Long.MAX_VALUE
    }

    @Test fun testDouble() {
        val callback = AssertingCallback<Double>()
        client.testDouble(Math.PI, callback)

        callback.result shouldBe Math.PI
    }

    @Test fun testBinary() {
        val binary = ByteString.encodeUtf8("Peace on Earth and Thrift for all mankind")

        val callback = AssertingCallback<ByteString>()
        client.testBinary(binary, callback)

        callback.result shouldBe binary
    }

    @Test fun testStruct() {
        val xtruct = Xtruct.Builder()
                .byte_thing(1.toByte())
                .i32_thing(2)
                .i64_thing(3L)
                .string_thing("foo")
                .build()

        val callback = AssertingCallback<Xtruct>()
        client.testStruct(xtruct, callback)

        callback.result shouldBe xtruct
    }

    @Test fun testNest() {
        val xtruct = Xtruct.Builder()
                .byte_thing(1.toByte())
                .i32_thing(2)
                .i64_thing(3L)
                .string_thing("foo")
                .build()

        val nest = Xtruct2.Builder()
                .byte_thing(4.toByte())
                .i32_thing(5)
                .struct_thing(xtruct)
                .build()

        val callback = AssertingCallback<Xtruct2>()

        client.testNest(nest, callback)

        callback.result shouldBe nest
    }

    @Test fun testMap() {
        val argument = mapOf(1 to 2, 3 to 4, 7 to 8)

        val callback = AssertingCallback<Map<Int, Int>>()
        client.testMap(argument, callback)

        callback.result shouldBe argument
    }

    @Test fun testStringMap() {
        val argument = mapOf(
                "foo" to "bar",
                "baz" to "quux",
                "one" to "more"
        )

        val callback = AssertingCallback<Map<String, String>>()
        client.testStringMap(argument, callback)

        callback.result shouldBe argument
    }

    @Test fun testSet() {
        val set = setOf(1, 2, 3, 4, 5)

        val callback = AssertingCallback<Set<Int>>()
        client.testSet(set, callback)

        callback.result shouldBe set
    }

    @Test fun testList() {
        val list = listOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1)

        val callback = AssertingCallback<List<Int>>()
        client.testList(list, callback)

        callback.result shouldBe list
    }

    @Test fun testEnum() {
        val argument = Numberz.EIGHT

        val callback = AssertingCallback<Numberz>()
        client.testEnum(argument, callback)

        callback.result shouldBe argument
    }

    @Test fun testTypedef() {
        val callback = AssertingCallback<Long>()
        client.testTypedef(Long.MIN_VALUE, callback)

        callback.result shouldBe Long.MIN_VALUE
    }

    @Test fun testMapMap() {
        val callback = AssertingCallback<Map<Int, Map<Int, Int>>>()
        client.testMapMap(Integer.MAX_VALUE, callback)

        callback.result shouldBe mapOf(
                -4 to mapOf(
                        -4 to -4,
                        -3 to -3,
                        -2 to -2,
                        -1 to -1
                ),

                4 to mapOf(
                        1 to 1,
                        2 to 2,
                        3 to 3,
                        4 to 4
                )
        )
    }

    @Test fun testInsanity() {
        val empty = Insanity.Builder().build()
        val argument = Insanity.Builder()
                .userMap(mapOf(Numberz.ONE to 10L, Numberz.TWO to 20L, Numberz.THREE to 40L))
                .xtructs(listOf(
                        Xtruct.Builder()
                                .byte_thing(18.toByte())
                                .i32_thing(37)
                                .i64_thing(101L)
                                .string_thing("what")
                                .build()
                ))
                .build()

        val expected = mapOf(
                1L to mapOf(Numberz.TWO to argument, Numberz.THREE to argument),
                2L to mapOf(Numberz.SIX to empty)
        )

        val callback = AssertingCallback<Map<Long, Map<Numberz, Insanity>>>()
        client.testInsanity(argument, callback)

        callback.result shouldBe expected
    }

    @Test fun testMulti() {
        val expected = Xtruct.Builder()
                .string_thing("Hello2")
                .byte_thing(9.toByte())
                .i32_thing(11)
                .i64_thing(13L)
                .build()

        val callback = AssertingCallback<Xtruct>()
        client.testMulti(9.toByte(), 11, 13L, mapOf(10.toShort() to "Hello"), Numberz.THREE, 5L, callback)

        callback.result shouldBe expected
    }

    @Test fun testExceptionNormalError() {
        val callback = AssertingCallback<Unit>()
        client.testException("Xception", callback)

        val error = callback.error
        error.shouldBeInstanceOf<Xception>()

        val (errorCode, message) = error as Xception
        errorCode shouldBe 1001
        message shouldBe "Xception"
    }

    @Test fun testExceptionInternalError() {
        val callback = AssertingCallback<Unit>()
        client.testException("TException", callback)

        val error = callback.error
        error.shouldBeInstanceOf<ThriftException>()

        val e = error as ThriftException
        e.kind shouldBe ThriftException.Kind.INTERNAL_ERROR
    }

    @Test fun testMultiExceptionNoError() {
        val callback = AssertingCallback<Xtruct>()
        client.testMultiException("Normal", "Hi there", callback)

        val (string_thing) = callback.result

        // Note: We aren't asserting against an expected value because the members
        //       of the result are unspecified besides 'string_thing', and Thrift
        //       implementations differ on whether to return unset primitive values,
        //       depending on options set during codegen.
        string_thing shouldBe "Hi there"
    }

    @Test fun testMultiExceptionErrorOne() {
        val callback = AssertingCallback<Xtruct>()
        client.testMultiException("Xception", "nope", callback)

        val expected = Xception.Builder()
                .errorCode(1001)
                .message_("This is an Xception")
                .build()

        callback.error shouldBe expected
    }

    @Test fun testMultiExceptionErrorTwo() {
        val callback = AssertingCallback<Xtruct>()
        client!!.testMultiException("Xception2", "nope", callback)

        val error = callback.error as Xception2

        // Note: We aren't asserting against an expected value because the members
        //       of 'struct_thing' are unspecified besides 'string_thing', and Thrift
        //       implementations differ on whether to return unset primitive values,
        //       depending on options set during codegen.
        error.errorCode shouldBe 2002
        error.struct_thing?.string_thing shouldBe "This is an Xception2"
    }

}
