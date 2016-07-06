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
package com.microsoft.thrifty.util;

import java.util.Collection;
import java.util.Map;

/**
 * Utility methods for printing obfuscated versions of sensitive data.
 */
public final class ObfuscationUtil {
    private static final char[] HEX_CHARS =
            { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private ObfuscationUtil() {
        // no instances
    }

    public static String summarizeCollection(Collection<?> collection, String collectionType, String elementType) {
        if (collection == null) {
            return "null";
        }
        return collectionType + "<" + elementType + ">(size=" + collection.size() + ")";
    }

    public static String summarizeMap(Map<?, ?> map, String keyType, String valueType) {
        if (map == null) {
            return "null";
        }
        return "Map<" + keyType + ", " + valueType + ">(size=" + map.size() + ")";
    }

    public static String hash(Object value) {
        int hashcode = 0x9e3779b9;
        if (value != null) {
            hashcode = value.hashCode();
        }

        // Integer.toHexString(int) doesn't pad to 8 chars;
        // this is simple enough to do ourselves.
        char[] hexChars = new char[8];
        hexChars[0] = HEX_CHARS[(hashcode >> 28) & 0xF];
        hexChars[1] = HEX_CHARS[(hashcode >> 24) & 0xF];
        hexChars[2] = HEX_CHARS[(hashcode >> 20) & 0xF];
        hexChars[3] = HEX_CHARS[(hashcode >> 16) & 0xF];
        hexChars[4] = HEX_CHARS[(hashcode >> 12) & 0xF];
        hexChars[5] = HEX_CHARS[(hashcode >>  8) & 0xF];
        hexChars[6] = HEX_CHARS[(hashcode >>  4) & 0xF];
        hexChars[7] = HEX_CHARS[ hashcode        & 0xF];
        return new String(hexChars);
    }
}
