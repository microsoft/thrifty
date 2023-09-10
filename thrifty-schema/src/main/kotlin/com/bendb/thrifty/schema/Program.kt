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

import com.bendb.thrifty.schema.parser.ThriftFileElement

/**
 * A Program is the set of elements declared in a Thrift file.  It
 * contains all types, namespaces, constants, and inclusions defined therein.
 */
class Program internal constructor(element: ThriftFileElement) {

    /**
     * All namespaces defined for this [Program].
     */
    val namespaces: Map<NamespaceScope, String> = element.namespaces
            .map { it.scope to it.namespace }
            .toMap()

    /**
     * All `cpp_include` statements in this [Program].
     */
    val cppIncludes: List<String> = element.includes
            .filter { it.isCpp }
            .map { it.path }

    private val thriftIncludes: List<String> = element.includes
            .filter { !it.isCpp }
            .map { it.path }

    /**
     * All [constants][Constant] contained within this [Program]
     */
    val constants: List<Constant> = element.constants.map { Constant(it, namespaces) }

    /**
     * All [enums][EnumType] contained within this [Program].
     */
    val enums: List<EnumType> = element.enums.map { EnumType(it, namespaces) }

    /**
     * All [structs][StructType] contained within this [Program].
     */
    val structs: List<StructType> = element.structs.map { StructType(it, namespaces) }

    /**
     * All [unions][StructType] contained within this [Program].
     */
    val unions: List<StructType> = element.unions.map { StructType(it, namespaces) }

    /**
     * All [exceptions][StructType] contained within this [Program].
     */
    val exceptions: List<StructType> = element.exceptions.map { StructType(it, namespaces) }

    /**
     * All [typedefs][TypedefType] contained within this [Program].
     */
    val typedefs: List<TypedefType> = element.typedefs.map { TypedefType(it, namespaces) }

    /**
     * All [services][ServiceType] contained within this [Program].
     */
    val services: List<ServiceType> = element.services.map { ServiceType(it, namespaces) }

    /**
     * The location of this [Program], possibly relative (if it was loaded from the search path).
     */
    val location: Location = element.location

    private var includedPrograms: List<Program>? = null
    private var constSymbols: Map<String, Constant>? = null

    /**
     * All other [programs][Program] included by this [Program].
     */
    val includes: List<Program>
        get() = includedPrograms ?: emptyList()

    /**
     * A map of constants in this program indexed by name.
     */
    val constantMap: Map<String, Constant>
        get() = constSymbols ?: emptyMap()

    /**
     * Get all named types declared in this Program.
     *
     * Note that this does not include [constants], which are
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
     * @param visited A [MutableMap] used to track a parent [Program], if it was visited from one.
     * @param parent The parent [Program] that is including this [Program],
     * `null` if this [Program] is not being loaded from another [Program].
     */
    internal fun loadIncludedPrograms(loader: Loader, visited: MutableMap<Program, Program?>, parent: Program?) {
        if (visited.containsKey(this)) {
            if (includedPrograms == null) {
                val includeChain = StringBuilder(this.location.programName);
                var current: Program? = parent
                while (current != null) {
                    includeChain.append(" -> ")
                    includeChain.append(current.location.programName)
                    if (current == this) {
                        break
                    }
                    current = visited[current]
                }
                loader.errorReporter().error(location, "Circular include; file includes itself transitively $includeChain")
                throw IllegalStateException("Circular include: " + location.path
                        + " includes itself transitively " + includeChain)
            }
            return
        }
        visited[this] = parent

        check(this.includedPrograms == null) { "Included programs already resolved" }

        val includes = mutableListOf<Program>()
        for (thriftImport in thriftIncludes) {
            val included = loader.resolveIncludedProgram(location, thriftImport)
            included.loadIncludedPrograms(loader, visited, this)
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

    /** @inheritdoc */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Program) return false

        // Programs are considered equal if they are derived from the same file.
        return location.base == other.location.base && location.path == other.location.path
    }

    /** @inheritdoc */
    override fun hashCode(): Int {
        var result = location.base.hashCode()
        result = 31 * result + location.path.hashCode()
        return result
    }
}
