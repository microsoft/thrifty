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

import  com.google.common.base.CaseFormat
import java.util.regex.Pattern

/**
 * Controls the style of names generated for fields.
 */
abstract class FieldNamingPolicy {

    /**
     * Apply this policy to the given [name].
     */
    abstract fun apply(name: String): String

    companion object {
        private val LOWER_CAMEL_REGEX = Pattern.compile("([a-z]+[A-Z]+\\w+)+")
        private val UPPER_CAMEL_REGEX = Pattern.compile("([A-Z]+[a-z]+\\w+)+")

        /**
         * The default policy is to leave names unaltered from their definition in Thrift IDL.
         */
        val DEFAULT: FieldNamingPolicy = object : FieldNamingPolicy() {
            override fun apply(name: String): String {
                return name
            }
        }

        /**
         * The Java policy generates camelCase names, unless the initial part of the field name
         * appears to be an acronym, in which case the casing is preserved.
         *
         * "Acronym" here is defined to be two or more consecutive upper-case characters
         * at the beginning of the name.  Thus, this policy will preserve `.SSLFlag` over
         * `.sSLFlag`.
         */
        val JAVA: FieldNamingPolicy = object : FieldNamingPolicy() {
            override fun apply(name: String): String {
                val caseFormat = caseFormatOf(name)
                if (caseFormat != null) {
                    val formattedName = caseFormat.to(CaseFormat.LOWER_CAMEL, name)
                    // Handle acronym as camel case made it lower case.
                    return if (name.length > 1
                            && formattedName.length > 1
                            && Character.isUpperCase(name[0])
                            && Character.isUpperCase(name[1])
                            && caseFormat !== CaseFormat.UPPER_UNDERSCORE) {
                        name[0] + formattedName.substring(1)
                    } else {
                        formattedName
                    }
                }

                // Unknown case format. Handle the acronym.
                if (Character.isUpperCase(name[0])) {
                    if (name.length == 1 || !Character.isUpperCase(name[1])) {
                        return Character.toLowerCase(name[0]) + name.substring(1)
                    }
                }
                return name
            }
        }

        /**
         * The Pascal-case policy generates PascalCase names.
         */
        val PASCAL: FieldNamingPolicy = object : FieldNamingPolicy() {
            override fun apply(name: String): String {
                val caseFormat = caseFormatOf(name)
                if (caseFormat != null) {
                    return caseFormat.to(CaseFormat.UPPER_CAMEL, name)
                }

                // Unknown format.  We'll bulldoze the name by uppercasing the
                // first char, then just removing any subsequent non-identifier chars.
                return buildString {
                    append(Character.toUpperCase(name[0]))
                    name.substring(1)
                            .filter { it.isJavaIdentifierPart() }
                            .forEach { append(it) }
                }
            }
        }

        /**
         * Find case format from string.
         * @param s the input String
         * @return CaseFormat the case format of the string.
         */
        private fun caseFormatOf(s: String): CaseFormat? {
            if (s.contains("_")) {
                if (s.toUpperCase() == s) {
                    return CaseFormat.UPPER_UNDERSCORE
                }

                if (s.toLowerCase() == s) {
                    return CaseFormat.LOWER_UNDERSCORE
                }
            } else if (s.contains("-")) {
                if (s.toLowerCase() == s) {
                    return CaseFormat.LOWER_HYPHEN
                }
            } else {
                if (Character.isLowerCase(s[0])) {
                    if (LOWER_CAMEL_REGEX.matcher(s).matches()) {
                        return null
                    }
                } else {
                    if (UPPER_CAMEL_REGEX.matcher(s).matches()) {
                        return CaseFormat.UPPER_CAMEL
                    }
                }
            }

            return null
        }
    }
}
