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

import com.microsoft.thrifty.schema.parser.ConstValueElement
import com.microsoft.thrifty.schema.parser.FieldElement

class Field @JvmOverloads internal constructor( // TODO(ben): Remove JvmOverloads when kotlin port is done
        private val element: FieldElement,
        private val mixin: UserElementMixin = UserElementMixin(element),
        private var type: ThriftType? = null
) : UserElement by mixin {

    val isRedacted: Boolean
        get() = mixin.hasThriftOrJavadocAnnotation("redacted")

    val isObfuscated: Boolean
        get() = mixin.hasThriftOrJavadocAnnotation("obfuscated")

    override val isDeprecated: Boolean
        get() = mixin.isDeprecated

    fun type(): ThriftType = type!!

    fun id(): Int = element.fieldId

    fun optional(): Boolean = element.requiredness === Requiredness.OPTIONAL

    fun required(): Boolean = element.requiredness === Requiredness.REQUIRED

    fun defaultValue(): ConstValueElement? = element.constValue

    fun typedefName(): String? {
        return type?.let {
            if (it.isTypedef) it.name else null
        }
    }

    fun toBuilder(): Builder = Builder(this)

    internal fun link(linker: Linker) {
        this.type = linker.resolveType(element.type)
    }

    internal fun validate(linker: Linker) {
        val value = element.constValue
        if (value != null) {
            try {
                Constant.validate(linker, value, type!!)
            } catch (e: IllegalStateException) {
                linker.addError(value.location, e.message ?: "Error validating default field value")
            }

        }
    }

    class Builder internal constructor(field: Field) : AbstractUserElementBuilder<Field, Builder>(field.mixin) {
        private val fieldElement: FieldElement = field.element
        private var fieldType: ThriftType? = null

        init {
            this.fieldType = field.type
        }

        fun type(type: ThriftType): Builder = apply {
            this.fieldType = type
        }

        override fun build(): Field {
            return Field(fieldElement, mixin, fieldType)
        }
    }
}
