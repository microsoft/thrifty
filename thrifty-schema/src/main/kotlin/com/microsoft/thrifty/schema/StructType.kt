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

import com.microsoft.thrifty.schema.parser.StructElement

import java.util.LinkedHashMap
import java.util.Objects

/**
 * Represents a 'structured' type in Thrift.  A StructType could be any of
 * 'struct', 'union', or 'exception'.
 */
class StructType : UserType {
    private val structType: StructElement.Type

    @get:JvmName("fields")
    val fields: List<Field>

    val isUnion: Boolean
        get() = structType === StructElement.Type.UNION

    val isException: Boolean
        get() = structType === StructElement.Type.EXCEPTION

    internal constructor(program: Program, element: StructElement) : super(program.namespaces, UserElementMixin(element)) {

        this.structType = element.type
        this.fields = element.fields
                .map { Field(it) }
    }

    private constructor(builder: Builder) : super(builder.namespaces, builder.mixin) {
        this.structType = builder.structType
        this.fields = builder.fields
    }

    override fun isStruct(): Boolean {
        return true
    }

    override fun <T> accept(visitor: ThriftType.Visitor<T>): T {
        return visitor.visitStruct(this)
    }

    override fun withAnnotations(annotations: Map<String, String>): ThriftType {
        return toBuilder()
                .annotations(ThriftType.merge(this.annotations(), annotations))
                .build()
    }

    fun toBuilder(): Builder {
        return Builder(this)
    }

    internal fun link(linker: Linker) {
        for (field in fields) {
            field.link(linker)
        }
    }

    internal fun validate(linker: Linker) {
        for (field in fields) {
            field.validate(linker)
        }

        val fieldsById = LinkedHashMap<Int, Field>(fields.size)
        for (field in fields) {
            val dupe = fieldsById.put(field.id(), field)
            if (dupe != null) {
                linker.addError(dupe.location(),
                        "Duplicate field IDs: " + field.name() + " and " + dupe.name()
                                + " both have the same ID (" + field.id() + ")")
            }

            if (isUnion && field.required()) {
                linker.addError(field.location(), "Unions may not have required fields: " + field.name())
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        val that = other as? StructType ?: return false
        return this.structType == that.structType && this.fields == that.fields
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), structType, fields)
    }

    class Builder internal constructor(type: StructType) : UserType.UserTypeBuilder<StructType, Builder>(type) {
        internal var structType: StructElement.Type = type.structType
        internal var fields: List<Field> = type.fields

        fun asUnion(): Builder = structType(StructElement.Type.UNION)
        fun asStruct(): Builder = structType(StructElement.Type.STRUCT)
        fun asException(): Builder = structType(StructElement.Type.EXCEPTION)

        fun structType(structType: StructElement.Type): Builder = apply {
            this.structType = structType
        }

        fun fields(fields: List<Field>): Builder = apply {
            this.fields = fields.toList()
        }

        override fun build(): StructType {
            return StructType(this)
        }
    }
}
