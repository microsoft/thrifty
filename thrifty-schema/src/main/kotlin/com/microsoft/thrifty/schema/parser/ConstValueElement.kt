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
 * Represents a literal value in a Thrift file for a constant or a field's
 * default value.
 *
 * @property location The location of the text corresponding to this element.
 * @property kind The kind of constant value this is - integer, real, list, identifier, etc.
 * @property thriftText The actual Thrift text comprising this const value.
 * @property value The parsed value itself.  Will be of a type dictated by the value's [kind].
 */
// TODO: Convert this to a sealed class
data class ConstValueElement(
        val location: Location,
        val kind: Kind,
        val thriftText: String,
        val value: Any
) {
    val isInt: Boolean
        get() = kind == Kind.INTEGER

    val isDouble: Boolean
        get() = kind == Kind.DOUBLE

    val isString: Boolean
        get() = kind == Kind.STRING

    val isIdentifier: Boolean
        get() = kind == Kind.IDENTIFIER

    val isList: Boolean
        get() = kind == Kind.LIST

    val isMap: Boolean
        get() = kind == Kind.MAP

    fun getAsInt(): Int {
        check(isInt) { "Cannot convert to int; kind=$kind" }
        return (value as Long).toInt()
    }

    fun getAsLong(): Long {
        check(isInt) { "Cannot convert to long; kind=$kind" }
        return value as Long
    }

    fun getAsDouble(): Double {
        check(isInt || isDouble) { "Cannot convert to double; kind=$kind" }
        return when  {
            isDouble -> value as Double
            isInt -> getAsInt().toDouble()
            else -> error("unpossible")
        }
    }

    fun getAsString(): String {
        check(isString || isIdentifier) { "Cannot convert to string; kind=$kind" }
        return value as String
    }

    @Suppress("UNCHECKED_CAST")
    fun getAsList(): List<ConstValueElement> {
        check(isList) { "Cannot convert to list; kind=$kind"}
        return value as List<ConstValueElement>
    }

    @Suppress("UNCHECKED_CAST")
    fun getAsMap(): Map<ConstValueElement, ConstValueElement> {
        check(isMap) { "Cannot convert to map; kind=$kind" }
        return value as Map<ConstValueElement, ConstValueElement>
    }

    /**
     * Defines the kinds of values representable as a [ConstValueElement].
     */
    enum class Kind {
        /**
         * Ye Olde Integer.
         *
         * Will actually be a [Long], at runtime.
         */
        INTEGER,

        /**
         * A 64-it floating-point number.
         */
        DOUBLE,

        /**
         * A quoted string.
         */
        STRING,

        /**
         * An unquoted string, naming some other Thrift entity.
         */
        IDENTIFIER,

        /**
         * A list of [ConstValueElements][ConstValueElement]
         *
         * It is assumed that all values in the list share the same type; this
         * is not enforced by the parser.
         */
        LIST,

        /**
         * An key-value mapping of [ConstValueElements][ConstValueElement].
         *
         * It is assumed that all keys share the same type, and that all values
         * also share the same type.  This is not enforced by the parser.
         */
        MAP,
    }

    companion object {
        fun integer(location: Location, text: String, value: Long): ConstValueElement {
            return ConstValueElement(location, Kind.INTEGER, text, value)
        }

        fun real(location: Location, text: String, value: Double): ConstValueElement {
            return ConstValueElement(location, Kind.DOUBLE, text, value)
        }

        fun literal(location: Location, text: String, value: String): ConstValueElement {
            return ConstValueElement(location, Kind.STRING, text, value)
        }

        fun identifier(location: Location, text: String, value: String): ConstValueElement {
            return ConstValueElement(location, Kind.IDENTIFIER, text, value)
        }

        fun list(location: Location, text: String, value: List<ConstValueElement>): ConstValueElement {
            return ConstValueElement(location, Kind.LIST, text, value.toList())
        }

        fun map(location: Location, text: String, value: Map<ConstValueElement, ConstValueElement>): ConstValueElement {
            return ConstValueElement(location, Kind.MAP, text, value.toMap())
        }
    }
}