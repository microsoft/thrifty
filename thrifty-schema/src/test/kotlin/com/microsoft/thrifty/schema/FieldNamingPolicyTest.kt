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

import io.kotlintest.shouldBe
import org.junit.Test

class FieldNamingPolicyTest {
    @Test
    fun defaultNamesAreUnaltered() {
        val policy = FieldNamingPolicy.DEFAULT

        policy.apply("SSLFlag") shouldBe "SSLFlag"
        policy.apply("MyField") shouldBe "MyField"
    }

    @Test
    fun javaPolicyCamelCasesNames() {
        val policy = FieldNamingPolicy.JAVA

        policy.apply("MyField") shouldBe "myField"
        policy.apply("X") shouldBe "x"
        policy.apply("abcde") shouldBe "abcde"
    }

    @Test
    fun javaPolicyPreservesAcronyms() {
        val policy = FieldNamingPolicy.JAVA

        policy.apply("OAuthToken") shouldBe "OAuthToken"
        policy.apply("SSLFlag") shouldBe "SSLFlag"
    }

    @Test
    fun javaPolicyDifferentCaseFormatCamelCaseNames() {
        val policy = FieldNamingPolicy.JAVA

        // lower_underscore
        policy.apply("my_field") shouldBe "myField"
        // lower-hyphen
        policy.apply("my-field") shouldBe "myField"
        // UpperCamel
        policy.apply("MyField") shouldBe "myField"
        // UPPER_UNDERSCORE
        policy.apply("MY_FIELD") shouldBe "myField"
    }
}
