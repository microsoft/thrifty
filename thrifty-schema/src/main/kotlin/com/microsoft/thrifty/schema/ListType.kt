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

/**
 * Represents a Thrift `list<T>`.
 */
class ListType internal constructor(
        val elementType: ThriftType,
        override val annotations: Map<String, String> = emptyMap()
) : ThriftType("list<" + elementType.name + ">") {

    override val isList: Boolean = true

    override fun <T> accept(visitor: ThriftType.Visitor<T>): T = visitor.visitList(this)

    override fun withAnnotations(annotations: Map<String, String>): ThriftType {
        return ListType(elementType, mergeAnnotations(this.annotations, annotations))
    }

    fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder(
            private var elementType: ThriftType,
            private var annotations: Map<String, String>
    ) {

        internal constructor(type: ListType) : this(type.elementType, type.annotations)

        fun elementType(elementType: ThriftType): Builder = apply {
            this.elementType = elementType
        }

        fun annotations(annotations: Map<String, String>): Builder = apply {
            this.annotations = annotations
        }

        fun build(): ListType {
            return ListType(elementType, annotations)
        }
    }
}
