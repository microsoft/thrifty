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

class BuiltinType internal constructor(
        name: String,
        override val annotations: Map<String, String> = emptyMap()
) : ThriftType(name) {

    val isNumeric: Boolean
        get() = (this == I8
                || this == I16
                || this == I32
                || this == I64
                || this == DOUBLE)

    override val isBuiltin: Boolean = true

    override fun <T> accept(visitor: ThriftType.Visitor<T>): T {
        return when (this) {
            VOID -> visitor.visitVoid(this)
            BOOL -> visitor.visitBool(this)
            BYTE, I8 -> visitor.visitByte(this)
            I16 -> visitor.visitI16(this)
            I32 -> visitor.visitI32(this)
            I64 -> visitor.visitI64(this)
            DOUBLE -> visitor.visitDouble(this)
            STRING -> visitor.visitString(this)
            BINARY -> visitor.visitBinary(this)
            else -> throw AssertionError("Unexpected ThriftType: $name")
        }
    }

    override fun withAnnotations(annotations: Map<String, String>): ThriftType {
        return BuiltinType(name, mergeAnnotations(this.annotations, annotations))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false

        val that = other as BuiltinType

        if (this.name == that.name) {
            return true
        }

        // 'byte' and 'i8' are synonyms
        val synonyms = arrayOf(BYTE.name, I8.name)
        return this.name in synonyms && that.name in synonyms
    }

    override fun hashCode(): Int {
        var name = name
        if (name == I8.name) {
            name = BYTE.name
        }
        return name.hashCode()
    }

    companion object {
        val BOOL: ThriftType = BuiltinType("bool")
        val BYTE: ThriftType = BuiltinType("byte")
        val I8: ThriftType = BuiltinType("i8")
        val I16: ThriftType = BuiltinType("i16")
        val I32: ThriftType = BuiltinType("i32")
        val I64: ThriftType = BuiltinType("i64")
        val DOUBLE: ThriftType = BuiltinType("double")
        val STRING: ThriftType = BuiltinType("string")
        val BINARY: ThriftType = BuiltinType("binary")
        val VOID: ThriftType = BuiltinType("void")

        private val BUILTINS = listOf(BOOL, BYTE, I8, I16, I32, I64, DOUBLE, STRING, BINARY, VOID)
                .map { it.name to it }
                .toMap()

        fun get(name: String): ThriftType? {
            return BUILTINS[name]
        }
    }
}
