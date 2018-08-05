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

/**
 * Represents a reference to a named type.
 *
 * Types can be scalar (numbers, strings, identifiers, etc) or collections.
 */
sealed class TypeElement(
        /**
         * The location of the text corresponding to this element.
         */
        val location: Location,

        /**
         * The name of the type referenced by this element.
         */
        val name: String,

        /**
         * The annotations associated with this type reference, if any.
         */
        val annotations: AnnotationElement? = null
) {

    companion object {
        /**
         * Creates and returns a scalar [TypeElement].
         *
         * @param location the location of the text corresponding to the scalar.
         * @param name the name of the scalar-type reference.
         * @param annotations the annotations associated with the reference, if any.
         * @return a [ScalarTypeElement].
         */
        fun scalar(location: Location, name: String, annotations: AnnotationElement? = null) =
                ScalarTypeElement(location, name, annotations)

        /**
         * Creates and returns a list [TypeElement].
         *
         * @param location the location of the text corresponding to the reference.
         * @param elementType the type of element contained by the list type.
         * @param annotations the annotations associated with the reference, if any.
         * @return a [ListTypeElement].
         */
        fun list(location: Location, elementType: TypeElement, annotations: AnnotationElement? = null) =
                ListTypeElement(elementType, location, annotations)

        /**
         * Creates and returns a set [TypeElement].
         *
         * @param location the location of the text corresponding to the reference.
         * @param elementType the type of element contained by the set type.
         * @param annotations the annotations associated with the reference, if any.
         * @return a [SetTypeElement].
         */
        fun set(location: Location, elementType: TypeElement, annotations: AnnotationElement? = null) =
                SetTypeElement(elementType, location, annotations)

        /**
         * Creates and returns a map [TypeElement].
         *
         * @param location the location of the text corresponding to the reference.
         * @param keyType the ley-type of the map.
         * @param valueType the value-type of the map.
         * @param annotations the annotations associated with the reference, if any.
         * @return a [MapTypeElement].
         */
        fun map(location: Location,
                keyType: TypeElement,
                valueType: TypeElement,
                annotations: AnnotationElement? = null) = MapTypeElement(keyType, valueType, location, annotations)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TypeElement

        if (location != other.location) return false
        if (name != other.name) return false
        if (annotations != other.annotations) return false

        return true
    }

    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (annotations?.hashCode() ?: 0)
        return result
    }
}

/**
 * Represents a reference to a scalar type.
 */
class ScalarTypeElement(
        location: Location,
        name: String,
        annotations: AnnotationElement?
) : TypeElement(location, name, annotations) {

    override fun toString(): String {
        return "ScalarTypeElement{location=$location, name=$name, annotations=$annotations}"
    }
}

/**
 * Represents a reference to a set-type.
 */
class SetTypeElement(
        /**
         * A reference to the type of element contained within this set type.
         */
        val elementType: TypeElement,
        location: Location,
        annotations: AnnotationElement? = null
) : TypeElement(location, "set<${elementType.name}>", annotations) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SetTypeElement) return false
        if (!super.equals(other)) return false

        if (elementType != other.elementType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + elementType.hashCode()
        return result
    }

    override fun toString(): String {
        return "SetTypeElement{location=$location, name=$name, annotations=$annotations, elementType=$elementType}"
    }
}

/**
 * Represents a reference to a list-type.
 */
class ListTypeElement(
        /**
         * A reference to the type of element contained within this list type.
         */
        val elementType: TypeElement,
        location: Location,
        annotations: AnnotationElement? = null
) : TypeElement(location, "list<${elementType.name}>", annotations) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ListTypeElement) return false
        if (!super.equals(other)) return false

        if (elementType != other.elementType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + elementType.hashCode()
        return result
    }

    override fun toString(): String {
        return "ListTypeElement{location=$location, name=$name, annotations=$annotations, elementType=$elementType}"
    }
}

/**
 * Represents a reference to a map-type.
 *
 * A map-type is a typical key-value lookup, a.k.a. associative array,
 * dictionary, etc.
 */
class MapTypeElement(
        /**
         * A reference to the type serving as the key-type of this map type.
         */
        val keyType: TypeElement,

        /**
         * A reference to the type serving as the value-type of this map type.
         */
        val valueType: TypeElement,

        location: Location,
        annotations: AnnotationElement? = null
) : TypeElement(location, "map<${keyType.name}, ${valueType.name}>", annotations) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapTypeElement) return false
        if (!super.equals(other)) return false

        if (keyType != other.keyType) return false
        if (valueType != other.valueType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + keyType.hashCode()
        result = 31 * result + valueType.hashCode()
        return result
    }

    override fun toString(): String {
        return "MapTypeElement{location=$location, name=$name, annotations=$annotations, keyType=$keyType, valueType=$valueType}"
    }
}