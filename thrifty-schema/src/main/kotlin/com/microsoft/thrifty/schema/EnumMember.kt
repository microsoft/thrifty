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

import com.google.common.base.Preconditions
import com.microsoft.thrifty.schema.parser.EnumMemberElement

/**
 * A named member of an [EnumType].
 */
class EnumMember private constructor(
        private val mixin: UserElementMixin,
        @get:JvmName("value")
        val value: Int
) : UserElement by mixin {

    internal constructor(element: EnumMemberElement)
            : this(UserElementMixin(element), element.value)

    private constructor(builder: Builder)
            : this(builder.mixin, builder.value)

    override fun toString(): String {
        return name()
    }

    fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder(member: EnumMember) : AbstractUserElementBuilder<EnumMember, Builder>(member.mixin) {
        internal var value: Int = member.value
            private set

        fun value(value: Int): Builder {
            Preconditions.checkArgument(value >= 0, "Enum values cannot be less than zero")
            this.value = value
            return this
        }

        override fun build(): EnumMember {
            return EnumMember(this)
        }
    }
}
