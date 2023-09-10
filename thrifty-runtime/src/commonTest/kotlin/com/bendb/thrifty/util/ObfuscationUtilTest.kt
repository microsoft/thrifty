/*
 * Thrifty
 *
 * Copyright (c) Benjamin Bader
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
package com.bendb.thrifty.util

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ObfuscationUtilTest {
    @Test
    fun summarizeStringList() {
        val strings = listOf("one", "two", "three")
        val summary = ObfuscationUtil.summarizeCollection(strings, "list", "string")
        summary shouldBe "list<string>(size=3)"
    }

    @Test
    fun summarizeObjectSet() {
        val set = setOf(3L, 4L)
        val summary = ObfuscationUtil.summarizeCollection(set, "set", "i64")
        summary shouldBe "set<i64>(size=2)"
    }

    @Test
    fun summarizeNullList() {
        val list: List<Int>? = null
        val summary = ObfuscationUtil.summarizeCollection(list, "list", "i32")
        summary shouldBe "null"
    }

    @Test
    fun summarizeMap() {
        val map = emptyMap<String, Int>()
        val summary = ObfuscationUtil.summarizeMap(map, "string", "i32")
        summary shouldBe "map<string, i32>(size=0)"
    }

    @Test
    fun summarizeSingleObject() {
        val obj = 0x12345678
        ObfuscationUtil.hash(obj) shouldBe "12345678"
    }

    @Test
    fun summarizeSingleObjectZeroPads() {
        val obj = 0x5678
        ObfuscationUtil.hash(obj) shouldBe "00005678"
    }

    @Test
    fun summarizeNullObjext() {
        val obj: String? = null
        ObfuscationUtil.hash(obj) shouldBe "null"
    }
}
