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

import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableMap
import com.microsoft.thrifty.schema.parser.ListTypeElement
import com.microsoft.thrifty.schema.parser.MapTypeElement
import com.microsoft.thrifty.schema.parser.ScalarTypeElement
import com.microsoft.thrifty.schema.parser.SetTypeElement
import com.microsoft.thrifty.schema.parser.TypeElement
import java.io.File
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.LinkedList

internal interface SymbolTable {
    fun lookupConst(symbol: String): Constant?
}

/**
 * An object that can resolve the types of typdefs, struct fields, and service
 * method parameters based on types declared in Thrift [Program]s and their
 * transitive included Programs.
 *
 * In other words, a type-checker.
 */
internal class Linker(
        private val environment: LinkEnvironment,
        private val program: Program,
        private val reporter: ErrorReporter
) : SymbolTable {

    private val typesByName = LinkedHashMap<String, ThriftType>()

    private var linking = false
    private var linked = false

    fun link() {
        if (!Thread.holdsLock(environment)) {
            throw AssertionError("Linking must be locked on the environment!")
        }

        if (linking) {
            reporter.error(program.location, "Circular link detected; file transitively includes itself.")
            return
        }

        if (linked) {
            return
        }

        linking = true

        try {
            linkIncludedPrograms()

            registerDeclaredTypes()

            // Next, figure out what types typedefs are aliasing.
            resolveTypedefs()

            // At this point, all types defined
            linkConstants()
            linkStructFields()
            linkExceptionFields()
            linkUnionFields()
            linkServices()

            // Only validate the schema if linking succeeded; no point otherwise.
            if (!reporter.hasError) {
                validateTypedefs()
                validateConstants()
                validateStructs()
                validateExceptions()
                validateUnions()
                validateServices()
            }

            linked = !environment.hasErrors
        } catch (ignored: LinkFailureException) {
            // The relevant errors will have already been
            // added to the environment; just let the caller
            // handle them.
        } finally {
            linking = false
        }
    }

    private fun linkIncludedPrograms() {
        // First, link included programs and add their resolved types
        // to our own map
        for (p in program.includes) {
            val linker = environment.getLinker(p).also { it.link() }

            val included = File(p.location.base, p.location.path)
            val name = included.name
            val ix = name.indexOf('.')
            if (ix == -1) {
                throw AssertionError(
                        "No extension found for included file " + included.absolutePath + ", "
                                + "invalid include statement")
            }
            val prefix = name.substring(0, ix)

            for ((key, value) in linker.typesByName) {
                // Include types defined directly within the included program,
                // but _not_ qualified names defined in programs that _it_ includes.
                // Include-chains like top.mid.bottom.SomeType are illegal.
                if ('.' !in key) {
                    val qualifiedName = "$prefix.$key"
                    typesByName[qualifiedName] = value
                }
            }
        }

        // Linking included programs may have failed - if so, bail.
        if (environment.hasErrors) {
            throw LinkFailureException()
        }
    }

    private fun registerDeclaredTypes() {
        // Register all types defined in this program
        for (structType in program.structs) {
            register(structType)
        }

        for (union in program.unions) {
            register(union)
        }

        for (exception in program.exceptions) {
            register(exception)
        }

        for (anEnum in program.enums) {
            register(anEnum)
        }

        for (service in program.services) {
            register(service)
        }
    }

    private fun resolveTypedefs() {
        // The strategy for resolving typedefs is:
        // Make a list of typedefs, then loop through it.  If the typedef is
        // successfully linked (i.e. its alias is resolvable), then remove it
        // from the list.  If not, skip it and continue through the list.
        //
        // Keep iterating over the list until it is either empty or contains only unresolvable
        // typedefs.  In the latter case, linking fails.
        // TODO: Surely there must be a more efficient way to do this.

        val typedefs = LinkedList(program.typedefs)
        while (!typedefs.isEmpty()) {
            var atLeastOneResolved = false
            val iter = typedefs.iterator()

            while (iter.hasNext()) {
                val typedef = iter.next()
                try {
                    typedef.link(this)
                    register(typedef)
                    atLeastOneResolved = true
                    iter.remove()
                } catch (ignored: LinkFailureException) {
                }

            }

            if (!atLeastOneResolved) {
                for (typedef in typedefs) {
                    reporter.error(typedef.location, "Unresolvable typedef '" + typedef.name + "'")
                }
                break
            }
        }

        if (environment.hasErrors) {
            throw LinkFailureException()
        }
    }

    private fun linkConstants() {
        for (constant in program.constants) {
            try {
                constant.link(this)
            } catch (e: LinkFailureException) {
                reporter.error(constant.location, "Failed to resolve type '" + e.message + "'")
            }

        }
    }

    private fun linkStructFields() {
        for (structType in program.structs) {
            try {
                structType.link(this)
            } catch (e: LinkFailureException) {
                reporter.error(structType.location, "Failed to resolve type '" + e.message + "'")
            }

        }
    }

    private fun linkUnionFields() {
        for (union in program.unions) {
            try {
                union.link(this)
            } catch (e: LinkFailureException) {
                reporter.error(union.location, "Failed to resolve type " + e.message + "'")
            }

        }
    }

    private fun linkExceptionFields() {
        for (exception in program.exceptions) {
            try {
                exception.link(this)
            } catch (e: LinkFailureException) {
                reporter.error(exception.location, "Failed to resolve type " + e.message + "'")
            }

        }
    }

    private fun linkServices() {
        for (service in program.services) {
            try {
                service.link(this)
            } catch (e: LinkFailureException) {
                reporter.error(service.location, "Failed to resolve type " + e.message + "'")
            }

        }
    }

    private fun validateConstants() {
        for (constant in program.constants) {
            try {
                constant.validate(this)
            } catch (e: IllegalStateException) {
                reporter.error(constant.location, e.message ?: "Error validating constants")
            }

        }
    }

    private fun validateStructs() {
        for (struct in program.structs) {
            struct.validate(this)
        }
    }

    private fun validateExceptions() {
        for (exception in program.exceptions) {
            exception.validate(this)
        }
    }

    private fun validateUnions() {
        for (union in program.unions) {
            union.validate(this)
        }
    }

    private fun validateTypedefs() {
        for (typedef in program.typedefs) {
            typedef.validate(this)
        }
    }

    private fun validateServices() {
        // Services form an inheritance tree
        val visited = LinkedHashSet<ServiceType>(program.services.size)
        val parentToChildren = HashMultimap.create<ServiceType, ServiceType>()
        val servicesToValidate = ArrayDeque<ServiceType>(program.services.size)

        for (service in program.services) {
            // If this service extends another, add the parent -> child relationship to the multmap.
            // Otherwise, this is a root node, and should be added to the processing queue.
            val baseType = service.extendsService
            if (baseType != null) {
                if (baseType.isService) {
                    parentToChildren.put(baseType as ServiceType, service)
                } else {
                    // We know that this is an error condition; queue this type up for validation anyways
                    // so that any other errors lurking here can be reported.
                    servicesToValidate.add(service)
                }
            } else {
                // Root node - add it to the queue
                servicesToValidate.add(service)
            }
        }

        checkForCircularInheritance()

        while (!servicesToValidate.isEmpty()) {
            val service = servicesToValidate.remove()
            if (visited.add(service)) {
                service.validate(this)
                servicesToValidate.addAll(parentToChildren.get(service))
            }
        }
    }

    private fun checkForCircularInheritance() {
        val visited = LinkedHashSet<ThriftType>()
        val stack = ArrayList<ThriftType>()
        val totalVisited = LinkedHashSet<ThriftType>()

        for (svc in program.services) {
            if (totalVisited.contains(svc)) {
                // We've already validated this hierarchy
                continue
            }

            visited.clear()
            stack.clear()
            visited.add(svc)
            stack.add(svc)

            var type: ThriftType? = svc.extendsService
            while (type != null) {
                stack.add(type)
                if (!visited.add(type)) {
                    val sb = StringBuilder("Circular inheritance detected: ")
                    val arrow = " -> "
                    for (t in stack) {
                        sb.append(t.name)
                        sb.append(arrow)
                    }
                    sb.setLength(sb.length - arrow.length)
                    addError(svc.location, sb.toString())
                    break
                }

                if (type !is ServiceType) {
                    // Service extends a non-service type?
                    // This is an error but is reported in
                    // ServiceType#validate(Linker).
                    break
                }

                type = type.extendsService
            }

            totalVisited.addAll(visited)
        }
    }

    private fun register(type: UserType) {
        typesByName[type.name] = type
    }

    open fun resolveType(type: TypeElement): ThriftType {
        val annotationElement = type.annotations
        val annotations = annotationElement?.values ?: ImmutableMap.of()

        typesByName[type.name]?.let {
            // If we are resolving e.g. the type of a field element, the type
            // may carry annotations that are not part of the canonical type.
            return if (annotations.isEmpty()) {
                it
            } else {
                it.withAnnotations(annotations)
            }
        }

        return when (type) {
            is ListTypeElement -> {
                val elementType = resolveType(type.elementType)
                val listType = ListType(elementType)
                typesByName[type.name] = listType
                listType.withAnnotations(annotations)
            }
            is SetTypeElement -> {
                val elementType = resolveType(type.elementType)
                val setType = SetType(elementType)
                typesByName[type.name] = setType
                setType.withAnnotations(annotations)
            }
            is MapTypeElement -> {
                val keyType = resolveType(type.keyType)
                val valueType = resolveType(type.valueType)
                val mapType = MapType(keyType, valueType)
                typesByName[type.name] = mapType
                mapType.withAnnotations(annotations)
            }
            is ScalarTypeElement -> {
                // At this point, all user-defined types should have been registered.
                // If we are resolving a built-in type, then that's fine.  If not, then
                // we have an error.
                val builtinType = BuiltinType.get(type.name)
                        ?: throw LinkFailureException(type.name)

                builtinType.withAnnotations(annotations)
            }
        }
    }

    override fun lookupConst(symbol: String): Constant? {
        var constant = program.constantMap[symbol]
        if (constant == null) {
            // As above, 'symbol' may be a reference to an included
            // constant.
            val ix = symbol.indexOf('.')
            if (ix != -1) {
                val includeName = symbol.substring(0, ix)
                val qualifiedName = symbol.substring(ix + 1)
                val expectedPath = "$includeName.thrift"
                constant = program.includes
                        .filter { p -> p.location.path == expectedPath }
                        .mapNotNull { p -> p.constantMap[qualifiedName] }
                        .firstOrNull()
            }
        }
        return constant
    }

    fun addError(location: Location, error: String) {
        reporter.error(location, error)
    }

    private class LinkFailureException : RuntimeException {
        internal constructor()

        internal constructor(message: String) : super(message) {}
    }
}
