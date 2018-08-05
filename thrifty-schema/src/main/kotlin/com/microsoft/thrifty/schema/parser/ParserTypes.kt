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
 * @property location The location of the text corresponding to this element.
 * @property values The annotation values
 */
data class AnnotationElement(
        val location: Location,
        val values: Map<String, String>
) {
    /**
     * True if this element contains no annotation values, otherwise false.
     */
    val isEmpty: Boolean
        get() = values.isEmpty()

    /**
     * The number of annotation values in this element.
     */
    val size: Int
        get() = values.size

    /**
     * Gets the value of the given annotation [key], if present.
     */
    operator fun get(key: String): String? = values[key]
}

/**
 * Represents a reference to a named type.
 *
 * Types can be scalar (numbers, strings, identifiers, etc) or collections.
 *
 * @property location The location of the text corresponding to this element.
 * @property name The name of the type referenced by this element.
 * @property annotationClass The annotations associated with this type reference, if any.
 */
sealed class TypeElement {
    abstract val location: Location
    abstract val name: String
    abstract val annotations: AnnotationElement?
}

/**
 * Represents a reference to a scalar type.
 *
 * @constructor Creates a new instance of [ScalarTypeElement].
 * @property location The location of the text corresponding to this element.
 * @property name The name of the type referenced by this element.
 * @property annotations The annotations associated with this element, if any.
 */
data class ScalarTypeElement(
        override val location: Location,
        override val name: String,
        override val annotations: AnnotationElement? = null
) : TypeElement()

/**
 * Represents a reference to a set-type.
 *
 * @constructor Creates a new instance of [SetTypeElement].
 * @property location The location of the text corresponding to this element.
 * @property elementType A reference to the type of element contained within this set type.
 * @property annotations The annotations associated with this element, if any.
 * @property name The name of the type referenced by this element.
 */
data class SetTypeElement(
        override val location: Location,
        val elementType: TypeElement,
        override val annotations: AnnotationElement? = null,
        override val name: String = "set<${elementType.name}>"
) : TypeElement()

/**
 * Represents a reference to a list-type.
 *
 * @constructor Creates a new instance of [ListTypeElement].
 * @property location The location of the text corresponding to this element.
 * @property elementType A reference to the type of element contained within this list type.
 * @property annotations The annotations associated with this element, if any.
 * @property name The name of the type referenced by this element.
 */
data class ListTypeElement(
        override val location: Location,
        val elementType: TypeElement,
        override val annotations: AnnotationElement? = null,
        override val name: String = "list<${elementType.name}>"
) : TypeElement()

/**
 * Represents a reference to a map-type.
 *
 * A map-type is a typical key-value lookup, a.k.a. associative array,
 * dictionary, etc.
 *
 * @constructor Creates a new instance of [MapTypeElement].
 * @property location The location of the text corresponding to this element.
 * @property keyType A reference to the type serving as the key-type of this map type.
 * @property valueType A reference to the type serving as the value-type of this map type.
 * @property annotations The annotations associated with this element, if any.
 * @property name The name of the type referenced by this element.
 */
data class MapTypeElement(
        override val location: Location,
        val keyType: TypeElement,
        val valueType: TypeElement,
        override val annotations: AnnotationElement? = null,
        override val name: String = "map<${keyType.name}, ${valueType.name}>"
) : TypeElement()

/**
 * Represents the inclusion of one Thrift program into another.
 *
 * @constructor Creates a new instance of [IncludeElement].
 * @property location The location of the text corresponding to this element.
 * @property isCpp Indicates whether or not this is a `cpp_include` statement.
 * @property path The path (relative or absolute) of the included program.
 */
data class IncludeElement(
        val location: Location,
        val isCpp: Boolean,
        val path: String
)

/**
 * Represents the declaration of a new name for an existing type.
 *
 * @constructor Creates a new instance of [TypedefElement].
 * @property location The location of the text corresponding to this element.
 * @property oldType The type receiving a new name.
 * @property newName The new name for [oldType].
 * @property documentation The documentation associated with this element, if any.
 * @property uuid A UUID uniquely identifying this element.
 * @property annotations The annotations associated with this element, if any.
 */
data class TypedefElement(
        val location: Location,
        val oldType: TypeElement,
        val newName: String,
        val documentation: String = "",
        val uuid: UUID = ThriftyParserPlugins.createUUID(),
        val annotations: AnnotationElement? = null
)

/**
 * Represents the declaration of a language-specific namespace in a Thrift
 * program.
 *
 * @constructor Creates a new instance of [NamespaceElement].
 *
 * @property location The location of the text corresponding to this element.
 * @property scope The language ("scope") to which this declaration applies.
 * @property namespace The name of the namespace.
 * @property annotations The annotations associated with this element, if any.
 */
data class NamespaceElement(
        val location: Location,
        val scope: NamespaceScope,
        val namespace: String,
        val annotations: AnnotationElement? = null
)

/**
 * Represents the declaration of a named constant value in a Thrift program.
 *
 * @constructor Creates a new instance of [ConstElement].
 * @property location The location of the text corresponding to this element.
 * @property type The type of the constant's value.
 * @property name The name of the constant.
 * @property value The literal value of the constant. This is not guaranteed by the
 *                 parser to conform to the declared [type].
 * @property documentation The documentation associated with this element, if any.
 * @property uuid A UUID uniquely identifying this element.
 */
data class ConstElement(
        val location: Location,
        val type: TypeElement,
        val name: String,
        val value: ConstValueElement,
        val documentation: String = "",
        val uuid: UUID = ThriftyParserPlugins.createUUID()
)

/**
 * Represents a single named member of a Thrift enumeration.
 *
 * @constructor Creates a new instance of [EnumMemberElement].
 * @property location The location of the text corresponding to this element.
 * @property name The name of the enum member.
 * @property value The integral value associated with the enum member.
 * @property documentation The documentation associated with this element, if any.
 * @property uuid A UUID uniquely identifying this element.
 * @property annotations The annotations associated with this element, if any.
 */
data class EnumMemberElement(
        val location: Location,
        val name: String,
        val value: Int,
        val documentation: String = "",
        val annotations: AnnotationElement? = null,
        val uuid: UUID = ThriftyParserPlugins.createUUID()
)

/**
 * Represents a Thrift enumeration.
 *
 * @constructor Creates a new instance of [EnumElement].
 * @property location The location of the text corresponding to this element.
 * @property name The name of the enumeration.
 * @property members The enumeration members.
 * @property documentation The documentation associated with this element, if any.
 * @property uuid A UUID uniquely identifying this element.
 * @property annotations The annotations associated with this element, if any.
 */
data class EnumElement(
        val location: Location,
        val name: String,
        val members: List<EnumMemberElement>,
        val documentation: String = "",
        val annotations: AnnotationElement? = null,
        val uuid: UUID = ThriftyParserPlugins.createUUID()
)

/**
 * Represents a field in a Thrift struct, union, or exception.
 *
 * @constructor Creates a new instance of [FieldElement].
 * @property location The location of the text corresponding to this element.
 * @property fieldId The integer ID of the field.
 * @property type The type of the field.
 * @property name The name of the field.
 * @property requiredness The [Requiredness] of the field.
 * @property documentation The documentation associated with this element, if any.
 * @property constValue The default value of the field, if any.
 * @property annotations The annotations associated with this element, if any.
 * @property uuid A UUID uniquely identifying this element.
 */
data class FieldElement(
        val location: Location,
        val fieldId: Int,
        val type: TypeElement,
        val name: String,
        val requiredness: Requiredness = Requiredness.DEFAULT,
        val documentation: String = "",
        val constValue: ConstValueElement? = null,
        val annotations: AnnotationElement? = null,
        val uuid: UUID = ThriftyParserPlugins.createUUID()
)

/**
 * Represents the definition of a Thrift struct, union, or exception.
 *
 * @constructor Creates a new instance of [StructElement].
 * @property location The location of the text corresponding to this element.
 * @property name The name of the struct, union, or exception.
 * @property type The kind of struct represented by this element: struct, union, or exception.
 * @property fields The fields comprising this struct.
 * @property documentation The documentation associated with this element, if any.
 * @property uuid A UUID uniquely identifying this element.
 * @property annotations The annotations associated with this element, if any.
 */
data class StructElement(
        val location: Location,
        val name: String,
        val type: Type,
        val fields: List<FieldElement>,
        val documentation: String = "",
        val annotations: AnnotationElement? = null,
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
 *
 * A function defined with the `oneway` keyword has no return type, and
 * generated clients will not wait for a response from the remote endpoint
 * when making a one-way RPC call.  One-way function must have a return type
 * of `void` - this is not validated at parse time.
 *
 * @constructor Creates a new instance of [FunctionElement].
 * @property location The location of the text corresponding to this element.
 * @property name The name of the function.
 * @property returnType The return type of the function; may be `void` to indicate no return type.
 * @property params A list, possibly empty, of function parameters.
 * @property exceptions A list, possibly empty, of exceptions thrown by this function.
 * @property documentation The documentation associated with this element, if any.
 * @property oneWay True if the function is `oneway`, otherwise false.
 * @property uuid A UUID uniquely identifying this element.
 * @property annotations The annotations associated with this element, if any.
 */
data class FunctionElement(
        val location: Location,
        val name: String,
        val returnType: TypeElement,
        val params: List<FieldElement> = emptyList(),
        val exceptions: List<FieldElement> = emptyList(),
        val oneWay: Boolean = false,
        val documentation: String = "",
        val annotations: AnnotationElement? = null,
        val uuid: UUID = ThriftyParserPlugins.createUUID()
)

/**
 * Represents the declaration of a Thrift service.
 *
 * A service is an entity having zero or more functions that can be invoked by
 * remote clients.  Services can inherit the definition of other services, very
 * much like inheritance in an object-oriented language.  The base type element
 * here is presumed to identify another service, but this is not validated at
 * parse time.
 *
 * @constructor Creates a new instance of [ServiceElement].
 * @property location The location of the text corresponding to this element.
 * @property name The name of the service.
 * @property functions A list, possibly empty, of functions defined by this service.
 * @property extendsService The base type, if any, of this service.
 * @property documentation The documentation associated with this element, if any.
 * @property uuid A UUID uniquely identifying this element.
 * @property annotations The annotations associated with this element, if any.
 */
data class ServiceElement(
        val location: Location,
        val name: String,
        val functions: List<FunctionElement> = emptyList(),
        val extendsService: TypeElement? = null,
        val documentation: String = "",
        val annotations: AnnotationElement? = null,
        val uuid: UUID = ThriftyParserPlugins.createUUID()
)

/**
 * Represents a Thrift file, and everything defined within it.
 *
 * @constructor Creates a new instance of [ThriftFileElement].
 * @property location The location of the text corresponding to this element.
 * @property namespaces The list of all namespaces defined within the file.
 * @property includes The list of all other thrift files included by this file.
 * @property constants The list of all constants defined within the file.
 * @property typedefs The list of all typedefs defined within the file.
 * @property enums The list of all enums defined within the file.
 * @property structs The list of all structs defined within the file.
 * @property unions The list of all unions defined within the file.
 * @property exceptions The list of all exceptions defined within the file.
 * @property services The list of all services defined within the file.
 */
data class ThriftFileElement(
        val location: Location,
        val namespaces: List<NamespaceElement> = emptyList(),
        val includes: List<IncludeElement> = emptyList(),
        val constants: List<ConstElement> = emptyList(),
        val typedefs: List<TypedefElement> = emptyList(),
        val enums: List<EnumElement> = emptyList(),
        val structs: List<StructElement> = emptyList(),
        val unions: List<StructElement> = emptyList(),
        val exceptions: List<StructElement> = emptyList(),
        val services: List<ServiceElement> = emptyList()
)