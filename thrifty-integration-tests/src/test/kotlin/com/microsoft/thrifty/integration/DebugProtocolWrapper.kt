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

import com.microsoft.thrifty.protocol.DecoratingProtocol
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
class DebugProtocolWrapper(protocol: Protocol): DecoratingProtocol(protocol) {
    override fun writeMessageBegin(name: String?, typeId: Byte, seqId: Int) {
        println("writeMessageBegin($name, $typeId, $seqId")
        super.writeMessageBegin(name, typeId, seqId)
    }

    override fun writeMessageEnd() {
        println("writeMessageEnd")
        super.writeMessageEnd()
    }

    override fun writeStructBegin(structName: String?) {
        println("writeStructBegin($structName)")
        super.writeStructBegin(structName)
    }

    override fun writeStructEnd() {
        println("writeStructEnd()")
        super.writeStructEnd()
    }

    override fun writeFieldBegin(fieldName: String?, fieldId: Int, typeId: Byte) {
        println("writeFieldBegin($fieldName, $fieldId, $typeId)")
        super.writeFieldBegin(fieldName, fieldId, typeId)
    }

    override fun writeFieldEnd() {
        println("writeFieldEnd()")
        super.writeFieldEnd()
    }

    override fun writeFieldStop() {
        println("writeFieldStop()")
        super.writeFieldStop()
    }

    override fun writeMapBegin(keyTypeId: Byte, valueTypeId: Byte, mapSize: Int) {
        println("writeMapBegin($keyTypeId, $valueTypeId, $mapSize)")
        super.writeMapBegin(keyTypeId, valueTypeId, mapSize)
    }

    override fun writeMapEnd() {
        println("writeMapEnd()")
        super.writeMapEnd()
    }

    override fun writeListBegin(elementTypeId: Byte, listSize: Int) {
        println("writeListBegin($elementTypeId, $listSize)")
        super.writeListBegin(elementTypeId, listSize)
    }

    override fun writeListEnd() {
        println("writeListEnd()")
        super.writeListEnd()
    }

    override fun writeSetBegin(elementTypeId: Byte, setSize: Int) {
        println("writeSetBegin($elementTypeId, $setSize)")
        super.writeSetBegin(elementTypeId, setSize)
    }

    override fun writeSetEnd() {
        println("writeSetEnd()")
        super.writeSetEnd()
    }

    override fun writeBool(b: Boolean) {
        println("writeBool($b)")
        super.writeBool(b)
    }

    override fun writeByte(b: Byte) {
        println("writeByte($b)")
        super.writeByte(b)
    }

    override fun writeI16(i16: Short) {
        println("writeI16($i16)")
        super.writeI16(i16)
    }

    override fun writeI32(i32: Int) {
        println("writeI32($i32)")
        super.writeI32(i32)
    }

    override fun writeI64(i64: Long) {
        println("writeI64($i64)")
        super.writeI64(i64)
    }

    override fun writeDouble(dub: Double) {
        println("writeDouble($dub)")
        super.writeDouble(dub)
    }

    override fun writeString(str: String?) {
        println("writeString($str)")
        super.writeString(str)
    }

    override fun writeBinary(buf: ByteString?) {
        println("writeBinary(${buf?.hex()})")
        super.writeBinary(buf)
    }

    override fun readMessageBegin(): MessageMetadata {
        println("readMessageBegin()")
        return super.readMessageBegin()
    }

    override fun readMessageEnd() {
        println("readMessageEnd()")
        super.readMessageEnd()
    }

    override fun readStructBegin(): StructMetadata {
        println("readStructBegin()")
        return super.readStructBegin()
    }

    override fun readStructEnd() {
        println("readStructEnd()")
        super.readStructEnd()
    }

    override fun readFieldBegin(): FieldMetadata {
        println("readFieldBegin()")
        return super.readFieldBegin()
    }

    override fun readFieldEnd() {
        println("readFieldEnd()")
        super.readFieldEnd()
    }

    override fun readMapBegin(): MapMetadata {
        println("readMapBegin()")
        return super.readMapBegin()
    }

    override fun readMapEnd() {
        println("readMapEnd()")
        super.readMapEnd()
    }

    override fun readListBegin(): ListMetadata {
        println("readListBegin()")
        return super.readListBegin()
    }

    override fun readListEnd() {
        println("readListEnd()")
        super.readListEnd()
    }

    override fun readSetBegin(): SetMetadata {
        println("readSetBegin()")
        return super.readSetBegin()
    }

    override fun readSetEnd() {
        println("readSetEnd()")
        super.readSetEnd()
    }

    override fun readBool(): Boolean {
        println("readBool()")
        return super.readBool()
    }

    override fun readByte(): Byte {
        println("readByte()")
        return super.readByte()
    }

    override fun readI16(): Short {
        println("readI16()")
        return super.readI16()
    }

    override fun readI32(): Int {
        println("readI32()")
        return super.readI32()
    }

    override fun readI64(): Long {
        println("readI64()")
        return super.readI64()
    }

    override fun readDouble(): Double {
        println("readDouble()")
        return super.readDouble()
    }

    override fun readString(): String {
        println("readString()")
        return super.readString()
    }

    override fun readBinary(): ByteString {
        println("readBinary()")
        return super.readBinary()
    }
}