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
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.EOFException
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.ProtocolException

/**
 * An implementation of the Thrift compact binary protocol.
 *
 *
 * Instances of this class are *not* threadsafe.
 */
class CompactProtocol(transport: Transport) : BaseProtocol(transport) {

    // Boolean fields get special treatment - their value is encoded
    // directly in the field header.  As such, when a boolean field
    // header is written, we cache it here until we get the value from
    // the subsequent `writeBool` call.
    private var booleanFieldId = -1

    // Similarly, we cache the value read from a field header until
    // the `readBool` call.
    private var booleanFieldType: Byte = -1
    private val buffer = ByteArray(16)

    // Keep track of the most-recently-written fields,
    // used for delta-encoding.
    private val writingFields = ShortStack()
    private var lastWritingField: Short = 0
    private val readingFields = ShortStack()
    private var lastReadingField: Short = 0

    @Throws(IOException::class)
    override fun writeMessageBegin(name: String, typeId: Byte, seqId: Int) {
        writeByte(PROTOCOL_ID)
        writeByte(((VERSION.toInt() and VERSION_MASK.toInt()) or ((typeId.toInt() shl TYPE_SHIFT_AMOUNT) and TYPE_MASK.toInt())).toByte())
        writeVarint32(seqId)
        writeString(name)
    }

    @Throws(IOException::class)
    override fun writeMessageEnd() {
        // no wire representation
    }

    @Throws(IOException::class)
    override fun writeStructBegin(structName: String) {
        writingFields.push(lastWritingField)
        lastWritingField = 0
    }

    @Throws(IOException::class)
    override fun writeStructEnd() {
        lastWritingField = writingFields.pop()
    }

    @Throws(IOException::class)
    override fun writeFieldBegin(fieldName: String, fieldId: Int, typeId: Byte) {
        if (typeId == TType.BOOL) {
            if (booleanFieldId != -1) {
                throw ProtocolException("Nested invocation of writeFieldBegin")
            }
            booleanFieldId = fieldId
        } else {
            writeFieldBegin(fieldId, CompactTypes.ttypeToCompact(typeId))
        }
    }

    @Throws(IOException::class)
    private fun writeFieldBegin(fieldId: Int, compactTypeId: Byte) {
        // Can we delta-encode the field ID?
        if (fieldId > lastWritingField && fieldId - lastWritingField <= 15) {
            writeByte((fieldId - lastWritingField shl 4 or compactTypeId.toInt()).toByte())
        } else {
            writeByte(compactTypeId)
            writeI16(fieldId.toShort())
        }
        lastWritingField = fieldId.toShort()
    }

    @Throws(IOException::class)
    override fun writeFieldEnd() {
        // no wire representation
    }

    @Throws(IOException::class)
    override fun writeFieldStop() {
        writeByte(TType.STOP)
    }

    @Throws(IOException::class)
    override fun writeMapBegin(keyTypeId: Byte, valueTypeId: Byte, mapSize: Int) {
        if (mapSize == 0) {
            writeByte(0.toByte())
        } else {
            val compactKeyType = CompactTypes.ttypeToCompact(keyTypeId)
            val compactValueType = CompactTypes.ttypeToCompact(valueTypeId)
            writeVarint32(mapSize)
            writeByte(((compactKeyType.toInt() shl 4) or compactValueType.toInt()).toByte())
        }
    }

    @Throws(IOException::class)
    override fun writeMapEnd() {
        // no wire representation
    }

    @Throws(IOException::class)
    override fun writeListBegin(elementTypeId: Byte, listSize: Int) {
        writeVectorBegin(elementTypeId, listSize)
    }

    @Throws(IOException::class)
    override fun writeListEnd() {
        // no wire representation
    }

    @Throws(IOException::class)
    override fun writeSetBegin(elementTypeId: Byte, setSize: Int) {
        writeVectorBegin(elementTypeId, setSize)
    }

    @Throws(IOException::class)
    override fun writeSetEnd() {
        // no wire representation
    }

    @Throws(IOException::class)
    override fun writeBool(b: Boolean) {
        val compactValue = if (b) CompactTypes.BOOLEAN_TRUE else CompactTypes.BOOLEAN_FALSE
        if (booleanFieldId != -1) {
            // We are writing a boolean field, and need to write the
            // deferred field header.  In this case we encode the value
            // directly in the header's type field.
            writeFieldBegin(booleanFieldId, compactValue)
            booleanFieldId = -1
        } else {
            // We are not writing a field - just write the value directly.
            writeByte(compactValue)
        }
    }

    @Throws(IOException::class)
    override fun writeByte(b: Byte) {
        buffer[0] = b
        transport.write(buffer, 0, 1)
    }

    @Throws(IOException::class)
    override fun writeI16(i16: Short) {
        writeVarint32(intToZigZag(i16.toInt()))
    }

    @Throws(IOException::class)
    override fun writeI32(i32: Int) {
        writeVarint32(intToZigZag(i32))
    }

    @Throws(IOException::class)
    override fun writeI64(i64: Long) {
        writeVarint64(longToZigZag(i64))
    }

    @Throws(IOException::class)
    override fun writeDouble(dub: Double) {
        val bits = java.lang.Double.doubleToLongBits(dub)

        // Doubles get written out in little-endian order
        buffer[0] = (bits and 0xFFL).toByte()
        buffer[1] = ((bits ushr  8) and 0xFFL).toByte()
        buffer[2] = ((bits ushr 16) and 0xFFL).toByte()
        buffer[3] = ((bits ushr 24) and 0xFFL).toByte()
        buffer[4] = ((bits ushr 32) and 0xFFL).toByte()
        buffer[5] = ((bits ushr 40) and 0xFFL).toByte()
        buffer[6] = ((bits ushr 48) and 0xFFL).toByte()
        buffer[7] = ((bits ushr 56) and 0xFFL).toByte()
        transport.write(buffer, 0, 8)
    }

    @Throws(IOException::class)
    override fun writeString(str: String) {
        try {
            val bytes = str.toByteArray(charset("UTF-8"))
            writeVarint32(bytes.size)
            transport.write(bytes)
        } catch (e: UnsupportedEncodingException) {
            throw AssertionError(e)
        }
    }

    @Throws(IOException::class)
    override fun writeBinary(buf: ByteString) {
        writeVarint32(buf.size)
        transport.write(buf.toByteArray())
    }

    @Throws(IOException::class)
    private fun writeVectorBegin(typeId: Byte, size: Int) {
        val compactId = CompactTypes.ttypeToCompact(typeId)
        if (size <= 14) {
            writeByte(((size shl 4) or compactId.toInt()).toByte())
        } else {
            writeByte((0xF0 or compactId.toInt()).toByte())
            writeVarint32(size)
        }
    }

    @Throws(IOException::class)
    private fun writeVarint32(n: Int) {
        var n = n
        for (i in buffer.indices) {
            if (n and 0x7F.inv() == 0x00) {
                buffer[i] = n.toByte()
                transport.write(buffer, 0, i + 1)
                return
            } else {
                buffer[i] = ((n and 0x7F) or 0x80).toByte()
                n = n ushr 7
            }
        }
        throw IllegalArgumentException("Cannot represent $n as a varint in 16 bytes or less")
    }

    @Throws(IOException::class)
    private fun writeVarint64(n: Long) {
        var n = n
        for (i in buffer.indices) {
            if (n and 0x7FL.inv() == 0x00L) {
                buffer[i] = n.toByte()
                transport.write(buffer, 0, i + 1)
                return
            } else {
                buffer[i] = ((n and 0x7F) or 0x80).toByte()
                n = n ushr 7
            }
        }
        throw IllegalArgumentException("Cannot represent $n as a varint in 16 bytes or less")
    }

    @Throws(IOException::class)
    override fun readMessageBegin(): MessageMetadata {
        val protocolId = readByte()
        if (protocolId != PROTOCOL_ID) {
            throw ProtocolException(
                    "Expected protocol ID " + Integer.toHexString(PROTOCOL_ID.toInt())
                            + " but got " + Integer.toHexString(protocolId.toInt()))
        }
        val versionAndType = readByte()
        val version = (VERSION_MASK.toInt() and versionAndType.toInt()).toByte()
        if (version != VERSION) {
            throw ProtocolException(
                    "Version mismatch; expected version " + VERSION
                            + " but got " + version)
        }
        val typeId = ((versionAndType.toInt() shr TYPE_SHIFT_AMOUNT) and TYPE_BITS.toInt()).toByte()
        val seqId = readVarint32()
        val name = readString()
        return MessageMetadata(name, typeId, seqId)
    }

    @Throws(IOException::class)
    override fun readMessageEnd() {
    }

    @Throws(IOException::class)
    override fun readStructBegin(): StructMetadata {
        readingFields.push(lastReadingField)
        lastReadingField = 0
        return NO_STRUCT
    }

    @Throws(IOException::class)
    override fun readStructEnd() {
        lastReadingField = readingFields.pop()
    }

    @Throws(IOException::class)
    override fun readFieldBegin(): FieldMetadata {
        val compactId = readByte()
        val typeId = CompactTypes.compactToTtype((compactId.toInt() and 0x0F).toByte())
        if (compactId == TType.STOP) {
            return END_FIELDS
        }
        val fieldId: Short
        val modifier = ((compactId.toInt() and 0xF0) shr 4).toShort()
        fieldId = if (modifier.toInt() == 0) {
            // This is not a field-ID delta - read the entire ID.
            readI16()
        } else {
            (lastReadingField + modifier).toShort()
        }
        if (typeId == TType.BOOL) {
            // the bool value is encoded in the lower nibble of the ID
            booleanFieldType = (compactId.toInt() and 0x0F).toByte()
        }
        lastReadingField = fieldId
        return FieldMetadata("", typeId, fieldId)
    }

    @Throws(IOException::class)
    override fun readFieldEnd() {
    }

    @Throws(IOException::class)
    override fun readMapBegin(): MapMetadata {
        val size = readVarint32()
        val keyAndValueTypes = if (size == 0) 0 else readByte()
        val keyType = CompactTypes.compactToTtype(((keyAndValueTypes.toInt() shr 4) and 0x0F).toByte())
        val valueType = CompactTypes.compactToTtype((keyAndValueTypes.toInt() and 0x0F).toByte())
        return MapMetadata(keyType, valueType, size)
    }

    @Throws(IOException::class)
    override fun readMapEnd() {
        // Nothing on the wire
    }

    @Throws(IOException::class)
    override fun readListBegin(): ListMetadata {
        val sizeAndType = readByte()
        var size: Int = (sizeAndType.toInt() shr 4) and 0x0F
        if (size == 0x0F) {
            size = readVarint32()
        }
        val compactType = (sizeAndType.toInt() and 0x0F).toByte()
        val ttype = CompactTypes.compactToTtype(compactType)
        return ListMetadata(ttype, size)
    }

    @Throws(IOException::class)
    override fun readListEnd() {
        // Nothing on the wire
    }

    @Throws(IOException::class)
    override fun readSetBegin(): SetMetadata {
        val sizeAndType = readByte()
        var size: Int = (sizeAndType.toInt() shr 4) and 0x0F
        if (size == 0x0F) {
            size = readVarint32()
        }
        val compactType = (sizeAndType.toInt() and 0x0F).toByte()
        val ttype = CompactTypes.compactToTtype(compactType)
        return SetMetadata(ttype, size)
    }

    @Throws(IOException::class)
    override fun readSetEnd() {
        // Nothing on the wire
    }

    @Throws(IOException::class)
    override fun readBool(): Boolean {
        val compactId: Byte
        if (booleanFieldType.toInt() != -1) {
            compactId = booleanFieldType
            booleanFieldType = -1
        } else {
            compactId = readByte()
        }
        return compactId == CompactTypes.BOOLEAN_TRUE
    }

    @Throws(IOException::class)
    override fun readByte(): Byte {
        readFully(buffer, 1)
        return buffer[0]
    }

    @Throws(IOException::class)
    override fun readI16(): Short {
        return zigZagToInt(readVarint32()).toShort()
    }

    @Throws(IOException::class)
    override fun readI32(): Int {
        return zigZagToInt(readVarint32())
    }

    @Throws(IOException::class)
    override fun readI64(): Long {
        return zigZagToLong(readVarint64())
    }

    @Throws(IOException::class)
    override fun readDouble(): Double {
        readFully(buffer, 8)
        val bits: Long = ((buffer[0].toLong() and 0xFFL)
                or ((buffer[1].toLong() and 0xFFL) shl 8)
                or ((buffer[2].toLong() and 0xFFL) shl 16)
                or ((buffer[3].toLong() and 0xFFL) shl 24)
                or ((buffer[4].toLong() and 0xFFL) shl 32)
                or ((buffer[5].toLong() and 0xFFL) shl 40)
                or ((buffer[6].toLong() and 0xFFL) shl 48)
                or ((buffer[7].toLong() and 0xFFL) shl 56))
        return java.lang.Double.longBitsToDouble(bits)
    }

    @Throws(IOException::class)
    override fun readString(): String {
        val length = readVarint32()
        if (length == 0) {
            return ""
        }
        val bytes = ByteArray(length)
        readFully(bytes, length)
        return String(bytes, Charsets.UTF_8)
    }

    @Throws(IOException::class)
    override fun readBinary(): ByteString {
        val length = readVarint32()
        if (length == 0) {
            return ByteString.EMPTY
        }
        val bytes = ByteArray(length)
        readFully(bytes, length)
        return bytes.toByteString()
    }

    @Throws(IOException::class)
    private fun readVarint32(): Int {
        var result = 0
        var shift = 0
        while (true) {
            val b = readByte()
            result = result or ((b.toInt() and 0x7F) shl shift)
            if (b.toInt() and 0x80 != 0x80) {
                return result
            }
            shift += 7
        }
    }

    @Throws(IOException::class)
    private fun readVarint64(): Long {
        var result: Long = 0
        var shift = 0
        while (true) {
            val b = readByte()
            result = result or ((b.toInt() and 0x7F).toLong() shl shift)
            if (b.toInt() and 0x80 != 0x80) {
                return result
            }
            shift += 7
        }
    }

    @Throws(IOException::class)
    private fun readFully(buffer: ByteArray, count: Int) {
        var toRead = count
        var offset = 0
        while (toRead > 0) {
            val read = transport.read(buffer, offset, toRead)
            if (read == -1) {
                throw EOFException()
            }
            toRead -= read
            offset += read
        }
    }

    private class CompactTypes private constructor() {
        companion object {
            const val BOOLEAN_TRUE: Byte = 0x01
            const val BOOLEAN_FALSE: Byte = 0x02
            const val BYTE: Byte = 0x03
            const val I16: Byte = 0x04
            const val I32: Byte = 0x05
            const val I64: Byte = 0x06
            const val DOUBLE: Byte = 0x07
            const val BINARY: Byte = 0x08
            const val LIST: Byte = 0x09
            const val SET: Byte = 0x0A
            const val MAP: Byte = 0x0B
            const val STRUCT: Byte = 0x0C
            fun ttypeToCompact(typeId: Byte): Byte {
                return when (typeId) {
                    TType.STOP -> TType.STOP
                    TType.VOID -> throw IllegalArgumentException("Unexpected VOID type")
                    TType.BOOL -> BOOLEAN_TRUE
                    TType.BYTE -> BYTE
                    TType.DOUBLE -> DOUBLE
                    TType.I16 -> I16
                    TType.I32 -> I32
                    TType.I64 -> I64
                    TType.STRING -> BINARY
                    TType.STRUCT -> STRUCT
                    TType.MAP -> MAP
                    TType.SET -> SET
                    TType.LIST -> LIST
                    else -> throw IllegalArgumentException(
                            "Unknown TType ID: $typeId")
                }
            }

            fun compactToTtype(compactId: Byte): Byte {
                return when (compactId) {
                    TType.STOP -> TType.STOP
                    BOOLEAN_TRUE -> TType.BOOL
                    BOOLEAN_FALSE -> TType.BOOL
                    BYTE -> TType.BYTE
                    I16 -> TType.I16
                    I32 -> TType.I32
                    I64 -> TType.I64
                    DOUBLE -> TType.DOUBLE
                    BINARY -> TType.STRING
                    LIST -> TType.LIST
                    SET -> TType.SET
                    MAP -> TType.MAP
                    STRUCT -> TType.STRUCT
                    else -> throw IllegalArgumentException(
                            "Unknown compact type ID: $compactId")
                }
            }
        }

        init {
            throw AssertionError("no instances")
        }
    }

    private class ShortStack internal constructor() {
        private var stack: ShortArray
        private var top: Int
        fun push(value: Short) {
            if (top + 1 == stack.size) {
                val biggerStack = ShortArray(stack.size shl 1)
                System.arraycopy(stack, 0, biggerStack, 0, stack.size)
                stack = biggerStack
            }
            stack[++top] = value
        }

        fun pop(): Short {
            return stack[top--]
        }

        init {
            stack = ShortArray(16)
            top = -1
        }
    }

    companion object {
        // Constants, as defined in TCompactProtocol.java
        private const val PROTOCOL_ID = 0x82.toByte()
        private const val VERSION: Byte = 1
        private const val VERSION_MASK: Byte = 0x1F
        private const val TYPE_MASK = 0xE0.toByte()
        private const val TYPE_BITS: Byte = 0x07
        private const val TYPE_SHIFT_AMOUNT = 5
        private val NO_STRUCT = StructMetadata("")
        private val END_FIELDS = FieldMetadata("", TType.STOP, 0.toShort())

        /**
         * Convert a twos-complement int to zigzag encoding,
         * allowing negative values to be written as varints.
         */
        private fun intToZigZag(n: Int): Int {
            return n shl 1 xor (n shr 31)
        }

        /**
         * Convert a twos-complement long to zigzag encoding,
         * allowing negative values to be written as varints.
         */
        private fun longToZigZag(n: Long): Long {
            return n shl 1 xor (n shr 63)
        }

        private fun zigZagToInt(n: Int): Int {
            return n ushr 1 xor -(n and 1)
        }

        private fun zigZagToLong(n: Long): Long {
            return n ushr 1 xor -(n and 1)
        }
    }
}