package com.microsoft.thrifty.integration

import com.microsoft.thrifty.integration.gen.HasCommentBasedRedaction
import com.microsoft.thrifty.integration.gen.HasObfuscation
import com.microsoft.thrifty.integration.gen.HasRedaction
import com.microsoft.thrifty.integration.gen.ObfuscatedCollections
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.matchers.string.shouldNotContain
import io.kotlintest.shouldBe
import org.junit.Test
import java.util.Arrays
import java.util.Collections

class KotlinRedactionTest {
    @Test fun redaction() {
        val hr = HasRedaction.Builder()
                .one("value-one")
                .two("should-not-appear")
                .three("value-three")  // expe
                .build()

        "$hr".shouldContain("one=value-one")
        "$hr".shouldNotContain("should-not-appear")
        hr.two shouldBe "should-not-appear"
    }

    @Test
    fun obfuscation() {
        val hr = HasRedaction.Builder()
                .one("value-one")
                .two("value-two")
                .three("value-three")
                .build()

        "$hr".shouldContain("three=6A39B242")
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

        "$oc".shouldContain("numz=list<i32>(size=3)")
    }

    @Test
    fun obfuscatedMap() {
        val oc = ObfuscatedCollections.Builder()
                .stringz(Collections.singletonMap("foo", "bar"))
                .build()

        "$oc".shouldContain("stringz=map<string, string>(size=1)")
    }

    @Test
    fun obfuscatedString() {
        var ho = HasObfuscation.Builder().build()
        "$ho" shouldBe "HasObfuscation{ssn=null}"

        ho = HasObfuscation.Builder().ssn("123-45-6789").build()
        "$ho" shouldBe "HasObfuscation{ssn=1E1DB4B3}"
    }
}
