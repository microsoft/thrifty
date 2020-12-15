package com.microsoft.thrifty

import com.microsoft.thrifty.protocol.BinaryProtocol
import com.microsoft.thrifty.protocol.CompactProtocol
import com.microsoft.thrifty.protocol.JsonProtocol
import com.microsoft.thrifty.protocol.SimpleJsonProtocol
import com.microsoft.thrifty.transport.BufferTransport
import com.microsoft.thrifty.transport.Transport
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource

/**
 * Creates a transport backed by the given [Buffer].
 *
 * @receiver the [Buffer] backing the new transport.
 * @return a transport that reads from/writes to the buffer.
 */
fun Buffer.transport() = BufferTransport(this)

/**
 * Creates a read-only transport from the given [BufferedSource].
 *
 * @receiver the source underlying the new transport.
 * @return a read-only transport.
 */
fun <S : BufferedSource> S.transport() = object : Transport {
    private val self = this@transport

    override fun close() = self.close()

    override fun read(buffer: ByteArray, offset: Int, count: Int) = self.read(buffer, offset, count)

    override fun write(data: ByteArray) = error("read-only transport")

    override fun write(buffer: ByteArray, offset: Int, count: Int) = error("read-only transport")

    override fun flush() {
        // No-op
    }
}

/**
 * Creates a write-only transport from the given [BufferedSink]
 *
 * @receiver the sink underlying the new transport.
 * @return a write-only transport.
 */
fun <S : BufferedSink> S.transport() = object : Transport {
    private val self = this@transport

    override fun close() = self.close()

    override fun read(buffer: ByteArray, offset: Int, count: Int) = error("write-only transport")

    override fun write(data: ByteArray) { self.write(data) }

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        self.write(buffer, offset, count)
    }

    override fun flush() = self.flush()
}

/**
 * Creates a [BinaryProtocol] from the given [Transport].
 */
fun <T : Transport> T.binaryProtocol() = BinaryProtocol(this)

/**
 * Creates a [CompactProtocol] from the given [Transport].
 */
fun <T : Transport> T.compactProtocol() = CompactProtocol(this)

/**
 * Creates a [JsonProtocol] from the given [Transport].
 */
fun <T : Transport> T.jsonProtocol() = JsonProtocol(this)

/**
 * Creates a [SimpleJsonProtocol] from the given [Transport].
 */
fun <T : Transport> T.simpleJsonProtocol() = SimpleJsonProtocol(this)
