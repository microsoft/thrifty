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

import com.bendb.thrifty.schema.parser.ConstValueElement
import com.bendb.thrifty.schema.parser.FieldElement

/**
 * Represents a named value in a struct, union, or exception; a field also
 * represents parameters accepted by or exceptions thrown by service methods.
 */
class Field private constructor(
        private val element: FieldElement,
        private val mixin: UserElementMixin,
        private var type_: ThriftType? = null
) : UserElement by mixin {

    /**
     * True if this field should be redacted when printed as a string.
     */
    val isRedacted: Boolean
        get() = mixin.hasThriftOrJavadocAnnotation("redacted")

    /**
     * True if this field should be obfuscated when printed as a string.
     *
     * The difference is that redaction eliminates _all_ information, while
     * obfuscation preserves _some_.  Typically, scalar values will be rendered
     * as a hash code, and collections will be rendered as a typename plus a
     * size, e.g. `list<string>(size=12)`.
     */
    val isObfuscated: Boolean
        get() = mixin.hasThriftOrJavadocAnnotation("obfuscated")

    override val isDeprecated: Boolean
        get() = mixin.isDeprecated

    /**
     * The type of value contained within this field.
     */
    val type: ThriftType
        get() = type_!!

    /**
     * The integer ID of this field in its containing structure.
     */
    val id: Int
        get() = element.fieldId

    /**
     * True if this field is explicitly marked `optional`, otherwise false.
     */
    val optional: Boolean
        get() = element.requiredness === Requiredness.OPTIONAL

    /**
     * True if this field is explicitly marked `required`, otherwise false.
     */
    val required: Boolean
        get() = element.requiredness === Requiredness.REQUIRED

    /**
     * The field's default value, if any.
     */
    val defaultValue: ConstValueElement?
        get() = element.constValue

    /**
     * If this field's type is a typedef, this value will be the name of the
     * typedef itself - that is, the "new" name, not the aliased type's name.
     */
    val typedefName: String?
        get() {
            return type_?.let {
                if (it.isTypedef) it.name else null
            }
        }

    internal constructor(element: FieldElement, namespaces: Map<NamespaceScope, String>)
            : this(element, UserElementMixin(element, namespaces))

    /**
     * Creates a [Builder] initialized with this field's values.
     */
    fun toBuilder(): Builder = Builder(this)

    internal fun link(linker: Linker) {
        this.type_ = linker.resolveType(element.type)
    }

    internal fun validate(linker: Linker) {
        val value = element.constValue
        if (value != null) {
            try {
                Constant.validate(linker, value, type_!!)
            } catch (e: IllegalStateException) {
                linker.addError(value.location, e.message ?: "Error validating default field value")
            }

        }
    }

    /**
     * An object that can build new [Field] instances.
     */
    class Builder internal constructor(field: Field) : AbstractUserElementBuilder<Field, Builder>(field.mixin) {
        private val fieldElement: FieldElement = field.element
        private var fieldType: ThriftType? = null

        init {
            this.fieldType = field.type_
        }

        /**
         * Use the given [type] for the [Field] under construction.
         */
        fun type(type: ThriftType): Builder = apply {
            this.fieldType = type
        }

        override fun build(): Field {
            return Field(fieldElement, mixin, fieldType)
        }
    }
}
