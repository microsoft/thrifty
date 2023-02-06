package com.microsoft.thrifty.gen

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TypeResolverTest {
    @Test
    fun defaultListTypeIsArrayList() {
        TypeResolver().listClass shouldBe TypeNames.ARRAY_LIST
    }

    @Test
    fun defaultMapTypeIsLinkedHashMap() {
        TypeResolver().mapClass shouldBe TypeNames.LINKED_HASH_MAP
    }

    @Test
    fun defaultSetTypeIsLinkedHashSet() {
        TypeResolver().setClass shouldBe TypeNames.LINKED_HASH_SET
    }
}