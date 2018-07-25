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
package com.microsoft.thrifty.schema

import com.google.common.collect.ImmutableMap

class BuiltinType @JvmOverloads internal constructor(
        name: String,
        private val annotations: Map<String, String> = emptyMap()
) : ThriftType(name) {

    val isNumeric: Boolean
        get() = (this == I8
                || this == I16
                || this == I32
                || this == I64
                || this == DOUBLE)

    override fun isBuiltin(): Boolean = true

    override fun <T> accept(visitor: ThriftType.Visitor<T>): T {
        return when (this) {
            BOOL -> visitor.visitBool(this)
            BYTE, I8 -> visitor.visitByte(this)
            I16 -> visitor.visitI16(this)
            I32 -> visitor.visitI32(this)
            I64 -> visitor.visitI64(this)
            DOUBLE -> visitor.visitDouble(this)
            STRING -> visitor.visitString(this)
            BINARY -> visitor.visitBinary(this)
            else -> throw AssertionError("Unexpected ThriftType: ${name()}")
        }
    }

    override fun annotations(): Map<String, String> {
        return annotations
    }

    override fun withAnnotations(annotations: Map<String, String>): ThriftType {
        return BuiltinType(name(), ThriftType.merge(this.annotations, annotations))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false

        val that = other as BuiltinType

        if (this.name() == that.name()) {
            return true
        }

        // 'byte' and 'i8' are synonyms
        val synonyms = arrayOf(BYTE.name(), I8.name())
        return this.name() in synonyms && that.name() in synonyms
    }

    override fun hashCode(): Int {
        var name = name()
        if (name == I8.name()) {
            name = BYTE.name()
        }
        return name.hashCode()
    }

    companion object {
        @JvmField val BOOL: ThriftType = BuiltinType("bool")
        @JvmField val BYTE: ThriftType = BuiltinType("byte")
        @JvmField val I8: ThriftType = BuiltinType("i8")
        @JvmField val I16: ThriftType = BuiltinType("i16")
        @JvmField val I32: ThriftType = BuiltinType("i32")
        @JvmField val I64: ThriftType = BuiltinType("i64")
        @JvmField val DOUBLE: ThriftType = BuiltinType("double")
        @JvmField val STRING: ThriftType = BuiltinType("string")
        @JvmField val BINARY: ThriftType = BuiltinType("binary")
        @JvmField val VOID: ThriftType = BuiltinType("void")

        private val BUILTINS: ImmutableMap<String, ThriftType>

        init {
            BUILTINS = ImmutableMap.builder<String, ThriftType>()
                    .put(BOOL.name(), BOOL)
                    .put(BYTE.name(), BYTE)
                    .put(I8.name(), I8)
                    .put(I16.name(), I16)
                    .put(I32.name(), I32)
                    .put(I64.name(), I64)
                    .put(DOUBLE.name(), DOUBLE)
                    .put(STRING.name(), STRING)
                    .put(BINARY.name(), BINARY)
                    .put(VOID.name(), VOID)
                    .build()
        }

        @JvmStatic
        fun get(name: String): ThriftType? {
            return BUILTINS[name]
        }
    }
}
