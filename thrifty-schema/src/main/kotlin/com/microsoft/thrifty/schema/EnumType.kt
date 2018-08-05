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

import com.microsoft.thrifty.schema.parser.EnumElement

/**
 * Represents an enumeration defined in Thrift IDL.
 */
class EnumType : UserType {

    val members: List<EnumMember>

    internal constructor(element: EnumElement, namespaces: Map<NamespaceScope, String>): super(UserElementMixin(element, namespaces)) {
        this.members = element.members.map { EnumMember(it, namespaces) }
    }

    private constructor(builder: Builder) : super(builder.mixin) {
        this.members = builder.members
    }

    fun findMemberByName(name: String): EnumMember {
        return members.first { it.name == name }
    }

    fun findMemberById(id: Int): EnumMember {
        return members.first { it.value == id }
    }

    override val isEnum: Boolean = true

    override fun <T> accept(visitor: ThriftType.Visitor<T>): T = visitor.visitEnum(this)

    override fun withAnnotations(annotations: Map<String, String>): ThriftType {
        return toBuilder()
                .annotations(mergeAnnotations(this.annotations, annotations))
                .build()
    }

    fun toBuilder(): Builder = Builder(this)

    class Builder internal constructor(enumType: EnumType) : UserType.UserTypeBuilder<EnumType, Builder>(enumType) {
        internal var members: List<EnumMember> = enumType.members
            private set

        fun members(members: List<EnumMember>): Builder = apply {
            this.members = members.toList()
        }

        override fun build(): EnumType = EnumType(this)
    }
}
