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
package com.microsoft.thrifty.util

import kotlin.jvm.JvmStatic

/**
 * Utility methods for printing obfuscated versions of sensitive data.
 */
object ObfuscationUtil {
    @JvmStatic
    fun summarizeCollection(collection: Collection<*>?, collectionType: String, elementType: String): String {
        return when (collection) {
            null -> "null"
            else -> "$collectionType<$elementType>(size=${collection.size})"
        }
    }

    @JvmStatic
    fun summarizeMap(map: Map<*, *>?, keyType: String, valueType: String): String {
        return when (map) {
            null -> "null"
            else -> "map<$keyType, $valueType>(size=${map.size})"
        }
    }

    @JvmStatic
    fun hash(value: Any?): String {
        if (value == null) {
            return "null"
        }
        val hashcode = value.hashCode()

        return hashcode.toString(radix = 16).uppercase().padStart(length = 8, padChar = '0')
    }
}
