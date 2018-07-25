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

import com.google.common.collect.ImmutableMap
import com.microsoft.thrifty.schema.parser.FunctionElement

import java.util.LinkedHashMap
import java.util.UUID

class ServiceMethod @JvmOverloads internal constructor(
        private val element: FunctionElement,
        private val mixin: UserElementMixin = UserElementMixin(element),
        private val parameters: List<Field> = element.params.map { Field(it) },
        private val exceptions: List<Field> = element.exceptions.map { Field(it) },
        private var returnType: ThriftType? = null
) : UserElement by mixin {

    override val isDeprecated: Boolean
        get() = mixin.isDeprecated

    fun parameters(): List<Field> {
        return parameters
    }

    fun exceptions(): List<Field> {
        return exceptions
    }

    fun returnType(): ThriftType {
        return returnType!!
    }

    fun oneWay(): Boolean {
        return element.oneWay
    }

    override fun uuid(): UUID {
        return mixin.uuid()
    }

    override fun name(): String {
        return mixin.name()
    }

    override fun location(): Location {
        return mixin.location()
    }

    override fun documentation(): String {
        return mixin.documentation()
    }

    override fun annotations(): ImmutableMap<String, String> {
        return mixin.annotations()
    }

    override fun hasJavadoc(): Boolean {
        return mixin.hasJavadoc()
    }

    fun toBuilder(): Builder {
        return Builder(this)
    }

    internal fun link(linker: Linker) {
        for (parameter in parameters) {
            parameter.link(linker)
        }

        for (exception in exceptions) {
            exception.link(linker)
        }

        returnType = linker.resolveType(element.returnType)
    }

    internal fun validate(linker: Linker) {
        if (oneWay() && BuiltinType.VOID != returnType) {
            linker.addError(location(), "oneway methods may not have a non-void return type")
        }

        if (oneWay() && !exceptions.isEmpty()) {
            linker.addError(location(), "oneway methods may not throw exceptions")
        }

        val fieldsById = LinkedHashMap<Int, Field>()
        for (param in parameters) {
            val oldParam = fieldsById.put(param.id(), param)
            if (oldParam != null) {
                val fmt = "Duplicate parameters; param '%s' has the same ID (%s) as param '%s'"
                linker.addError(param.location(), String.format(fmt, param.name(), param.id(), oldParam.name()))

                fieldsById[oldParam.id()] = oldParam
            }
        }

        fieldsById.clear()
        for (exn in exceptions) {
            val oldExn = fieldsById.put(exn.id(), exn)
            if (oldExn != null) {
                val fmt = "Duplicate exceptions; exception '%s' has the same ID (%s) as exception '%s'"
                linker.addError(exn.location(), String.format(fmt, exn.name(), exn.id(), oldExn.name()))

                fieldsById[oldExn.id()] = oldExn
            }
        }

        for (field in exceptions) {
            val type = field.type()
            if (type.isStruct) {
                val struct = type as StructType?
                if (struct!!.isException) {
                    continue
                }
            }

            linker.addError(field.location(), "Only exception types can be thrown")
        }
    }

    class Builder internal constructor(method: ServiceMethod) : AbstractUserElementBuilder<ServiceMethod, Builder>(method.mixin) {

        private val element: FunctionElement = method.element
        private var parameters: List<Field>
        private var exceptions: List<Field>
        private var returnType: ThriftType?

        init {
            this.parameters = method.parameters
            this.exceptions = method.exceptions
            this.returnType = method.returnType
        }

        fun parameters(parameters: List<Field>): Builder = apply {
            this.parameters = parameters.toList()
        }

        fun exceptions(exceptions: List<Field>): Builder = apply {
            this.exceptions = exceptions.toList()
        }

        fun returnType(type: ThriftType): Builder = apply {
            returnType = type
        }

        override fun build(): ServiceMethod {
            return ServiceMethod(element, mixin, parameters, exceptions, returnType)
        }
    }
}
