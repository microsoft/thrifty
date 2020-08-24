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

/**
 * Utility methods for printing obfuscated versions of sensitive data.
 */
object ObfuscationUtil {
    private val HEX_CHARS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

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

        // Integer.toHexString(int) doesn't pad to 8 chars;
        // this is simple enough to do ourselves.
        val hexChars = CharArray(8)
        hexChars[0] = HEX_CHARS[hashcode shr 28 and 0xF]
        hexChars[1] = HEX_CHARS[hashcode shr 24 and 0xF]
        hexChars[2] = HEX_CHARS[hashcode shr 20 and 0xF]
        hexChars[3] = HEX_CHARS[hashcode shr 16 and 0xF]
        hexChars[4] = HEX_CHARS[hashcode shr 12 and 0xF]
        hexChars[5] = HEX_CHARS[hashcode shr 8 and 0xF]
        hexChars[6] = HEX_CHARS[hashcode shr 4 and 0xF]
        hexChars[7] = HEX_CHARS[hashcode and 0xF]
        return String(hexChars)
    }
}