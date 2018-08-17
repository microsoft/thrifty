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
    /**
     * All `struct` entities contained in the parsed .thrift files
     */
    val structs: List<StructType>

    /**
     * All `union` entities contained in the parsed .thrift files
     */
    val unions: List<StructType>

    /**
     * All `exception` entities contained in the parsed .thrift files
     */
    val exceptions: List<StructType>

    /**
     * All `enum` types defined in the parsed .thrift files.
     */
    val enums: List<EnumType>

    /**
     * All `const` elements defined in the parsed .thrift files.
     */
    val constants: List<Constant>

    /**
     * All `typedef` aliases defined in the parsed .thrift files.
     */
    val typedefs: List<TypedefType>

    /**
     * All `service` types defined in the parsed .thrift files.
     */
    val services: List<ServiceType>

    /**
     * @return an [Iterable] of all [UserElements][UserElement] in this [Schema].
     */
    fun elements(): Iterable<UserElement> {
        return structs + unions + exceptions + enums + constants + typedefs + services
    }

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

    /**
     * Returns a [Builder] initialized with this schema's types.
     */
    fun toBuilder(): Builder {
        return Builder(structs, unions, exceptions, enums, constants, typedefs, services)
    }

    /**
     * A builder for [schemas][Schema].
     */
    class Builder internal constructor(
            internal var structs: List<StructType>,
            internal var unions: List<StructType>,
            internal var exceptions: List<StructType>,
            internal var enums: List<EnumType>,
            internal var constants: List<Constant>,
            internal var typedefs: List<TypedefType>,
            internal var services: List<ServiceType>
    ) {
        /**
         * Use the given [structs] for the schema under construction.
         */
        fun structs(structs: List<StructType>): Builder = apply {
            this.structs = structs.toList()
        }

        /**
         * Use the given [unions] for the schema under construction.
         */
        fun unions(unions: List<StructType>): Builder = apply {
            this.unions = unions.toList()
        }

        /**
         * Use the given [exceptions] for the schema under construction.
         */
        fun exceptions(exceptions: List<StructType>): Builder = apply {
            this.exceptions = exceptions.toList()
        }

        /**
         * Use the given [enums] for the schema under construction.
         */
        fun enums(enums: List<EnumType>): Builder = apply {
            this.enums = enums.toList()
        }

        /**
         * Use the given [constants] for the schema under construction.
         */
        fun constants(constants: List<Constant>): Builder = apply {
            this.constants = constants.toList()
        }

        /**
         * Use the given [typedefs] for the schema under construction.
         */
        fun typedefs(typedefs: List<TypedefType>): Builder = apply {
            this.typedefs = typedefs.toList()
        }

        /**
         * Use the given [services] for the schema under construction.
         */
        fun services(services: List<ServiceType>): Builder = apply {
            this.services = services.toList()
        }

        /**
         * Build a new [Schema].
         */
        fun build(): Schema = Schema(this)
    }

    /** @inheritdoc */
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

    /** @inheritdoc */
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
