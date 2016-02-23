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
package com.microsoft.thrifty.schema;

/**
 * Utility that determines whether an instance of one of several types
 * has non-empty Javadoc.
 *
 * If only we had traits - or JDK 8, at the very least.
 */
public final class JavadocUtil {
    private JavadocUtil() {
        // no instances
    }

    static boolean hasJavadoc(Named named) {
        return isNonEmptyJavadoc(named.documentation());
    }

    static boolean hasJavadoc(Field field) {
        return isNonEmptyJavadoc(field.documentation());
    }

    static boolean hasJavadoc(EnumType.Member enumMember) {
        return isNonEmptyJavadoc(enumMember.documentation());
    }

    static boolean hasJavadoc(ServiceMethod method) {
        return isNonEmptyJavadoc(method.documentation());
    }

    public static boolean isNonEmptyJavadoc(String doc) {
        if (doc == null) return false;
        if (doc.isEmpty()) return false;

        for (int i = 0; i < doc.length(); ++i) {
            char c = doc.charAt(i);
            if (!Character.isWhitespace(c)) {
                return true;
            }
        }

        return false;
    }
}
