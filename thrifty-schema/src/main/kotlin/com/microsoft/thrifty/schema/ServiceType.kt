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

import com.microsoft.thrifty.schema.parser.ServiceElement
import com.microsoft.thrifty.schema.parser.TypeElement

import java.util.ArrayDeque
import java.util.LinkedHashMap

class ServiceType : UserType {
    private val methods: List<ServiceMethod>
    private val extendsServiceType: TypeElement?

    // This is intentionally too broad - it is not legal for a service to extend
    // a non-service type, but if we've parsed that we need to keep the invalid
    // state long enough to catch it during link validation.
    var extendsService: ThriftType? = null
        private set

    internal constructor(program: Program, element: ServiceElement) : super(program.namespaces, UserElementMixin(element)) {

        this.extendsServiceType = element.extendsService
        this.methods = element.functions.map { ServiceMethod(it) }
    }

    private constructor(builder: Builder) : super(builder.namespaces, builder.mixin) {
        this.methods = builder.methods
        this.extendsServiceType = builder.extendsServiceType
        this.extendsService = builder.extendsService
    }

    fun methods(): List<ServiceMethod> = methods

    fun extendsService(): ThriftType? = extendsService

    override val isService: Boolean = true

    override fun <T> accept(visitor: ThriftType.Visitor<T>): T = visitor.visitService(this)

    override fun withAnnotations(annotations: Map<String, String>): ThriftType {
        return toBuilder()
                .annotations(ThriftType.merge(this.annotations, annotations))
                .build()
    }

    fun toBuilder(): Builder = Builder(this)

    internal fun link(linker: Linker) {
        for (method in methods) {
            method.link(linker)
        }

        if (this.extendsServiceType != null) {
            this.extendsService = linker.resolveType(extendsServiceType)
        }
    }

    internal fun validate(linker: Linker) {
        // Validate the following properties:
        // 1. If the service extends a type, that the type is itself a service
        // 2. The service contains no duplicate methods, including those inherited from base types.
        // 3. All service methods themselves are valid.

        val methodsByName = LinkedHashMap<String, ServiceMethod>()

        val hierarchy = ArrayDeque<ServiceType>()

        if (extendsService != null) {
            if (!extendsService!!.isService) {
                linker.addError(location, "Base type '" + extendsService!!.name + "' is not a service")
            }
        }

        // Assume base services have already been validated
        var baseType = extendsService
        while (baseType != null) {
            if (!baseType.isService) {
                break
            }

            val svc = baseType as ServiceType
            hierarchy.add(svc)

            baseType = svc.extendsService
        }


        while (!hierarchy.isEmpty()) {
            // Process from most- to least-derived services; that way, if there
            // is a name conflict, we'll report the conflict with the least-derived
            // class.
            val svc = hierarchy.remove()

            for (serviceMethod in svc.methods()) {
                // Add the base-type method names to the map.  In this case,
                // we don't care about duplicates because the base types have
                // already been validated and we have already reported that error.
                methodsByName[serviceMethod.name] = serviceMethod
            }
        }

        for (method in methods) {
            val conflictingMethod = methodsByName.put(method.name, method)
            if (conflictingMethod != null) {
                methodsByName[conflictingMethod.name] = conflictingMethod

                linker.addError(method.location, "Duplicate method; '" + method.name
                        + "' conflicts with another method declared at " + conflictingMethod.location)
            }
        }

        for (method in methods) {
            method.validate(linker)
        }
    }

    class Builder internal constructor(type: ServiceType) : UserType.UserTypeBuilder<ServiceType, Builder>(type) {
        internal var methods: List<ServiceMethod> = type.methods
        internal val extendsServiceType: TypeElement? = type.extendsServiceType
        internal var extendsService: ThriftType? = type.extendsService

        init {
            this.methods = type.methods
            this.extendsService = type.extendsService
        }

        fun methods(methods: List<ServiceMethod>): Builder = apply {
            this.methods = methods
        }

        fun extendsService(extendsService: ThriftType): Builder = apply {
            this.extendsService = extendsService
        }

        override fun build(): ServiceType {
            return ServiceType(this)
        }
    }
}
