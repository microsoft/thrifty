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
 * Encapsulates all types, values, and services defined in a set of Thrift
 * files.
 *
 * Strictly speaking, this is a lossy representation - the original filesystem
 * structure of the source Programs is not preserved.  As such, this isn't a
 * suitable representation from which to generate C++ code, or any other
 * module-based language for the matter.  But as we're only concerned with Java
 * here, it's perfectly convenient.
 */
class Schema {
    @get:JvmName("structs")
    val structs: List<StructType>

    @get:JvmName("unions")
    val unions: List<StructType>

    @get:JvmName("exceptions")
    val exceptions: List<StructType>

    @get:JvmName("enums")
    val enums: List<EnumType>

    @get:JvmName("constants")
    val constants: List<Constant>

    @get:JvmName("typedefs")
    val typedefs: List<TypedefType>

    @get:JvmName("services")
    val services: List<ServiceType>

    internal constructor(programs: Iterable<Program>) {
        val structs = mutableListOf<StructType>()
        val unions = mutableListOf<StructType>()
        val exceptions = mutableListOf<StructType>()
        val enums = mutableListOf<EnumType>()
        val constants = mutableListOf<Constant>()
        val typedefs = mutableListOf<TypedefType>()
        val services = mutableListOf<ServiceType>()

        for (program in programs) {
            structs.addAll(program.structs)
            unions.addAll(program.unions)
            exceptions.addAll(program.exceptions)
            enums.addAll(program.enums)
            constants.addAll(program.constants)
            typedefs.addAll(program.typedefs)
            services.addAll(program.services)
        }

        this.structs = structs
        this.unions = unions
        this.exceptions = exceptions
        this.enums = enums
        this.constants = constants
        this.typedefs = typedefs
        this.services = services
    }

    private constructor(builder: Builder) {
        this.structs = builder.structs
        this.unions = builder.unions
        this.exceptions = builder.exceptions
        this.enums = builder.enums
        this.constants = builder.constants
        this.typedefs = builder.typedefs
        this.services = builder.services
    }

    fun toBuilder(): Builder {
        return Builder(structs, unions, exceptions, enums, constants, typedefs, services)
    }

    class Builder internal constructor(
            internal var structs: List<StructType>,
            internal var unions: List<StructType>,
            internal var exceptions: List<StructType>,
            internal var enums: List<EnumType>,
            internal var constants: List<Constant>,
            internal var typedefs: List<TypedefType>,
            internal var services: List<ServiceType>
    ) {

        fun structs(structs: List<StructType>): Builder = apply {
            this.structs = structs.toList()
        }

        fun unions(unions: List<StructType>): Builder = apply {
            this.unions = unions.toList()
        }

        fun exceptions(exceptions: List<StructType>): Builder = apply {
            this.exceptions = exceptions.toList()
        }

        fun enums(enums: List<EnumType>): Builder = apply {
            this.enums = enums.toList()
        }

        fun constants(constants: List<Constant>): Builder = apply {
            this.constants = constants.toList()
        }

        fun typedefs(typedefs: List<TypedefType>): Builder = apply {
            this.typedefs = typedefs.toList()
        }

        fun services(services: List<ServiceType>): Builder = apply {
            this.services = services.toList()
        }

        fun build(): Schema = Schema(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Schema

        if (structs != other.structs) return false
        if (unions != other.unions) return false
        if (exceptions != other.exceptions) return false
        if (enums != other.enums) return false
        if (constants != other.constants) return false
        if (typedefs != other.typedefs) return false
        if (services != other.services) return false

        return true
    }

    override fun hashCode(): Int {
        var result = structs.hashCode()
        result = 31 * result + unions.hashCode()
        result = 31 * result + exceptions.hashCode()
        result = 31 * result + enums.hashCode()
        result = 31 * result + constants.hashCode()
        result = 31 * result + typedefs.hashCode()
        result = 31 * result + services.hashCode()
        return result
    }
}
