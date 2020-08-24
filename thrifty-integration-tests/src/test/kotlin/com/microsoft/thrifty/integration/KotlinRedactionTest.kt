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
package com.microsoft.thrifty.integration

import com.microsoft.thrifty.integration.gen.HasCommentBasedRedaction
import com.microsoft.thrifty.integration.gen.HasObfuscation
import com.microsoft.thrifty.integration.gen.HasRedaction
import com.microsoft.thrifty.integration.gen.ObfuscatedCollections
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.contain
import org.junit.jupiter.api.Test
import java.util.Arrays
import java.util.Collections

class KotlinRedactionTest {
    @Test fun redaction() {
        val hr = HasRedaction.Builder()
                .one("value-one")
                .two("should-not-appear")
                .three("value-three")  // expe
                .build()

        "$hr" should contain("one=value-one")
        "$hr" shouldNot contain("should-not-appear")
        hr.two shouldBe "should-not-appear"
    }

    @Test
    fun obfuscation() {
        val hr = HasRedaction.Builder()
                .one("value-one")
                .two("value-two")
                .three("value-three")
                .build()

        "$hr" should contain("three=6A39B242")
        hr.three shouldBe "value-three"
    }

    @Test
    fun commentBasedRedaction() {
        val hcbr = HasCommentBasedRedaction.Builder()
                .foo("bar")
                .build()

        "$hcbr" shouldBe "HasCommentBasedRedaction{foo=<REDACTED>}"
    }

    @Test
    fun obfuscatedList() {
        val oc = ObfuscatedCollections.Builder()
                .numz(Arrays.asList(1, 2, 3))
                .build()

        "$oc" should contain("numz=list<i32>(size=3)")
    }

    @Test
    fun obfuscatedMap() {
        val oc = ObfuscatedCollections.Builder()
                .stringz(Collections.singletonMap("foo", "bar"))
                .build()

        "$oc" should contain("stringz=map<string, string>(size=1)")
    }

    @Test
    fun obfuscatedString() {
        var ho = HasObfuscation.Builder().build()
        "$ho" shouldBe "HasObfuscation{ssn=null}"

        ho = HasObfuscation.Builder().ssn("123-45-6789").build()
        "$ho" shouldBe "HasObfuscation{ssn=1E1DB4B3}"
    }
}
