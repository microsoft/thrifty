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

import com.microsoft.thrifty.schema.parser.FunctionElement

import java.util.LinkedHashMap

/**
 * Represents a method defined by a Thrift service.
 *
 * @property parameters The parameters accepted by this method.
 * @property exceptions The exceptions thrown by this method.
 */
class ServiceMethod private constructor(
        private val element: FunctionElement,
        private val mixin: UserElementMixin,
        val parameters: List<Field> = element.params.map { Field(it, mixin.namespaces) },
        val exceptions: List<Field> = element.exceptions.map { Field(it, mixin.namespaces) },
        private var returnType_: ThriftType? = null
) : UserElement by mixin {

    /**
     * The type of value returned by this method, or [BuiltinType.VOID].
     */
    val returnType: ThriftType
        get() = returnType_!!

    /**
     * True if this method was declared as `oneway`, otherwise false.
     */
    val oneWay: Boolean
        get() = element.oneWay

    internal constructor(element: FunctionElement, namespaces: Map<NamespaceScope, String>)
            : this(element, UserElementMixin(element, namespaces))

    /**
     * Creates a new [Builder] initialized with this method's values.
     */
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

        returnType_ = linker.resolveType(element.returnType)
    }

    internal fun validate(linker: Linker) {
        if (oneWay && BuiltinType.VOID != returnType) {
            linker.addError(location, "oneway methods may not have a non-void return type")
        }

        if (oneWay && !exceptions.isEmpty()) {
            linker.addError(location, "oneway methods may not throw exceptions")
        }

        val fieldsById = LinkedHashMap<Int, Field>()
        for (param in parameters) {
            val oldParam = fieldsById.put(param.id, param)
            if (oldParam != null) {
                val fmt = "Duplicate parameters; param '%s' has the same ID (%s) as param '%s'"
                linker.addError(param.location, String.format(fmt, param.name, param.id, oldParam.name))

                fieldsById[oldParam.id] = oldParam
            }
        }

        fieldsById.clear()
        for (exn in exceptions) {
            val oldExn = fieldsById.put(exn.id, exn)
            if (oldExn != null) {
                val fmt = "Duplicate exceptions; exception '%s' has the same ID (%s) as exception '%s'"
                linker.addError(exn.location, String.format(fmt, exn.name, exn.id, oldExn.name))

                fieldsById[oldExn.id] = oldExn
            }
        }

        for (field in exceptions) {
            val type = field.type
            if (type.isStruct) {
                val struct = type as StructType?
                if (struct!!.isException) {
                    continue
                }
            }

            linker.addError(field.location, "Only exception types can be thrown")
        }
    }

    /**
     * An object that can create new [ServiceMethod] instances.
     */
    class Builder internal constructor(
            method: ServiceMethod
    ) : AbstractUserElementBuilder<ServiceMethod, Builder>(method.mixin) {

        private val element: FunctionElement = method.element
        private var parameters: List<Field>
        private var exceptions: List<Field>
        private var returnType: ThriftType?

        init {
            this.parameters = method.parameters
            this.exceptions = method.exceptions
            this.returnType = method.returnType
        }

        /**
         * Use the given [parameters] for the method under construction.
         */
        fun parameters(parameters: List<Field>): Builder = apply {
            this.parameters = parameters.toList()
        }

        /**
         * Use the given [exceptions] for the method under construction.
         */
        fun exceptions(exceptions: List<Field>): Builder = apply {
            this.exceptions = exceptions.toList()
        }

        /**
         * Use the given return [type] for the method under construction.
         */
        fun returnType(type: ThriftType): Builder = apply {
            returnType = type
        }

        /**
         * Creates a new [ServiceMethod] instance.
         */
        override fun build(): ServiceMethod {
            return ServiceMethod(element, mixin, parameters, exceptions, returnType)
        }
    }
}
