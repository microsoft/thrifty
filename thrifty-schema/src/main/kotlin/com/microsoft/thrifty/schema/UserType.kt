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

import java.util.Objects

/**
 * Base type of all user-defined Thrift IDL types, including structs, unions,
 * exceptions, services, and typedefs.
 */
abstract class UserType internal constructor(
        private val namespaces: Map<NamespaceScope, String>,
        private val mixin: UserElementMixin
) : ThriftType(mixin.name()), UserElement by mixin {

    override val isDeprecated: Boolean
        get() = mixin.isDeprecated

    internal constructor(program: Program, mixin: UserElementMixin) : this(program.namespaces, mixin)

    fun getNamespaceFor(namespace: NamespaceScope): String? {
        return when (namespace) {
            NamespaceScope.ALL -> namespaces[namespace]
            else -> namespaces[namespace] ?: namespaces[NamespaceScope.ALL]
        }
    }

    override fun name(): String = mixin.name()

    fun namespaces(): Map<NamespaceScope, String> = namespaces

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        if (other !is UserType) return false

        return this.mixin == other.mixin && this.namespaces == other.namespaces
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), mixin, namespaces)
    }

    abstract class UserTypeBuilder<TType : UserType, TBuilder : UserType.UserTypeBuilder<TType, TBuilder>> internal constructor(
            type: TType
    ) : AbstractUserElementBuilder<TType, TBuilder>(type.mixin) {

        internal var namespaces: Map<NamespaceScope, String> = type.namespaces

        fun namespaces(namespaces: Map<NamespaceScope, String>): TBuilder {
            this.namespaces = namespaces

            @Suppress("UNCHECKED_CAST")
            return this as TBuilder
        }
    }
}
