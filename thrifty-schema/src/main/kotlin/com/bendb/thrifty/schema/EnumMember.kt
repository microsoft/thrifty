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
package com.bendb.thrifty.schema

import com.bendb.thrifty.schema.parser.EnumMemberElement

/**
 * A named member of an [EnumType].
 *
 * @property value The integer constant associated with this enum member.
 */
class EnumMember private constructor(
        private val mixin: UserElementMixin,
        val value: Int
) : UserElement by mixin {

    internal constructor(element: EnumMemberElement, namespaces: Map<NamespaceScope, String>)
            : this(UserElementMixin(element, namespaces), element.value)

    private constructor(builder: Builder)
            : this(builder.mixin, builder.value)

    /** @inheritdoc */
    override fun toString(): String {
        return name
    }

    /**
     * Returns a [Builder] initialized with this enum member.
     */
    fun toBuilder(): Builder {
        return Builder(this)
    }

    /**
     * An object that can construct [EnumMembers][EnumMember].
     */
    class Builder internal constructor(member: EnumMember) : AbstractUserElementBuilder<EnumMember, Builder>(member.mixin) {
        internal var value: Int = member.value
            private set

        /**
         * Use the given [value] for the member under construction.
         */
        fun value(value: Int): Builder = apply {
            require(value >= 0) { "Enum values cannot be less than zero" }
            this.value = value
        }

        override fun build(): EnumMember {
            return EnumMember(this)
        }
    }
}
