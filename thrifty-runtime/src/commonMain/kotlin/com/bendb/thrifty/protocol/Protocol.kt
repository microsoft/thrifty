/*
 * Thrifty
 *
 * Copyright (c) Benjamin Bader
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
package com.bendb.thrifty.protocol

import okio.ByteString
import okio.Closeable
import okio.IOException

interface Protocol : Closeable {

    @Throws(IOException::class)
    fun writeMessageBegin(name: String, typeId: Byte, seqId: Int)

    @Throws(IOException::class)
    fun writeMessageEnd()

    @Throws(IOException::class)
    fun writeStructBegin(structName: String)

    @Throws(IOException::class)
    fun writeStructEnd()

    @Throws(IOException::class)
    fun writeFieldBegin(fieldName: String, fieldId: Int, typeId: Byte)

    @Throws(IOException::class)
    fun writeFieldEnd()

    @Throws(IOException::class)
    fun writeFieldStop()

    @Throws(IOException::class)
    fun writeMapBegin(keyTypeId: Byte, valueTypeId: Byte, mapSize: Int)

    @Throws(IOException::class)
    fun writeMapEnd()

    @Throws(IOException::class)
    fun writeListBegin(elementTypeId: Byte, listSize: Int)

    @Throws(IOException::class)
    fun writeListEnd()

    @Throws(IOException::class)
    fun writeSetBegin(elementTypeId: Byte, setSize: Int)

    @Throws(IOException::class)
    fun writeSetEnd()

    @Throws(IOException::class)
    fun writeBool(b: Boolean)

    @Throws(IOException::class)
    fun writeByte(b: Byte)

    @Throws(IOException::class)
    fun writeI16(i16: Short)

    @Throws(IOException::class)
    fun writeI32(i32: Int)

    @Throws(IOException::class)
    fun writeI64(i64: Long)

    @Throws(IOException::class)
    fun writeDouble(dub: Double)

    @Throws(IOException::class)
    fun writeString(str: String)

    @Throws(IOException::class)
    fun writeBinary(buf: ByteString)

    ////////

    @Throws(IOException::class)
    fun readMessageBegin(): MessageMetadata

    @Throws(IOException::class)
    fun readMessageEnd()

    @Throws(IOException::class)
    fun readStructBegin(): StructMetadata

    @Throws(IOException::class)
    fun readStructEnd()

    @Throws(IOException::class)
    fun readFieldBegin(): FieldMetadata

    @Throws(IOException::class)
    fun readFieldEnd()

    @Throws(IOException::class)
    fun readMapBegin(): MapMetadata

    @Throws(IOException::class)
    fun readMapEnd()

    @Throws(IOException::class)
    fun readListBegin(): ListMetadata

    @Throws(IOException::class)
    fun readListEnd()

    @Throws(IOException::class)
    fun readSetBegin(): SetMetadata

    @Throws(IOException::class)
    fun readSetEnd()

    @Throws(IOException::class)
    fun readBool(): Boolean

    @Throws(IOException::class)
    fun readByte(): Byte

    @Throws(IOException::class)
    fun readI16(): Short

    @Throws(IOException::class)
    fun readI32(): Int

    @Throws(IOException::class)
    fun readI64(): Long

    @Throws(IOException::class)
    fun readDouble(): Double

    @Throws(IOException::class)
    fun readString(): String

    @Throws(IOException::class)
    fun readBinary(): ByteString

    //////////////

    @Throws(IOException::class)
    fun flush()

    fun reset() {
        // to be implemented by implementations as needed
    }
}
