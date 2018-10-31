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
import okio.ByteString

/**
 * Represents a literal value in a Thrift file for a constant or a field's
 * default value.
 *
 * @property location The location of the text corresponding to this element.
 * @property thriftText The actual Thrift text comprising this const value.
 */
sealed class ConstValueElement {
    abstract val location: Location
    abstract val thriftText: String
}

/**
 * An integer constant value.
 *
 * @property value The value, as a [Long].
 */
data class IntValueElement(
        override val location: Location,
        override val thriftText: String,
        val value: Long
) : ConstValueElement() {
    /** @inheritdoc */
    override fun toString(): String = "$value"
}

/**
 * A floating-point constant value.
 *
 * @property value The value, as a [Double].
 */
data class DoubleValueElement(
        override val location: Location,
        override val thriftText: String,
        val value: Double
) : ConstValueElement() {
    /** @inheritdoc */
    override fun toString(): String = "$value"
}

data class BinaryValueElement(
        override val location: Location,
        override val thriftText: String,
        val value: ByteString
): ConstValueElement() {
    override fun toString(): String = value.toString()
}

/**
 * A string constant.
 *
 * @property value The value, as a [String].
 */
data class LiteralValueElement(
        override val location: Location,
        override val thriftText: String,
        val value: String
) : ConstValueElement() {
    /** @inheritdoc */
    override fun toString(): String = value
}

/**
 * An identifier constant, such as an enum name or a const reference.
 *
 * @property value The identifier, as a [String].
 */
data class IdentifierValueElement(
        override val location: Location,
        override val thriftText: String,
        val value: String
) : ConstValueElement() {
    /** @inheritdoc */
    override fun toString(): String = value
}

/**
 * A list constant.
 *
 * @property value The value, as a [List].
 */
data class ListValueElement(
        override val location: Location,
        override val thriftText: String,
        val value: List<ConstValueElement>
) : ConstValueElement() {
    /** @inheritdoc */
    override fun toString(): String = "$value"
}

/**
 * A map constant.
 *
 * @property value The value, as a [Map].
 */
data class MapValueElement(
        override val location: Location,
        override val thriftText: String,
        val value: Map<ConstValueElement, ConstValueElement>
) : ConstValueElement() {
    /** @inheritdoc */
    override fun toString(): String = "$value"
}