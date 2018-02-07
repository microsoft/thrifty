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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FieldNamingPolicyTest {
    @Test
    public void defaultNamesAreUnaltered() {
        FieldNamingPolicy policy = FieldNamingPolicy.DEFAULT;

        assertThat(policy.apply("SSLFlag"), is("SSLFlag"));
        assertThat(policy.apply("MyField"), is("MyField"));
    }

    @Test
    public void javaPolicyCamelCasesNames() {
        FieldNamingPolicy policy = FieldNamingPolicy.JAVA;

        assertThat(policy.apply("MyField"), is("myField"));
        assertThat(policy.apply("X"), is("x"));
        assertThat(policy.apply("abcde"), is("abcde"));
    }

    @Test
    public void javaPolicyPreservesAcronyms() {
        FieldNamingPolicy policy = FieldNamingPolicy.JAVA;

        assertThat(policy.apply("OAuthToken"), is("OAuthToken"));
        assertThat(policy.apply("SSLFlag"), is("SSLFlag"));
    }

    @Test
    public void javaPolicyDifferentCaseFormatCamelCaseNames() {
        FieldNamingPolicy policy = FieldNamingPolicy.JAVA;

        // lower_underscore
        assertThat(policy.apply("my_field"), is("myField"));
        // lower-hyphen
        assertThat(policy.apply("my-field"), is("myField"));
        // UpperCamel
        assertThat(policy.apply("MyField"), is("myField"));
        // UPPER_UNDERSCORE
        assertThat(policy.apply("MY_FIELD"), is("myField"));
    }
}
