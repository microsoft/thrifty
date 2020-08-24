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

import com.microsoft.thrifty.Adapter
import com.microsoft.thrifty.Struct
import com.microsoft.thrifty.StructBuilder
import com.microsoft.thrifty.TType
import com.microsoft.thrifty.ThriftField
import com.microsoft.thrifty.util.ProtocolUtil.skip
import java.io.IOException

class Xtruct private constructor(builder: Builder) : Struct {
    @ThriftField(fieldId = 1)
    val string_thing: String?

    @ThriftField(fieldId = 4)
    val byte_thing: Byte?

    @ThriftField(fieldId = 9)
    val i32_thing: Int?

    @ThriftField(fieldId = 11)
    val i64_thing: Long?

    @ThriftField(fieldId = 13)
    val double_thing: Double?

    @ThriftField(fieldId = 15)
    val bool_thing: Boolean?
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other !is Xtruct) return false
        val that = other
        return ((string_thing === that.string_thing || string_thing != null && string_thing == that.string_thing)
                && (byte_thing === that.byte_thing || byte_thing != null && byte_thing == that.byte_thing)
                && (i32_thing === that.i32_thing || i32_thing != null && i32_thing == that.i32_thing)
                && (i64_thing === that.i64_thing || i64_thing != null && i64_thing == that.i64_thing)
                && (double_thing === that.double_thing || double_thing != null && double_thing == that.double_thing)
                && (bool_thing === that.bool_thing || bool_thing != null && bool_thing == that.bool_thing))
    }

    override fun hashCode(): Int {
        var code = 16777619
        code = code xor if (string_thing == null) 0 else string_thing.hashCode()
        code *= -0x7ee3623b
        code = code xor if (byte_thing == null) 0 else byte_thing.hashCode()
        code *= -0x7ee3623b
        code = code xor if (i32_thing == null) 0 else i32_thing.hashCode()
        code *= -0x7ee3623b
        code = code xor if (i64_thing == null) 0 else i64_thing.hashCode()
        code *= -0x7ee3623b
        code = code xor if (double_thing == null) 0 else double_thing.hashCode()
        code *= -0x7ee3623b
        code = code xor if (bool_thing == null) 0 else bool_thing.hashCode()
        code *= -0x7ee3623b
        return code
    }

    override fun toString(): String {
        return "Xtruct{string_thing=" + string_thing + ", byte_thing=" + byte_thing + ", i32_thing=" + i32_thing + ", i64_thing=" + i64_thing + ", double_thing=" + double_thing + ", bool_thing=" + bool_thing + "}"
    }

    @Throws(IOException::class)
    override fun write(protocol: Protocol) {
        ADAPTER.write(protocol, this)
    }

    class Builder : StructBuilder<Xtruct> {
        var string_thing: String? = null
        var byte_thing: Byte? = null
        var i32_thing: Int? = null
        var i64_thing: Long? = null
        var double_thing: Double? = null
        var bool_thing: Boolean? = null

        constructor() {}
        constructor(struct: Xtruct) {
            string_thing = struct.string_thing
            byte_thing = struct.byte_thing
            i32_thing = struct.i32_thing
            i64_thing = struct.i64_thing
            double_thing = struct.double_thing
            bool_thing = struct.bool_thing
        }

        fun string_thing(string_thing: String?): Builder {
            this.string_thing = string_thing
            return this
        }

        fun byte_thing(byte_thing: Byte?): Builder {
            this.byte_thing = byte_thing
            return this
        }

        fun i32_thing(i32_thing: Int?): Builder {
            this.i32_thing = i32_thing
            return this
        }

        fun i64_thing(i64_thing: Long?): Builder {
            this.i64_thing = i64_thing
            return this
        }

        fun double_thing(double_thing: Double?): Builder {
            this.double_thing = double_thing
            return this
        }

        fun bool_thing(bool_thing: Boolean?): Builder {
            this.bool_thing = bool_thing
            return this
        }

        override fun build(): Xtruct {
            return Xtruct(this)
        }

        override fun reset() {
            string_thing = null
            byte_thing = null
            i32_thing = null
            i64_thing = null
            double_thing = null
            bool_thing = null
        }
    }

    private class XtructAdapter : Adapter<Xtruct, Builder> {
        @Throws(IOException::class)
        override fun write(protocol: Protocol, struct: Xtruct) {
            protocol.writeStructBegin("Xtruct")
            if (struct.string_thing != null) {
                protocol.writeFieldBegin("string_thing", 1, TType.STRING)
                protocol.writeString(struct.string_thing)
                protocol.writeFieldEnd()
            }
            if (struct.byte_thing != null) {
                protocol.writeFieldBegin("byte_thing", 4, TType.BYTE)
                protocol.writeByte(struct.byte_thing)
                protocol.writeFieldEnd()
            }
            if (struct.i32_thing != null) {
                protocol.writeFieldBegin("i32_thing", 9, TType.I32)
                protocol.writeI32(struct.i32_thing)
                protocol.writeFieldEnd()
            }
            if (struct.i64_thing != null) {
                protocol.writeFieldBegin("i64_thing", 11, TType.I64)
                protocol.writeI64(struct.i64_thing)
                protocol.writeFieldEnd()
            }
            if (struct.double_thing != null) {
                protocol.writeFieldBegin("double_thing", 13, TType.DOUBLE)
                protocol.writeDouble(struct.double_thing)
                protocol.writeFieldEnd()
            }
            if (struct.bool_thing != null) {
                protocol.writeFieldBegin("bool_thing", 15, TType.BOOL)
                protocol.writeBool(struct.bool_thing)
                protocol.writeFieldEnd()
            }
            protocol.writeFieldStop()
            protocol.writeStructEnd()
        }

        @Throws(IOException::class)
        override fun read(protocol: Protocol, builder: Builder): Xtruct {
            protocol.readStructBegin()
            while (true) {
                val field = protocol.readFieldBegin()
                if (field.typeId == TType.STOP) {
                    break
                }
                when (field.fieldId.toInt()) {
                    1 -> {
                        if (field.typeId == TType.STRING) {
                            val value = protocol.readString()
                            builder.string_thing(value)
                        } else {
                            skip(protocol, field.typeId)
                        }
                    }
                    4 -> {
                        if (field.typeId == TType.BYTE) {
                            val value = protocol.readByte()
                            builder.byte_thing(value)
                        } else {
                            skip(protocol, field.typeId)
                        }
                    }
                    9 -> {
                        if (field.typeId == TType.I32) {
                            val value = protocol.readI32()
                            builder.i32_thing(value)
                        } else {
                            skip(protocol, field.typeId)
                        }
                    }
                    11 -> {
                        if (field.typeId == TType.I64) {
                            val value = protocol.readI64()
                            builder.i64_thing(value)
                        } else {
                            skip(protocol, field.typeId)
                        }
                    }
                    13 -> {
                        if (field.typeId == TType.DOUBLE) {
                            val value = protocol.readDouble()
                            builder.double_thing(value)
                        } else {
                            skip(protocol, field.typeId)
                        }
                    }
                    15 -> {
                        if (field.typeId == TType.BOOL) {
                            val value = protocol.readBool()
                            builder.bool_thing(value)
                        } else {
                            skip(protocol, field.typeId)
                        }
                    }
                    else -> {
                        skip(protocol, field.typeId)
                    }
                }
                protocol.readFieldEnd()
            }
            protocol.readStructEnd()
            return builder.build()
        }

        @Throws(IOException::class)
        override fun read(protocol: Protocol): Xtruct {
            return read(protocol, Builder())
        }
    }

    companion object {
        @JvmField
        val ADAPTER: Adapter<Xtruct, Builder> = XtructAdapter()
    }

    init {
        string_thing = builder.string_thing
        byte_thing = builder.byte_thing
        i32_thing = builder.i32_thing
        i64_thing = builder.i64_thing
        double_thing = builder.double_thing
        bool_thing = builder.bool_thing
    }
}