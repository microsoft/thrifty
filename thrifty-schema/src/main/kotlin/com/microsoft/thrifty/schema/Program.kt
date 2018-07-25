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

import com.microsoft.thrifty.schema.parser.ThriftFileElement

/**
 * A Program is the set of elements declared in a Thrift file.  It
 * contains all types, namespaces, constants, and inclusions defined therein.
 */
class Program internal constructor(element: ThriftFileElement) {

    @get:JvmName("namespaces")
    val namespaces: Map<NamespaceScope, String> = element.namespaces
            .map { it.scope to it.namespace }
            .toMap()


    @get:JvmName("cppIncludes")
    val cppIncludes: List<String> = element.includes
            .filter { it.isCpp }
            .map { it.path }

    private val thriftIncludes: List<String> = element.includes
            .filter { !it.isCpp }
            .map { it.path }

    @get:JvmName("constants")
    val constants: List<Constant>    = element.constants.map { Constant(it, namespaces) }

    @get:JvmName("enums")
    val enums: List<EnumType> = element.enums.map { EnumType(this, it) }

    @get:JvmName("structs")
    val structs: List<StructType>    = element.structs.map { StructType(this, it) }

    @get:JvmName("unions")
    val unions: List<StructType>     = element.unions.map { StructType(this, it) }

    @get:JvmName("exceptions")
    val exceptions: List<StructType> = element.exceptions.map { StructType(this, it) }

    @get:JvmName("typedefs")
    val typedefs: List<TypedefType>  = element.typedefs.map { TypedefType(this, it) }

    @get:JvmName("services")
    val services: List<ServiceType>  = element.services.map { ServiceType(this, it) }

    @get:JvmName("location")
    val location: Location = element.location

    private var includedPrograms: List<Program>? = null
    private var symbols: Map<String, UserType>? = null
    private var constSymbols: Map<String, Constant>? = null

    fun includes(): List<Program>? {
        return this.includedPrograms
    }

    fun symbols(): Map<String, UserType>? {
        return this.symbols
    }

    fun constantMap(): Map<String, Constant>? {
        return this.constSymbols
    }

    /**
     * Get all named types declared in this Program.
     *
     * Note that this does not include [.constants], which are
     * not types.
     *
     * @return all user-defined types contained in this Program.
     */
    fun allUserTypes(): Iterable<UserType> {
        return listOf(enums, structs, unions, exceptions, services, typedefs)
                .flatMapTo(mutableListOf()) { it }
    }

    /**
     * Loads this program's symbol table and list of included Programs.
     * @param loader
     * @param visited
     */
    // TODO: Make this internal after refactoring to Kotlin is complete
    fun loadIncludedPrograms(loader: Loader, visited: MutableSet<Program>) {
        if (!visited.add(this)) {
            if (includedPrograms == null) {
                loader.errorReporter().error(location, "Circular include; file includes itself transitively")
                throw IllegalStateException("Circular include: " + location.path
                        + " includes itself transitively")
            }
            return
        }

        check(this.includedPrograms == null) { "Included programs already resolved" }

        val includes = mutableListOf<Program>()
        for (thriftImport in thriftIncludes) {
            val included = loader.resolveIncludedProgram(location, thriftImport)
            included.loadIncludedPrograms(loader, visited)
            includes.add(included)
        }

        this.includedPrograms = includes

        val symbolMap = mutableMapOf<String, UserType>()
        for (userType in allUserTypes()) {
            val oldValue = symbolMap.put(userType.name, userType)
            if (oldValue != null) {
                reportDuplicateSymbol(loader.errorReporter(), oldValue, userType)
            }
        }

        this.symbols = symbolMap

        val constSymbolMap = mutableMapOf<String, Constant>()
        for (constant in constants) {
            val oldValue = constSymbolMap.put(constant.name, constant)
            if (oldValue != null) {
                reportDuplicateSymbol(loader.errorReporter(), oldValue, constant)
            }
        }

        this.constSymbols = constSymbolMap
    }

    private fun reportDuplicateSymbol(
            reporter: ErrorReporter,
            oldValue: UserElement,
            newValue: UserElement) {
        val message = "Duplicate symbols: ${oldValue.name} defined at ${oldValue.location} and at ${newValue.location}"
        reporter.error(newValue.location, message)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Program) return false

        // Programs are considered equal if they are derived from the same file.
        return location.base == other.location.base && location.path == other.location.path
    }

    override fun hashCode(): Int {
        var result = location.base.hashCode()
        result = 31 * result + location.path.hashCode()
        return result
    }
}
