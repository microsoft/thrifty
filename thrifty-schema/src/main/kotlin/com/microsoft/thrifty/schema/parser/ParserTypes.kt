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
package com.microsoft.thrifty.schema.parser

import com.microsoft.thrifty.schema.Location
import com.microsoft.thrifty.schema.NamespaceScope
import com.microsoft.thrifty.schema.Requiredness
import java.util.UUID

/**
 * Represents an instance of one or more Thrift annotations.
 *
 * @constructor Creates a new instance of [AnnotationElement].
 */
data class AnnotationElement(
        /**
         * The location of the text corresponding to this element.
         */
        @get:JvmName("location")
        val location: Location,

        /**
         * The annotation values.
         */
        @get:JvmName("values")
        val values: Map<String, String>
) {
    /**
     * True if this element contains no annotation values, otherwise false.
     */
    @get:JvmName("isEmpty")
    val isEmpty: Boolean
        get() = values.isEmpty()

    /**
     * The number of annotation values in this element.
     */
    @get:JvmName("size")
    val size: Int
        get() = values.size

    /**
     * Gets the value of the given annotation [key], if present.
     */
    operator fun get(key: String): String? = values[key]
}

/**
 * Represents the inclusion of one Thrift program into another.
 */
data class IncludeElement(
        /**
         * The location of the text corresponding to this element.
         */
        @get:JvmName("location")
        val location: Location,

        /**
         * Indicates whether or not this is a `cpp_include` statement.
         */
        @get:JvmName("isCpp")
        val isCpp: Boolean,

        /**
         * The path (relative or absolute) of the included program.
         */
        @get:JvmName("path")
        val path: String
)

/**
 * Represents the declaration of a new name for an existing type.
 */
data class TypedefElement @JvmOverloads constructor(
        /**
         * The location of the text corresponding to this element.
         */
        @get:JvmName("location")
        val location: Location,

        /**
         * The type receiving a new name.
         */
        @get:JvmName("oldType")
        val oldType: TypeElement,

        /**
         * The new name for [oldType].
         */
        @get:JvmName("newName")
        val newName: String,

        /**
         * The documentation associated with this element, if any.
         */
        @get:JvmName("documentation")
        val documentation: String = "",

        /**
         * A UUID uniquely identifying this element.
         */
        @get:JvmName("uuid")
        val uuid: UUID = ThriftyParserPlugins.createUUID(),

        /**
         * The annotations associated with this element, if any.
         */
        @get:JvmName("annotations")
        val annotations: AnnotationElement? = null
)

/**
 * Represents the declaration of a language-specific namespace in a Thrift
 * program.
 */
data class NamespaceElement @JvmOverloads constructor(
        /**
         * The location of the text corresponding to this element.
         */
        @get:JvmName("location")
        val location: Location,

        /**
         * The language ("scope") to which this declaration applies.
         */
        @get:JvmName("scope")
        val scope: NamespaceScope,

        /**
         * The name of the namespace.
         */
        @get:JvmName("namespace")
        val namespace: String,

        /**
         * The annotations associated with this element, if any.
         */
        @get:JvmName("annotations")
        val annotations: AnnotationElement? = null
)

/**
 * Represents the declaration of a named constant value in a Thrift program.
 */
data class ConstElement @JvmOverloads constructor(
        /**
         * The location of the text corresponding to this element.
         */
        @get:JvmName("location")
        val location: Location,

        /**
         * The type of the constant's value.
         */
        @get:JvmName("type")
        val type: TypeElement,

        /**
         * The name of the constant.
         */
        @get:JvmName("name")
        val name: String,

        /**
         * The literal value of the constant.
         *
         * This is not guaranteed by the parser to conform to the declared [type].
         */
        @get:JvmName("value")
        val value: ConstValueElement,

        /**
         * The documentation associated with this element, if any.
         */
        @get:JvmName("documentation")
        val documentation: String = "",

        /**
         * A UUID uniquely identifying this element.
         */
        @get:JvmName("uuid")
        val uuid: UUID = ThriftyParserPlugins.createUUID()
)

/**
 * Represents a single named member of a Thrift enumeration.
 */
data class EnumMemberElement @JvmOverloads constructor(
        /**
         * The location of the text corresponding to this element.
         */
        @get:JvmName("location")
        val location: Location,

        /**
         * The name of the enum member.
         */
        @get:JvmName("name")
        val name: String,

        /**
         * The integral value associated with the enum member.
         */
        @get:JvmName("value")
        val value: Int,

        /**
         * The documentation associated with this element, if any.
         */
        @get:JvmName("documentation")
        val documentation: String = "",

        /**
         * The annotations associated with the enum member, if any.
         */
        @get:JvmName("annotations")
        val annotations: AnnotationElement? = null,

        /**
         * A UUID uniquely identifying this element.
         */
        @get:JvmName("uuid")
        val uuid: UUID = ThriftyParserPlugins.createUUID()
)

/**
 * Represents a Thrift enumeration.
 */
data class EnumElement @JvmOverloads constructor(
        /**
         * The location of the text corresponding to this element.
         */
        @get:JvmName("location")
        val location: Location,

        /**
         * The name of the enumeration.
         */
        @get:JvmName("name")
        val name: String,

        /**
         * The members comprising this enumeration.
         */
        @get:JvmName("members")
        val members: List<EnumMemberElement>,

        /**
         * The documentation associated with this enumeration, if any.
         */
        @get:JvmName("documentation")
        val documentation: String = "",

        /**
         * The annotations associated with this element, if any.
         */
        @get:JvmName("annotations")
        val annotations: AnnotationElement? = null,

        /**
         * A UUID uniquely identifying this element.
         */
        @get:JvmName("uuid")
        val uuid: UUID = ThriftyParserPlugins.createUUID()
)

/**
 * Represents a field in a Thrift struct, union, or exception.
 */
data class FieldElement @JvmOverloads constructor(
        /**
         * The location of the text corresponding to this element.
         */
        @get:JvmName("location")
        val location: Location,

        /**
         * The integer ID of the field.
         */
        @get:JvmName("fieldId")
        val fieldId: Int,

        /**
         * The type of the field.
         */
        @get:JvmName("type")
        val type: TypeElement,

        /**
         * The name of the field.
         */
        @get:JvmName("name")
        val name: String,

        /**
         * The [Requiredness] of the field.
         */
        @get:JvmName("requiredness")
        val requiredness: Requiredness = Requiredness.DEFAULT,

        /**
         * The documentation associated with the field, if any.
         */
        @get:JvmName("documentation")
        val documentation: String = "",

        /**
         * The default value of the field, if any.
         */
        @get:JvmName("constValue")
        val constValue: ConstValueElement? = null,

        /**
         * The annotations associated with the field, if any.
         */
        @get:JvmName("annotations")
        val annotations: AnnotationElement? = null,

        /**
         * A UUID uniquely identifying this element.
         */
        @get:JvmName("uuid")
        val uuid: UUID = ThriftyParserPlugins.createUUID()
)

/**
 * Represents the definition of a Thrift struct, union, or exception.
 */
data class StructElement @JvmOverloads constructor(
        /**
         * The location of the text corresponding to this element.
         */
        @get:JvmName("location")
        val location: Location,

        /**
         * The name of the struct, union, or exception.
         */
        @get:JvmName("name")
        val name: String,

        /**
         * The kind of struct represented by this element: struct, union, or exception.
         */
        @get:JvmName("type")
        val type: Type,

        /**
         * The fields comprising this struct.
         */
        @get:JvmName("fields")
        val fields: List<FieldElement>,

        /**
         * The documentation associated with this struct, if any.
         */
        @get:JvmName("documentation")
        val documentation: String = "",

        /**
         * The annotations associated with this element, if any.
         */
        @get:JvmName("annotations")
        val annotations: AnnotationElement? = null,

        /**
         * A UUID uniquely identifying this element.
         */
        @get:JvmName("uuid")
        val uuid: UUID = ThriftyParserPlugins.createUUID()
) {
    /**
     * Defines the different types of structured element in the Thrift language.
     */
    enum class Type {
        /**
         * A struct, in the C sense of the term.  That is, an ordered set of
         * named fields having heterogeneous types.
         */
        STRUCT,

        /**
         * A union, also in the C sense of the term.  A set of named fields, of
         * which at most one may have a value at the same time.
         */
        UNION,

        /**
         * An exception is like a [STRUCT], except that it serves as an error
         * type that communicates failure from an RPC call.  Declared as part
         * of a [FunctionElement].
         */
        EXCEPTION
    }
}

/**
 * Represents an RPC function declaration.
 *
 * Functions are always declared within a [ServiceElement].
 */
data class FunctionElement @JvmOverloads constructor(
        /**
         * The location of the text corresponding to this element.
         */
        @get:JvmName("location")
        val location: Location,

        /**
         * The name of the function.
         */
        @get:JvmName("name")
        val name: String,

        /**
         * The return type of the function.  May be `void` to indicate no
         * return type.
         */
        @get:JvmName("returnType")
        val returnType: TypeElement,

        /**
         * A list, possibly empty, of function parameters.
         */
        @get:JvmName("params")
        val params: List<FieldElement> = emptyList(),

        /**
         * A list, possibly empty, of exceptions thrown by this function.
         */
        @get:JvmName("exceptions")
        val exceptions: List<FieldElement> = emptyList(),

        /**
         * True if the function is `oneway`, otherwise false.
         *
         * A function declared with the `oneway` keyword has no return type,
         * and generated service clients will not wait for a response from
         * the remote endpoint when making a one-way RPC call.
         *
         * One-way functions must have a return type of `void`; this is not
         * validated at parse time.
         */
        @get:JvmName("oneWay")
        val oneWay: Boolean = false,

        /**
         * The documentation associated with this function, if any.
         */
        @get:JvmName("documentation")
        val documentation: String = "",

        /**
         * The annotations associated with this function, if any.
         */
        @get:JvmName("annotations")
        val annotations: AnnotationElement? = null,

        /**
         * A UUID uniquely identifying this element.
         */
        @get:JvmName("uuid")
        val uuid: UUID = ThriftyParserPlugins.createUUID()
)

/**
 * Represents the declaration of a Thrift service.
 *
 * A service is an entity having zero or more functions that can be invoked by
 * remote clients.  Services can inherit the definition of other services, very
 * much like inheritance in an object-oriented language.
 */
data class ServiceElement @JvmOverloads constructor(
        /**
         * The location of the text corresponding to this element.
         */
        @get:JvmName("location")
        val location: Location,

        /**
         * The name of the service.
         */
        @get:JvmName("name")
        val name: String,

        /**
         * The list, possibly empty, of functions defined by this service.
         */
        @get:JvmName("functions")
        val functions: List<FunctionElement> = emptyList(),

        /**
         * The base type, if any, of this service.
         *
         * A base type is presumed to refer to another service, but that is not
         * enforced by the parser.
         */
        @get:JvmName("extendsService")
        val extendsService: TypeElement? = null,

        /**
         * The documentation associated with this service, if any.
         */
        @get:JvmName("documentation")
        val documentation: String = "",

        /**
         * The annotations associated with this service, if any.
         */
        @get:JvmName("annotations")
        val annotations: AnnotationElement? = null,

        /**
         * A UUID uniquely identifying this element.
         */
        @get:JvmName("uuid")
        val uuid: UUID = ThriftyParserPlugins.createUUID()
)

/**
 * Represents a Thrift file, and everything defined within it.
 */
data class ThriftFileElement(
        /**
         * The location of the text corresponding to this element.
         */
        @get:JvmName("location")
        val location: Location,

        /**
         * The list of all namespaces defined within this file.
         */
        @get:JvmName("namespaces")
        val namespaces: List<NamespaceElement> = emptyList(),

        /**
         * The list of all other thrift files included by this file.
         */
        @get:JvmName("includes")
        val includes: List<IncludeElement> = emptyList(),

        /**
         * The list of all constants defined within this file.
         */
        @get:JvmName("constants")
        val constants: List<ConstElement> = emptyList(),

        /**
         * The list of all typedefs defined within this file.
         */
        @get:JvmName("typedefs")
        val typedefs: List<TypedefElement> = emptyList(),

        /**
         * The list of all enums defined within this file.
         */
        @get:JvmName("enums")
        val enums: List<EnumElement> = emptyList(),

        /**
         * The list of all structs defined within this file.
         */
        @get:JvmName("structs")
        val structs: List<StructElement> = emptyList(),

        /**
         * The list of all unions defined within this file.
         */
        @get:JvmName("unions")
        val unions: List<StructElement> = emptyList(),

        /**
         * The list of all exceptions defined within this file.
         */
        @get:JvmName("exceptions")
        val exceptions: List<StructElement> = emptyList(),

        /**
         * The list of all services defined within this file.
         */
        @get:JvmName("services")
        val services: List<ServiceElement> = emptyList()
) {
    class Builder(private val location: Location) {
        private var namespaces = emptyList<NamespaceElement>()
        private var includes = emptyList<IncludeElement>()
        private var constants = emptyList<ConstElement>()
        private var typedefs = emptyList<TypedefElement>()
        private var enums = emptyList<EnumElement>()
        private var structs = emptyList<StructElement>()
        private var unions = emptyList<StructElement>()
        private var exceptions = emptyList<StructElement>()
        private var services: List<ServiceElement> = emptyList()

        fun namespaces(namespaces: List<NamespaceElement>) = apply {
            this.namespaces = namespaces
        }

        fun includes(includes: List<IncludeElement>) = apply {
            this.includes = includes
        }

        fun constants(constants: List<ConstElement>) = apply {
            this.constants = constants
        }

        fun typedefs(typedefs: List<TypedefElement>) = apply {
            this.typedefs = typedefs
        }

        fun enums(enums: List<EnumElement>) = apply {
            this.enums = enums
        }

        fun structs(structs: List<StructElement>) = apply {
            this.structs = structs
        }

        fun unions(unions: List<StructElement>) = apply {
            this.unions = unions
        }

        fun exceptions(exceptions: List<StructElement>) = apply {
            this.exceptions = exceptions
        }

        fun services(services: List<ServiceElement>) = apply {
            this.services = services
        }

        fun build(): ThriftFileElement {
            return ThriftFileElement(
                    location = location,
                    namespaces = namespaces,
                    includes = includes,
                    constants = constants,
                    typedefs = typedefs,
                    enums = enums,
                    structs = structs,
                    unions = unions,
                    exceptions = exceptions,
                    services = services
            )
        }
    }

    companion object {
        @JvmStatic fun builder(location: Location) = Builder(location)
    }
}