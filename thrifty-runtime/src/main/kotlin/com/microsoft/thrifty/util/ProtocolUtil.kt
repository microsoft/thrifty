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
package com.microsoft.thrifty.util

import com.microsoft.thrifty.TType
import java.io.IOException
import com.microsoft.thrifty.protocol.FieldMetadata
import com.microsoft.thrifty.util.ProtocolUtil
import com.microsoft.thrifty.protocol.ListMetadata
import com.microsoft.thrifty.protocol.SetMetadata
import com.microsoft.thrifty.protocol.MapMetadata
import com.microsoft.thrifty.protocol.Protocol
import java.net.ProtocolException

object ProtocolUtil {
    @JvmStatic
    @Throws(IOException::class)
    fun skip(protocol: Protocol, typeCode: Byte) {
        when (typeCode) {
            TType.BOOL -> protocol.readBool()
            TType.BYTE -> protocol.readByte()
            TType.I16 -> protocol.readI16()
            TType.I32 -> protocol.readI32()
            TType.I64 -> protocol.readI64()
            TType.DOUBLE -> protocol.readDouble()
            TType.STRING -> protocol.readString()
            TType.STRUCT -> {
                protocol.readStructBegin()
                while (true) {
                    val fieldMetadata = protocol.readFieldBegin()
                    if (fieldMetadata.typeId == TType.STOP) {
                        break
                    }
                    skip(protocol, fieldMetadata.typeId)
                    protocol.readFieldEnd()
                }
                protocol.readStructEnd()
            }
            TType.LIST -> {
                val listMetadata = protocol.readListBegin()
                for (i in 0 until listMetadata.size) {
                    skip(protocol, listMetadata.elementTypeId)
                }
                protocol.readListEnd()
            }
            TType.SET -> {
                val setMetadata = protocol.readSetBegin()
                for (i in 0 until setMetadata.size) {
                    skip(protocol, setMetadata.elementTypeId)
                }
                protocol.readSetEnd()
            }
            TType.MAP -> {
                val mapMetadata = protocol.readMapBegin()
                for (i in 0 until mapMetadata.size) {
                    skip(protocol, mapMetadata.keyTypeId)
                    skip(protocol, mapMetadata.valueTypeId)
                }
                protocol.readMapEnd()
            }
            else -> throw ProtocolException("Unrecognized TType value: $typeCode")
        }
    }
}
