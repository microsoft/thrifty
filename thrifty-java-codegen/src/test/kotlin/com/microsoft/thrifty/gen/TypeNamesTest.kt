package com.microsoft.thrifty.gen

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TypeNamesTest {
    @Test
    fun typesUseJavaPackagesNotKotlin() {
        val typesAndExpectedNames = linkedMapOf(
            TypeNames.STRING to "java.lang.String",
            TypeNames.LIST to "java.util.List",
            TypeNames.MAP to "java.util.Map",
            TypeNames.SET to "java.util.Set",
            TypeNames.MAP_ENTRY to "java.util.Map.Entry",
            TypeNames.EXCEPTION to "java.lang.Exception",
            TypeNames.ARRAY_LIST to "java.util.ArrayList",
            TypeNames.LINKED_HASH_MAP to "java.util.LinkedHashMap",
            TypeNames.LINKED_HASH_SET to "java.util.LinkedHashSet",
            TypeNames.IO_EXCEPTION to "java.io.IOException",
        )

        for ((type, expectedName) in typesAndExpectedNames) {
            type.toString() shouldBe expectedName
        }
    }
}