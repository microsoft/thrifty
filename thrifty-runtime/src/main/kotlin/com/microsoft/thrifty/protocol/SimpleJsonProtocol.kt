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
package com.microsoft.thrifty.protocol

import com.microsoft.thrifty.transport.Transport
import com.microsoft.thrifty.util.UnsafeByteArrayOutputStream
import okio.ByteString
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.ProtocolException
import java.util.ArrayDeque
import java.util.Deque

/**
 * A protocol that maps Thrift data to idiomatic JSON.
 *
 * "Idiomatic" here means that structs map to JSON maps, with field names
 * for keys.  Field tags are not included, and precise type information is not
 * preserved.  For this reason, SimpleJsonProtocol *does not support round-
 * tripping* - it is write-only.
 *
 * Note that, as of the initial release, this Protocol does not guarantee
 * that all emitted data is strictly valid JSON.  In particular, map keys are
 * not guaranteed to to be strings.
 */
class SimpleJsonProtocol(transport: Transport?) : BaseProtocol(transport!!) {
    /**
     * Indicates how [binary][ByteString] data is serialized when
     * written as JSON.
     */
    enum class BinaryOutputMode {
        /**
         * Write binary data as a hex-encoded string.
         */
        HEX,

        /**
         * Write binary data as a base-64-encoded string.
         */
        BASE_64,

        /**
         * Write binary data using Unicode escape syntax.
         */
        UNICODE
    }

    private open class WriteContext {
        @Throws(IOException::class)
        open fun beforeWrite() {
        }

        @Throws(IOException::class)
        open fun onPop() {
            // Fine
        }
    }

    private inner class ListWriteContext : WriteContext() {
        private var hasWritten = false
        @Throws(IOException::class)
        override fun beforeWrite() {
            if (hasWritten) {
                transport.write(COMMA)
            } else {
                hasWritten = true
            }
        }
    }

    private inner class MapWriteContext : WriteContext() {
        private var hasWritten = false
        private var mode = MODE_KEY
        @Throws(IOException::class)
        override fun beforeWrite() {
            if (hasWritten) {
                if (mode == MODE_KEY) {
                    transport.write(COMMA)
                } else {
                    transport.write(COLON)
                }
            } else {
                hasWritten = true
            }
            mode = !mode
        }

        @Throws(IOException::class)
        override fun onPop() {
            if (mode == MODE_VALUE) {
                throw ProtocolException("Incomplete JSON map, expected a value")
            }
        }
    }

    companion object {
        private const val MODE_KEY = false
        private const val MODE_VALUE = true
        
        private val ESCAPES: Array<CharArray?> = arrayOfNulls(128)
        private val TRUE = byteArrayOf('t'.toByte(), 'r'.toByte(), 'u'.toByte(), 'e'.toByte())
        private val FALSE = byteArrayOf('f'.toByte(), 'a'.toByte(), 'l'.toByte(), 's'.toByte(), 'e'.toByte())
        private val COMMA = byteArrayOf(','.toByte())
        private val COLON: ByteArray = byteArrayOf(':'.toByte())
        private val LBRACKET = byteArrayOf('['.toByte())
        private val RBRACKET = byteArrayOf(']'.toByte())
        private val LBRACE = byteArrayOf('{'.toByte())
        private val RBRACE = byteArrayOf('}'.toByte())

        init {
            for (i in 0..31) {
                // Control chars must be escaped
                ESCAPES[i] = String.format("\\u%04x", i).toCharArray()
            }
            ESCAPES['\\'.toInt()] = charArrayOf('\\', '\\')
            ESCAPES['\"'.toInt()] = charArrayOf('\\', '"')
            ESCAPES['\b'.toInt()] = charArrayOf('\\', 'b')
            ESCAPES['\u000C'.toInt()] = charArrayOf('\\', 'f')
            ESCAPES['\r'.toInt()] = charArrayOf('\\', 'r')
            ESCAPES['\n'.toInt()] = charArrayOf('\\', 'n')
            ESCAPES['\t'.toInt()] = charArrayOf('\\', 't')
        }
    }

    private val defaultWriteContext: WriteContext = object : WriteContext() {
        @Throws(IOException::class)
        override fun beforeWrite() {
            // nothing
        }
    }
    private val writeStack: Deque<WriteContext> = ArrayDeque()
    private var binaryOutputMode = BinaryOutputMode.HEX
    fun withBinaryOutputMode(mode: BinaryOutputMode): SimpleJsonProtocol {
        binaryOutputMode = mode
        return this
    }

    @Throws(IOException::class)
    override fun writeMessageBegin(name: String, typeId: Byte, seqId: Int) {
        writeMapBegin(typeId, typeId, 0) // values are ignored here
        writeString("name")
        writeString(name)
        writeString("value")
    }

    @Throws(IOException::class)
    override fun writeMessageEnd() {
        writeMapEnd()
    }

    @Throws(IOException::class)
    override fun writeStructBegin(structName: String) {
        writeContext()!!.beforeWrite()
        pushWriteContext(MapWriteContext())
        transport.write(LBRACE)
        writeString("__thriftStruct")
        writeString(structName)
    }

    @Throws(IOException::class)
    override fun writeStructEnd() {
        transport.write(RBRACE)
        popWriteContext()
    }

    @Throws(IOException::class)
    override fun writeFieldBegin(fieldName: String, fieldId: Int, typeId: Byte) {
        // TODO: assert that we're in map context
        writeString(fieldName)
    }

    @Throws(IOException::class)
    override fun writeFieldEnd() {
    }

    @Throws(IOException::class)
    override fun writeFieldStop() {
    }

    @Throws(IOException::class)
    override fun writeMapBegin(keyTypeId: Byte, valueTypeId: Byte, mapSize: Int) {
        writeContext()!!.beforeWrite()
        pushWriteContext(MapWriteContext())
        transport.write(LBRACE)
    }

    @Throws(IOException::class)
    override fun writeMapEnd() {
        transport.write(RBRACE)
        popWriteContext()
    }

    @Throws(IOException::class)
    override fun writeListBegin(elementTypeId: Byte, listSize: Int) {
        writeContext()!!.beforeWrite()
        pushWriteContext(ListWriteContext())
        transport.write(LBRACKET)
    }

    @Throws(IOException::class)
    override fun writeListEnd() {
        transport.write(RBRACKET)
        popWriteContext()
    }

    @Throws(IOException::class)
    override fun writeSetBegin(elementTypeId: Byte, setSize: Int) {
        writeContext()!!.beforeWrite()
        pushWriteContext(ListWriteContext())
        transport.write(LBRACKET)
    }

    @Throws(IOException::class)
    override fun writeSetEnd() {
        transport.write(RBRACKET)
        popWriteContext()
    }

    @Throws(IOException::class)
    override fun writeBool(b: Boolean) {
        writeContext()!!.beforeWrite()
        transport.write(if (b) TRUE else FALSE)
    }

    @Throws(IOException::class)
    override fun writeByte(b: Byte) {
        writeContext()!!.beforeWrite()
        val toWrite = b.toString().toByteArray(Charsets.UTF_8)
        transport.write(toWrite)
    }

    @Throws(IOException::class)
    override fun writeI16(i16: Short) {
        writeContext()!!.beforeWrite()
        transport.write(i16.toString().toByteArray(Charsets.UTF_8))
    }

    @Throws(IOException::class)
    override fun writeI32(i32: Int) {
        writeContext()!!.beforeWrite()
        transport.write(i32.toString().toByteArray(Charsets.UTF_8))
    }

    @Throws(IOException::class)
    override fun writeI64(i64: Long) {
        writeContext()!!.beforeWrite()
        transport.write(i64.toString().toByteArray(Charsets.UTF_8))
    }

    @Throws(IOException::class)
    override fun writeDouble(dub: Double) {
        writeContext()!!.beforeWrite()
        transport.write(dub.toString().toByteArray(Charsets.UTF_8))
    }

    @Throws(IOException::class)
    override fun writeString(str: String) {
        writeContext()!!.beforeWrite()
        val len = str.length
        val baos = UnsafeByteArrayOutputStream(len)
        val writer = OutputStreamWriter(baos, Charsets.UTF_8)
        writer.write('"'.toInt())
        for (i in 0 until len) {
            val c = str[i]
            if (c.toInt() < 128) {
                val maybeEscape = ESCAPES[c.toInt()]
                if (maybeEscape != null) {
                    writer.write(maybeEscape)
                } else {
                    writer.write(c.toInt())
                }
            } else {
                writer.write(c.toInt())
            }
        }
        writer.write('"'.toInt())
        writer.flush()
        transport.write(baos.buffer, 0, baos.size())
    }

    @Throws(IOException::class)
    override fun writeBinary(buf: ByteString) {
        val out = when (binaryOutputMode) {
            BinaryOutputMode.HEX -> buf.hex()
            BinaryOutputMode.BASE_64 -> buf.base64()
            BinaryOutputMode.UNICODE -> buf.utf8()
        }
        writeString(out)
    }

    private fun pushWriteContext(context: WriteContext) {
        writeStack.push(context)
    }

    private fun writeContext(): WriteContext? {
        var top = writeStack.peek()
        if (top == null) {
            top = defaultWriteContext
        }
        return top
    }

    @Throws(IOException::class)
    private fun popWriteContext() {
        val context = writeStack.pollFirst()
        if (context == null) {
            throw ProtocolException("stack underflow")
        } else {
            context.onPop()
        }
    }

    @Throws(IOException::class)
    override fun readMessageBegin(): MessageMetadata {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readMessageEnd() {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readStructBegin(): StructMetadata {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readStructEnd() {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readFieldBegin(): FieldMetadata {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readFieldEnd() {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readMapBegin(): MapMetadata {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readMapEnd() {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readListBegin(): ListMetadata {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readListEnd() {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readSetBegin(): SetMetadata {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readSetEnd() {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readBool(): Boolean {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readByte(): Byte {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readI16(): Short {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readI32(): Int {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readI64(): Long {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readDouble(): Double {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readString(): String {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun readBinary(): ByteString {
        throw UnsupportedOperationException()
    }
}
