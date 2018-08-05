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

import com.microsoft.thrifty.schema.parser.TypeElement
import com.microsoft.thrifty.schema.parser.TypedefElement
import java.util.Objects

class TypedefType : UserType {
    private val oldTypeElement: TypeElement

    private var oldType_: ThriftType? = null

    val oldType: ThriftType
        get() = oldType_!!

    internal constructor(program: Program, element: TypedefElement) : super(program.namespaces, UserElementMixin(element)) {
        this.oldTypeElement = element.oldType
    }

    // visible for testing
    // TODO(ben): Get rid of this, or conversely get rid of the ctors taking a Program
    internal constructor(namespaces: Map<NamespaceScope, String>, element: TypedefElement) : super(namespaces, UserElementMixin(element)) {
        this.oldTypeElement = element.oldType
    }

    private constructor(builder: Builder) : super(builder.namespaces, builder.mixin) {
        this.oldTypeElement = builder.oldTypeElement
        this.oldType_ = builder.oldType
    }

    internal fun link(linker: Linker) {
        this.oldType_ = linker.resolveType(oldTypeElement)
    }

    internal fun validate(linker: Linker) {
        if (oldType_!!.isService) {
            linker.addError(location, "Cannot declare a typedef of a service")
        }

        if (oldType_ == BuiltinType.VOID) {
            linker.addError(location, "Cannot declare a typedef of void")
        }

        // We've already validated that this is not part of an unresolvable
        // cycle of typedefs (e.g. A -> B -> C -> A) during linking; this
        // happens in Linker#resolveTypedefs().
    }

    override val isTypedef: Boolean = true

    override val trueType: ThriftType
        get() = oldType.trueType

    override fun <T> accept(visitor: ThriftType.Visitor<T>): T = visitor.visitTypedef(this)

    override fun withAnnotations(annotations: Map<String, String>): ThriftType {
        return toBuilder()
                .annotations(ThriftType.merge(this.annotations, annotations))
                .build()
    }

    fun toBuilder(): Builder = Builder(this)

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        if (other !is TypedefType) return false

        return this.oldTypeElement == other.oldTypeElement
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), oldTypeElement)
    }

    class Builder internal constructor(typedef: TypedefType) : UserType.UserTypeBuilder<TypedefType, Builder>(typedef) {
        internal var oldTypeElement: TypeElement = typedef.oldTypeElement
        internal var oldType: ThriftType? = typedef.oldType

        fun oldTypeElement(oldTypeElement: TypeElement): Builder = apply {
            this.oldTypeElement = oldTypeElement
        }

        fun oldType(oldType: ThriftType): Builder = apply {
            this.oldType = oldType
        }

        override fun build(): TypedefType = TypedefType(this)
    }
}
