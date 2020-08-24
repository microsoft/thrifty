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
package com.microsoft.thrifty.integration

import com.microsoft.thrifty.protocol.FieldMetadata
import com.microsoft.thrifty.protocol.ListMetadata
import com.microsoft.thrifty.protocol.MapMetadata
import com.microsoft.thrifty.protocol.MessageMetadata
import com.microsoft.thrifty.protocol.Protocol
import com.microsoft.thrifty.protocol.SetMetadata
import com.microsoft.thrifty.protocol.StructMetadata
import okio.ByteString

/**
 * A [Protocol] wrapper that prints each method as it is invoked, which is
 * helpful when debugging client-server errors from generated code.
 */
class DebugProtocolWrapper(
        private val protocol: Protocol
) : Protocol by protocol {
    override fun writeMessageBegin(name: String, typeId: Byte, seqId: Int) {
        println("writeMessageBegin($name, $typeId, $seqId")
        protocol.writeMessageBegin(name, typeId, seqId)
    }

    override fun writeMessageEnd() {
        println("writeMessageEnd")
        protocol.writeMessageEnd()
    }

    override fun writeStructBegin(structName: String) {
        println("writeStructBegin($structName)")
        protocol.writeStructBegin(structName)
    }

    override fun writeStructEnd() {
        println("writeStructEnd()")
        protocol.writeStructEnd()
    }

    override fun writeFieldBegin(fieldName: String, fieldId: Int, typeId: Byte) {
        println("writeFieldBegin($fieldName, $fieldId, $typeId)")
        protocol.writeFieldBegin(fieldName, fieldId, typeId)
    }

    override fun writeFieldEnd() {
        println("writeFieldEnd()")
        protocol.writeFieldEnd()
    }

    override fun writeFieldStop() {
        println("writeFieldStop()")
        protocol.writeFieldStop()
    }

    override fun writeMapBegin(keyTypeId: Byte, valueTypeId: Byte, mapSize: Int) {
        println("writeMapBegin($keyTypeId, $valueTypeId, $mapSize)")
        protocol.writeMapBegin(keyTypeId, valueTypeId, mapSize)
    }

    override fun writeMapEnd() {
        println("writeMapEnd()")
        protocol.writeMapEnd()
    }

    override fun writeListBegin(elementTypeId: Byte, listSize: Int) {
        println("writeListBegin($elementTypeId, $listSize)")
        protocol.writeListBegin(elementTypeId, listSize)
    }

    override fun writeListEnd() {
        println("writeListEnd()")
        protocol.writeListEnd()
    }

    override fun writeSetBegin(elementTypeId: Byte, setSize: Int) {
        println("writeSetBegin($elementTypeId, $setSize)")
        protocol.writeSetBegin(elementTypeId, setSize)
    }

    override fun writeSetEnd() {
        println("writeSetEnd()")
        protocol.writeSetEnd()
    }

    override fun writeBool(b: Boolean) {
        println("writeBool($b)")
        protocol.writeBool(b)
    }

    override fun writeByte(b: Byte) {
        println("writeByte($b)")
        protocol.writeByte(b)
    }

    override fun writeI16(i16: Short) {
        println("writeI16($i16)")
        protocol.writeI16(i16)
    }

    override fun writeI32(i32: Int) {
        println("writeI32($i32)")
        protocol.writeI32(i32)
    }

    override fun writeI64(i64: Long) {
        println("writeI64($i64)")
        protocol.writeI64(i64)
    }

    override fun writeDouble(dub: Double) {
        println("writeDouble($dub)")
        protocol.writeDouble(dub)
    }

    override fun writeString(str: String) {
        println("writeString($str)")
        protocol.writeString(str)
    }

    override fun writeBinary(buf: ByteString) {
        println("writeBinary(${buf.hex()})")
        protocol.writeBinary(buf)
    }

    override fun readMessageBegin(): MessageMetadata {
        println("readMessageBegin()")
        return protocol.readMessageBegin()
    }

    override fun readMessageEnd() {
        println("readMessageEnd()")
        protocol.readMessageEnd()
    }

    override fun readStructBegin(): StructMetadata {
        println("readStructBegin()")
        return protocol.readStructBegin()
    }

    override fun readStructEnd() {
        println("readStructEnd()")
        protocol.readStructEnd()
    }

    override fun readFieldBegin(): FieldMetadata {
        println("readFieldBegin()")
        return protocol.readFieldBegin()
    }

    override fun readFieldEnd() {
        println("readFieldEnd()")
        protocol.readFieldEnd()
    }

    override fun readMapBegin(): MapMetadata {
        println("readMapBegin()")
        return protocol.readMapBegin()
    }

    override fun readMapEnd() {
        println("readMapEnd()")
        protocol.readMapEnd()
    }

    override fun readListBegin(): ListMetadata {
        println("readListBegin()")
        return protocol.readListBegin()
    }

    override fun readListEnd() {
        println("readListEnd()")
        protocol.readListEnd()
    }

    override fun readSetBegin(): SetMetadata {
        println("readSetBegin()")
        return protocol.readSetBegin()
    }

    override fun readSetEnd() {
        println("readSetEnd()")
        protocol.readSetEnd()
    }

    override fun readBool(): Boolean {
        println("readBool()")
        return protocol.readBool()
    }

    override fun readByte(): Byte {
        println("readByte()")
        return protocol.readByte()
    }

    override fun readI16(): Short {
        println("readI16()")
        return protocol.readI16()
    }

    override fun readI32(): Int {
        println("readI32()")
        return protocol.readI32()
    }

    override fun readI64(): Long {
        println("readI64()")
        return protocol.readI64()
    }

    override fun readDouble(): Double {
        println("readDouble()")
        return protocol.readDouble()
    }

    override fun readString(): String {
        println("readString()")
        return protocol.readString()
    }

    override fun readBinary(): ByteString {
        println("readBinary()")
        return protocol.readBinary()
    }
}