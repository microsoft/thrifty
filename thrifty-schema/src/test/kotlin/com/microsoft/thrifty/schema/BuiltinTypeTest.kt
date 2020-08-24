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

import io.kotest.matchers.shouldBe
import org.junit.Test

class BuiltinTypeTest {
    @Test
    fun boolIsNotNumeric() {
        (BuiltinType.BOOL as BuiltinType).isNumeric shouldBe false
    }

    @Test
    fun byteIsNumeric() {
        (BuiltinType.BYTE as BuiltinType).isNumeric shouldBe true
    }

    @Test
    fun i8IsNumeric() {
        (BuiltinType.I8 as BuiltinType).isNumeric shouldBe true
    }

    @Test
    fun i16IsNumeric() {
        (BuiltinType.I16 as BuiltinType).isNumeric shouldBe true
    }

    @Test
    fun i32IsNumeric() {
        (BuiltinType.I32 as BuiltinType).isNumeric shouldBe true
    }

    @Test
    fun i64IsNumeric() {
        (BuiltinType.I64 as BuiltinType).isNumeric shouldBe true
    }

    @Test
    fun doubleIsNumeric() {
        (BuiltinType.DOUBLE as BuiltinType).isNumeric shouldBe true
    }

    @Test
    fun stringIsNotNumeric() {
        (BuiltinType.STRING as BuiltinType).isNumeric shouldBe false
    }

    @Test
    fun binaryIsNotNumeric() {
        (BuiltinType.BINARY as BuiltinType).isNumeric shouldBe false
    }

    @Test
    fun voidIsNotNumeric() {
        (BuiltinType.VOID as BuiltinType).isNumeric shouldBe false
    }
}