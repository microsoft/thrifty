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

import com.google.common.base.CaseFormat;
import javax.annotation.Nullable;

/**
 * Controls the style of names generated for fields.
 */
public abstract class FieldNamingPolicy {
    public abstract String apply(String name);

    public FieldNamingPolicy() {
    }

    /**
     * The default policy is to leave names unaltered from their definition in Thrift IDL.
     */
    public static final FieldNamingPolicy DEFAULT = new FieldNamingPolicy() {
        @Override
        public String apply(String name) {
            return name;
        }
    };

    /**
     * The Java policy generates camelCase names, unless the initial part of the field name
     * appears to be an acronym, in which case the casing is preserved.
     *
     * <p>"Acronym" here is defined to be two or more consecutive upper-case characters
     * at the beginning of the name.  Thus, this policy will preserve `.SSLFlag` over
     * `.sSLFlag`.
     */
    public static final FieldNamingPolicy JAVA = new FieldNamingPolicy() {
        @Override
        public String apply(String name) {

            CaseFormat caseFormat = CASE_FORMAT(name);
            if (caseFormat != null) {
                String formattedName = caseFormat.to(CaseFormat.LOWER_CAMEL, name);
                // Handle acronym as camel case made it lower case.
                if (name.length() > 1
                    && formattedName.length() > 1
                    && Character.isUpperCase(name.charAt(0))
                    && Character.isUpperCase(name.charAt(1))
                    && caseFormat != CaseFormat.UPPER_UNDERSCORE) {
                    return name.charAt(0) + formattedName.substring(1);
                }
                return formattedName;
            }

            // Unknown case format. Handle the acronym.
            if (Character.isUpperCase(name.charAt(0))) {
                if (name.length() == 1 || !Character.isUpperCase(name.charAt(1))) {
                    name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                }
            }
            return name;
        }
    };

    /**
     * Find case format from string.
     * @param s the input String
     * @return CaseFormat the case format of the string.
     */
    @Nullable
    private static CaseFormat CASE_FORMAT(String s) {

        if (s.contains("_")) {

            if (s.toUpperCase().equals(s))
                return CaseFormat.UPPER_UNDERSCORE;

            if (s.toLowerCase().equals(s))
                return CaseFormat.LOWER_UNDERSCORE;

        } else if (s.contains("-")) {

            if (s.toLowerCase().equals(s))
                return CaseFormat.LOWER_HYPHEN;

        } else {

            if (Character.isLowerCase(s.charAt(0))) {

                if (s.matches("([a-z]+[A-Z]+\\w+)+"))
                    return null;

            } else {

                if (s.matches("([A-Z]+[a-z]+\\w+)+"))
                    return CaseFormat.UPPER_CAMEL;
            }
        }

        return null;
    }
}
