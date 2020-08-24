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
/*
 * This file is derived from the file TCompactProtocol.java, in the Apache
 * Thrift implementation.  The original license header is reproduced
 * below:
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.microsoft.thrifty.protocol

import com.microsoft.thrifty.TType
import com.microsoft.thrifty.transport.Transport
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.ProtocolException
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.Arrays

/**
 * Json protocol implementation for thrift.
 *
 *
 * This is a full-featured protocol supporting write and read.
 */
class JsonProtocol : BaseProtocol {
    // Stack of nested contexts that we may be in
    private val contextStack = ArrayDeque<JsonBaseContext>()

    // Current context that we are in
    private var context = JsonBaseContext()

    // Reader that manages a 1-byte buffer
    private var reader: LookaheadReader = LookaheadReader()

    // Write out the TField names as a string instead of the default integer value
    private var fieldNamesAsString = false

    // Push a new Json context onto the stack.
    private fun pushContext(c: JsonBaseContext) {
        contextStack.push(context)
        context = c
    }

    // Pop the last Json context off the stack
    private fun popContext() {
        context = contextStack.pop()
    }

    // Reset the context stack to its initial state
    private fun resetContext() {
        while (!contextStack.isEmpty()) {
            popContext()
        }
    }

    constructor(transport: Transport?) : super(transport!!) {}
    constructor(transport: Transport?, fieldNamesAsString: Boolean) : super(transport!!) {
        this.fieldNamesAsString = fieldNamesAsString
    }

    override fun reset() {
        contextStack.clear()
        context = JsonBaseContext()
        reader = LookaheadReader()
    }

    // Temporary buffer used by several methods
    private val tmpbuf = ByteArray(4)

    // Read a byte that must match b[0]; otherwise an exception is thrown.
    // Marked protected to avoid synthetic accessor in JsonListContext.read
    // and JsonPairContext.read
    @Throws(IOException::class)
    protected fun readJsonSyntaxChar(b: ByteArray) {
        val ch = reader.read()
        if (ch != b[0]) {
            throw ProtocolException("Unexpected character:" + ch.toChar())
        }
    }

    // Write the bytes in array buf as a Json characters, escaping as needed
    @Throws(IOException::class)
    private fun writeJsonString(b: ByteArray) {
        context.write()
        transport.write(QUOTE)
        val len = b.size
        for (i in 0 until len) {
            if (b[i].toInt() and 0x00FF >= 0x30) {
                if (b[i] == BACKSLASH[0]) {
                    transport.write(BACKSLASH)
                    transport.write(BACKSLASH)
                } else {
                    transport.write(b, i, 1)
                }
            } else {
                tmpbuf[0] = JSON_CHAR_TABLE[b[i].toInt()]
                if (tmpbuf[0] == 1.toByte()) {
                    transport.write(b, i, 1)
                } else if (tmpbuf[0] > 1) {
                    transport.write(BACKSLASH)
                    transport.write(tmpbuf, 0, 1)
                } else {
                    transport.write(ESCSEQ)
                    tmpbuf[0] = hexChar((b[i].toInt() shr 4).toByte())
                    tmpbuf[1] = hexChar(b[i])
                    transport.write(tmpbuf, 0, 2)
                }
            }
        }
        transport.write(QUOTE)
    }

    // Write out number as a Json value. If the context dictates so, it will be
    // wrapped in quotes to output as a Json string.
    @Throws(IOException::class)
    private fun writeJsonInteger(num: Long) {
        context.write()
        val str = java.lang.Long.toString(num)
        val escapeNum = context.escapeNum()
        if (escapeNum) {
            transport.write(QUOTE)
        }
        try {
            val buf = str.toByteArray(charset("UTF-8"))
            transport.write(buf)
        } catch (e: UnsupportedEncodingException) {
            throw AssertionError(e)
        }
        if (escapeNum) {
            transport.write(QUOTE)
        }
    }

    // Write out a double as a Json value. If it is NaN or infinity or if the
    // context dictates escaping, write out as Json string.
    @Throws(IOException::class)
    private fun writeJsonDouble(num: Double) {
        context.write()
        val str = java.lang.Double.toString(num)
        var special = false
        when (str[0]) {
            'N', 'I' -> special = true
            '-' -> if (str[1] == 'I') { // -Infinity
                special = true
            }
            else -> {
            }
        }
        val escapeNum = special || context.escapeNum()
        if (escapeNum) {
            transport.write(QUOTE)
        }
        try {
            val b = str.toByteArray(charset("UTF-8"))
            transport.write(b, 0, b.size)
        } catch (e: UnsupportedEncodingException) {
            throw AssertionError(e)
        }
        if (escapeNum) {
            transport.write(QUOTE)
        }
    }

    @Throws(IOException::class)
    private fun writeJsonObjectStart() {
        context.write()
        transport.write(LBRACE)
        pushContext(JsonPairContext())
    }

    @Throws(IOException::class)
    private fun writeJsonObjectEnd() {
        popContext()
        transport.write(RBRACE)
    }

    @Throws(IOException::class)
    private fun writeJsonArrayStart() {
        context.write()
        transport.write(LBRACKET)
        pushContext(JsonListContext())
    }

    @Throws(IOException::class)
    private fun writeJsonArrayEnd() {
        popContext()
        transport.write(RBRACKET)
    }

    @Throws(IOException::class)
    override fun writeMessageBegin(name: String, typeId: Byte, seqId: Int) {
        resetContext() // THRIFT-3743
        writeJsonArrayStart()
        writeJsonInteger(VERSION)
        try {
            val b = name.toByteArray(charset("UTF-8"))
            writeJsonString(b)
        } catch (e: UnsupportedEncodingException) {
            throw AssertionError(e)
        }
        writeJsonInteger(typeId.toLong())
        writeJsonInteger(seqId.toLong())
    }

    @Throws(IOException::class)
    override fun writeMessageEnd() {
        writeJsonArrayEnd()
    }

    @Throws(IOException::class)
    override fun writeStructBegin(structName: String) {
        writeJsonObjectStart()
    }

    @Throws(IOException::class)
    override fun writeStructEnd() {
        writeJsonObjectEnd()
    }

    @Throws(IOException::class)
    override fun writeFieldBegin(fieldName: String, fieldId: Int, typeId: Byte) {
        if (fieldNamesAsString) {
            writeString(fieldName)
        } else {
            writeJsonInteger(fieldId.toLong())
        }
        writeJsonObjectStart()
        writeJsonString(JsonTypes.ttypeToJson(typeId))
    }

    @Throws(IOException::class)
    override fun writeFieldEnd() {
        writeJsonObjectEnd()
    }

    override fun writeFieldStop() {}
    @Throws(IOException::class)
    override fun writeMapBegin(keyTypeId: Byte, valueTypeId: Byte, mapSize: Int) {
        writeJsonArrayStart()
        writeJsonString(JsonTypes.ttypeToJson(keyTypeId))
        writeJsonString(JsonTypes.ttypeToJson(valueTypeId))
        writeJsonInteger(mapSize.toLong())
        writeJsonObjectStart()
    }

    @Throws(IOException::class)
    override fun writeMapEnd() {
        writeJsonObjectEnd()
        writeJsonArrayEnd()
    }

    @Throws(IOException::class)
    override fun writeListBegin(elementTypeId: Byte, listSize: Int) {
        writeJsonArrayStart()
        writeJsonString(JsonTypes.ttypeToJson(elementTypeId))
        writeJsonInteger(listSize.toLong())
    }

    @Throws(IOException::class)
    override fun writeListEnd() {
        writeJsonArrayEnd()
    }

    @Throws(IOException::class)
    override fun writeSetBegin(elementTypeId: Byte, setSize: Int) {
        writeJsonArrayStart()
        writeJsonString(JsonTypes.ttypeToJson(elementTypeId))
        writeJsonInteger(setSize.toLong())
    }

    @Throws(IOException::class)
    override fun writeSetEnd() {
        writeJsonArrayEnd()
    }

    @Throws(IOException::class)
    override fun writeBool(b: Boolean) {
        writeJsonInteger(if (b) 1.toLong() else 0.toLong())
    }

    @Throws(IOException::class)
    override fun writeByte(b: Byte) {
        writeJsonInteger(b.toLong())
    }

    @Throws(IOException::class)
    override fun writeI16(i16: Short) {
        writeJsonInteger(i16.toLong())
    }

    @Throws(IOException::class)
    override fun writeI32(i32: Int) {
        writeJsonInteger(i32.toLong())
    }

    @Throws(IOException::class)
    override fun writeI64(i64: Long) {
        writeJsonInteger(i64)
    }

    @Throws(IOException::class)
    override fun writeDouble(dub: Double) {
        writeJsonDouble(dub)
    }

    @Throws(IOException::class)
    override fun writeString(str: String) {
        try {
            val b = str.toByteArray(charset("UTF-8"))
            writeJsonString(b)
        } catch (e: UnsupportedEncodingException) {
            throw AssertionError(e)
        }
    }

    @Throws(IOException::class)
    override fun writeBinary(buf: ByteString) {
        writeString(buf.base64())
    }

    /**
     * Reading methods.
     */
    // Read in a Json string, unescaping as appropriate.. Skip reading from the
    // context if skipContext is true.
    @Throws(IOException::class)
    private fun readJsonString(skipContext: Boolean): ByteString {
        val buffer = Buffer()
        val codeunits = ArrayList<Char>()
        if (!skipContext) {
            context.read()
        }
        readJsonSyntaxChar(QUOTE)
        while (true) {
            var ch = reader.read()
            if (ch == QUOTE[0]) {
                break
            }
            if (ch == ESCSEQ[0]) {
                ch = reader.read()
                ch = if (ch == ESCSEQ[1]) {
                    transport.read(tmpbuf, 0, 4)
                    val cu = ((hexVal(tmpbuf[0]).toInt() shl 12)
                            + (hexVal(tmpbuf[1]).toInt() shl 8)
                            + (hexVal(tmpbuf[2]).toInt() shl 4)
                            + hexVal(tmpbuf[3]).toInt()).toShort()
                    try {
                        if (Character.isHighSurrogate(cu.toChar())) {
                            if (codeunits.size > 0) {
                                throw ProtocolException("Expected low surrogate char")
                            }
                            codeunits.add(cu.toChar())
                        } else if (Character.isLowSurrogate(cu.toChar())) {
                            if (codeunits.size == 0) {
                                throw ProtocolException("Expected high surrogate char")
                            }
                            codeunits.add(cu.toChar())
                            buffer.write(String(intArrayOf(codeunits[0].toInt(), codeunits[1].toInt()), 0, 2)
                                    .toByteArray(charset("UTF-8")))
                            codeunits.clear()
                        } else {
                            buffer.write(String(intArrayOf(cu.toInt()), 0, 1).toByteArray(charset("UTF-8")))
                        }
                        continue
                    } catch (e: UnsupportedEncodingException) {
                        throw AssertionError(e)
                    } catch (ex: IOException) {
                        throw ProtocolException("Invalid unicode sequence")
                    }
                } else {
                    val off = ESCAPE_CHARS.indexOf(ch.toChar())
                    if (off == -1) {
                        throw ProtocolException("Expected control char")
                    }
                    ESCAPE_CHAR_VALS[off]
                }
            }
            buffer.write(byteArrayOf(ch))
        }
        return buffer.readByteString()
    }

    // Return true if the given byte could be a valid part of a Json number.
    private fun isJsonNumeric(b: Byte): Boolean {
        when (b.toChar()) {
            '+', '-', '.', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'E', 'e' -> return true
        }
        return false
    }

    // Read in a sequence of characters that are all valid in Json numbers. Does
    // not do a complete regex check to validate that this is actually a number.
    @Throws(IOException::class)
    private fun readJsonNumericChars(): String {
        val strbld = StringBuilder()
        while (true) {
            val ch = reader.peek()
            if (!isJsonNumeric(ch)) {
                break
            }
            strbld.append(reader.read().toChar())
        }
        return strbld.toString()
    }

    // Read in a Json number. If the context dictates, read in enclosing quotes.
    @Throws(IOException::class)
    private fun readJsonInteger(): Long {
        context.read()
        if (context.escapeNum()) {
            readJsonSyntaxChar(QUOTE)
        }
        val str = readJsonNumericChars()
        if (context.escapeNum()) {
            readJsonSyntaxChar(QUOTE)
        }
        return try {
            java.lang.Long.valueOf(str)
        } catch (ex: NumberFormatException) {
            throw ProtocolException("Bad data encountered in numeric data")
        }
    }

    // Read in a Json double value. Throw if the value is not wrapped in quotes
    // when expected or if wrapped in quotes when not expected.
    @Throws(IOException::class)
    private fun readJsonDouble(): Double {
        context.read()
        return if (reader.peek() == QUOTE[0]) {
            val str = readJsonString(true)
            val dub = java.lang.Double.valueOf(str.utf8())
            if (!context.escapeNum() && !java.lang.Double.isNaN(dub)
                    && !java.lang.Double.isInfinite(dub)) {
                // Throw exception -- we should not be in a string in this case
                throw ProtocolException("Numeric data unexpectedly quoted")
            }
            dub
        } else {
            if (context.escapeNum()) {
                // This will throw - we should have had a quote if escapeNum == true
                readJsonSyntaxChar(QUOTE)
            }
            try {
                java.lang.Double.valueOf(readJsonNumericChars())
            } catch (ex: NumberFormatException) {
                throw ProtocolException("Bad data encountered in numeric data")
            }
        }
    }

    // Read in a Json string containing base-64 encoded data and decode it.
    @Throws(IOException::class)
    private fun readJsonBase64(): ByteString {
        val str = readJsonString(false)
        return str.utf8().decodeBase64()!!
    }

    @Throws(IOException::class)
    private fun readJsonObjectStart() {
        context.read()
        readJsonSyntaxChar(LBRACE)
        pushContext(JsonPairContext())
    }

    @Throws(IOException::class)
    private fun readJsonObjectEnd() {
        readJsonSyntaxChar(RBRACE)
        popContext()
    }

    @Throws(IOException::class)
    private fun readJsonArrayStart() {
        context.read()
        readJsonSyntaxChar(LBRACKET)
        pushContext(JsonListContext())
    }

    @Throws(IOException::class)
    private fun readJsonArrayEnd() {
        readJsonSyntaxChar(RBRACKET)
        popContext()
    }

    @Throws(IOException::class)
    override fun readMessageBegin(): MessageMetadata {
        resetContext() // THRIFT-3743
        readJsonArrayStart()
        if (readJsonInteger() != VERSION) {
            throw ProtocolException("Message contained bad version.")
        }
        val name: String
        name = try {
            readJsonString(false).utf8()
        } catch (ex: UnsupportedEncodingException) {
            throw AssertionError(ex)
        }
        val type = readJsonInteger().toByte()
        val seqid = readJsonInteger().toInt()
        return MessageMetadata(name, type, seqid)
    }

    @Throws(IOException::class)
    override fun readMessageEnd() {
        readJsonArrayEnd()
    }

    @Throws(IOException::class)
    override fun readStructBegin(): StructMetadata {
        readJsonObjectStart()
        return StructMetadata("")
    }

    @Throws(IOException::class)
    override fun readStructEnd() {
        readJsonObjectEnd()
    }

    @Throws(IOException::class)
    override fun readFieldBegin(): FieldMetadata {
        val ch = reader.peek()
        val type: Byte
        var id: Short = 0
        if (ch == RBRACE[0]) {
            type = TType.STOP
        } else {
            id = readJsonInteger().toShort()
            readJsonObjectStart()
            type = JsonTypes.jsonToTtype(readJsonString(false).toByteArray())
        }
        return FieldMetadata("", type, id)
    }

    @Throws(IOException::class)
    override fun readFieldEnd() {
        readJsonObjectEnd()
    }

    @Throws(IOException::class)
    override fun readMapBegin(): MapMetadata {
        readJsonArrayStart()
        val keyType = JsonTypes.jsonToTtype(readJsonString(false).toByteArray())
        val valueType = JsonTypes.jsonToTtype(readJsonString(false).toByteArray())
        val size = readJsonInteger().toInt()
        readJsonObjectStart()
        return MapMetadata(keyType, valueType, size)
    }

    @Throws(IOException::class)
    override fun readMapEnd() {
        readJsonObjectEnd()
        readJsonArrayEnd()
    }

    @Throws(IOException::class)
    override fun readListBegin(): ListMetadata {
        readJsonArrayStart()
        val elemType = JsonTypes.jsonToTtype(readJsonString(false).toByteArray())
        val size = readJsonInteger().toInt()
        return ListMetadata(elemType, size)
    }

    @Throws(IOException::class)
    override fun readListEnd() {
        readJsonArrayEnd()
    }

    @Throws(IOException::class)
    override fun readSetBegin(): SetMetadata {
        readJsonArrayStart()
        val elemType = JsonTypes.jsonToTtype(readJsonString(false).toByteArray())
        val size = readJsonInteger().toInt()
        return SetMetadata(elemType, size)
    }

    @Throws(IOException::class)
    override fun readSetEnd() {
        readJsonArrayEnd()
    }

    @Throws(IOException::class)
    override fun readBool(): Boolean {
        return if (readJsonInteger() == 0L) false else true
    }

    @Throws(IOException::class)
    override fun readByte(): Byte {
        return readJsonInteger().toByte()
    }

    @Throws(IOException::class)
    override fun readI16(): Short {
        return readJsonInteger().toShort()
    }

    @Throws(IOException::class)
    override fun readI32(): Int {
        return readJsonInteger().toInt()
    }

    @Throws(IOException::class)
    override fun readI64(): Long {
        return readJsonInteger()
    }

    @Throws(IOException::class)
    override fun readDouble(): Double {
        return readJsonDouble()
    }

    @Throws(IOException::class)
    override fun readString(): String {
        return readJsonString(false).utf8()
    }

    @Throws(IOException::class)
    override fun readBinary(): ByteString {
        return readJsonBase64()
    }

    // Holds up to one byte from the transport
    protected inner class LookaheadReader {
        private var hasData = false
        private val data = ByteArray(1)

        // Return and consume the next byte to be read, either taking it from the
        // data buffer if present or getting it from the transport otherwise.
        @Throws(IOException::class)
        fun read(): Byte {
            if (hasData) {
                hasData = false
            } else {
                transport.read(data, 0, 1)
            }
            return data[0]
        }

        // Return the next byte to be read without consuming, filling the data
        // buffer if it has not been filled already.
        @Throws(IOException::class)
        fun peek(): Byte {
            if (!hasData) {
                transport.read(data, 0, 1)
            }
            hasData = true
            return data[0]
        }
    }

    private class JsonTypes private constructor() {
        companion object {
            val BOOLEAN = byteArrayOf('t'.toByte(), 'f'.toByte())
            val BYTE = byteArrayOf('i'.toByte(), '8'.toByte())
            val I16 = byteArrayOf('i'.toByte(), '1'.toByte(), '6'.toByte())
            val I32 = byteArrayOf('i'.toByte(), '3'.toByte(), '2'.toByte())
            val I64 = byteArrayOf('i'.toByte(), '6'.toByte(), '4'.toByte())
            val DOUBLE = byteArrayOf('d'.toByte(), 'b'.toByte(), 'l'.toByte())
            val STRUCT = byteArrayOf('r'.toByte(), 'e'.toByte(), 'c'.toByte())
            val STRING = byteArrayOf('s'.toByte(), 't'.toByte(), 'r'.toByte())
            val MAP = byteArrayOf('m'.toByte(), 'a'.toByte(), 'p'.toByte())
            val LIST = byteArrayOf('l'.toByte(), 's'.toByte(), 't'.toByte())
            val SET = byteArrayOf('s'.toByte(), 'e'.toByte(), 't'.toByte())
            fun ttypeToJson(typeId: Byte): ByteArray {
                return when (typeId) {
                    TType.STOP -> throw IllegalArgumentException("Unexpected STOP type")
                    TType.VOID -> throw IllegalArgumentException("Unexpected VOID type")
                    TType.BOOL -> BOOLEAN
                    TType.BYTE -> BYTE
                    TType.DOUBLE -> DOUBLE
                    TType.I16 -> I16
                    TType.I32 -> I32
                    TType.I64 -> I64
                    TType.STRING -> STRING
                    TType.STRUCT -> STRUCT
                    TType.MAP -> MAP
                    TType.SET -> SET
                    TType.LIST -> LIST
                    else -> throw IllegalArgumentException(
                            "Unknown TType ID: $typeId")
                }
            }

            fun jsonToTtype(jsonId: ByteArray): Byte {
                var result = TType.STOP
                if (jsonId.size > 1) {
                    when (jsonId[0].toChar()) {
                        'd' -> result = TType.DOUBLE
                        'i' -> when (jsonId[1].toChar()) {
                            '8' -> result = TType.BYTE
                            '1' -> result = TType.I16
                            '3' -> result = TType.I32
                            '6' -> result = TType.I64
                        }
                        'l' -> result = TType.LIST
                        'm' -> result = TType.MAP
                        'r' -> result = TType.STRUCT
                        's' -> result = if (jsonId[1] == 't'.toByte()) {
                            TType.STRING
                        } else {
                            TType.SET
                        }
                        't' -> result = TType.BOOL
                    }
                }
                require(result != TType.STOP) { "Unknown json type ID: " + Arrays.toString(jsonId) }
                return result
            }
        }

        init {
            throw AssertionError("no instances")
        }
    }

    // Base class for tracking Json contexts that may require inserting/reading
    // additional Json syntax characters
    // This base context does nothing.
    private open class JsonBaseContext {
        @Throws(IOException::class)
        open fun write() {
        }

        @Throws(IOException::class)
        open fun read() {
        }

        open fun escapeNum(): Boolean {
            return false
        }
    }

    // Context for Json lists. Will insert/read commas before each item except
    // for the first one
    private inner class JsonListContext : JsonBaseContext() {
        private var first = true
        @Throws(IOException::class)
        override fun write() {
            if (first) {
                first = false
            } else {
                transport.write(COMMA)
            }
        }

        @Throws(IOException::class)
        override fun read() {
            if (first) {
                first = false
            } else {
                readJsonSyntaxChar(COMMA)
            }
        }
    }

    // Context for Json records. Will insert/read colons before the value portion
    // of each record pair, and commas before each key except the first. In
    // addition, will indicate that numbers in the key position need to be
    // escaped in quotes (since Json keys must be strings).
    private inner class JsonPairContext : JsonBaseContext() {
        private var first = true
        private var colon = true
        @Throws(IOException::class)
        override fun write() {
            if (first) {
                first = false
                colon = true
            } else {
                transport.write(if (colon) COLON else COMMA)
                colon = !colon
            }
        }

        @Throws(IOException::class)
        override fun read() {
            if (first) {
                first = false
                colon = true
            } else {
                readJsonSyntaxChar(if (colon) COLON else COMMA)
                colon = !colon
            }
        }

        override fun escapeNum(): Boolean {
            return colon
        }
    }

    companion object {
        private val COMMA = byteArrayOf(','.toByte())
        private val COLON = byteArrayOf(':'.toByte())
        private val LBRACE = byteArrayOf('{'.toByte())
        private val RBRACE = byteArrayOf('}'.toByte())
        private val LBRACKET = byteArrayOf('['.toByte())
        private val RBRACKET = byteArrayOf(']'.toByte())
        private val QUOTE = byteArrayOf('"'.toByte())
        private val BACKSLASH = byteArrayOf('\\'.toByte())
        private val ESCSEQ = byteArrayOf('\\'.toByte(), 'u'.toByte(), '0'.toByte(), '0'.toByte())
        private const val VERSION: Long = 1
        private val JSON_CHAR_TABLE = byteArrayOf( /*  0 1 2 3 4 5 6 7 8 9 A B C D E F */
                0, 0, 0, 0, 0, 0, 0, 0, 'b'.toByte(), 't'.toByte(), 'n'.toByte(), 0, 'f'.toByte(), 'r'.toByte(), 0, 0,  // 0
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // 1
                1, 1, '"'.toByte(), 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
        private const val ESCAPE_CHARS = "\"\\/bfnrt"
        private val ESCAPE_CHAR_VALS = byteArrayOf(
                '"'.toByte(), '\\'.toByte(), '/'.toByte(), '\b'.toByte(), '\u000C'.toByte(), '\n'.toByte(), '\r'.toByte(), '\t'.toByte())

        // Convert a byte containing a hex char ('0'-'9' or 'a'-'f') into its
        // corresponding hex value
        @Throws(IOException::class)
        private fun hexVal(ch: Byte): Byte {
            return if (ch >= '0'.toByte() && ch <= '9'.toByte()) {
                (ch.toChar() - '0').toByte()
            } else if (ch >= 'a'.toByte() && ch <= 'f'.toByte()) {
                (ch.toChar() - 'a' + 10).toByte()
            } else {
                throw ProtocolException("Expected hex character")
            }
        }

        // Convert a byte containing a hex value to its corresponding hex character
        private fun hexChar(value: Byte): Byte {
            val b = (value.toInt() and 0x0F).toByte()
            return if (b < 10) {
                (b.toInt() + '0'.toInt()).toByte()
            } else {
                ((b.toInt() - 10) + 'a'.toInt()).toByte()
            }
        }
    }
}