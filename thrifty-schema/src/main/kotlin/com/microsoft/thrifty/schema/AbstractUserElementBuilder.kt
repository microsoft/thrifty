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
 * The base type of all UserElement-derived builders.
 *
 * @param TElement the type of element being built
 * @param TBuilder the most-derived type of the builder
 */
@Suppress("UNCHECKED_CAST")
abstract class AbstractUserElementBuilder<TElement : UserElement, TBuilder : AbstractUserElementBuilder<TElement, TBuilder>> internal constructor(
        internal var mixin: UserElementMixin
) {

    fun name(name: String): TBuilder {
        mixin = mixin.toBuilder().name(name).build()
        return this as TBuilder
    }

    fun location(location: Location): TBuilder {
        mixin = mixin.toBuilder().location(location).build()
        return this as TBuilder
    }

    fun documentation(documentation: String): TBuilder {
        mixin = mixin.toBuilder().documentation(documentation).build()
        return this as TBuilder
    }

    fun annotations(annotations: Map<String, String>): TBuilder {
        mixin = mixin.toBuilder().annotations(annotations).build()
        return this as TBuilder
    }

    fun type(type: TElement): TBuilder {
        mixin = mixin.toBuilder()
                .name(type.name())
                .location(type.location())
                .documentation(type.documentation())
                .annotations(type.annotations())
                .build()
        return this as TBuilder
    }

    abstract fun build(): TElement
}
