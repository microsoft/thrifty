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

import org.junit.Test

import org.hamcrest.Matchers.`is`
import org.hamcrest.MatcherAssert.assertThat

class BuiltinTypeTest {
    @Test
    fun boolIsNotNumeric() {
        assertThat((BuiltinType.BOOL as BuiltinType).isNumeric, `is`(false))
    }

    @Test
    fun byteIsNumeric() {
        assertThat((BuiltinType.BYTE as BuiltinType).isNumeric, `is`(true))
    }

    @Test
    fun i8IsNumeric() {
        assertThat((BuiltinType.I8 as BuiltinType).isNumeric, `is`(true))
    }

    @Test
    fun i16IsNumeric() {
        assertThat((BuiltinType.I16 as BuiltinType).isNumeric, `is`(true))
    }

    @Test
    fun i32IsNumeric() {
        assertThat((BuiltinType.I32 as BuiltinType).isNumeric, `is`(true))
    }

    @Test
    fun i64IsNumeric() {
        assertThat((BuiltinType.I64 as BuiltinType).isNumeric, `is`(true))
    }

    @Test
    fun doubleIsNumeric() {
        assertThat((BuiltinType.DOUBLE as BuiltinType).isNumeric, `is`(true))
    }

    @Test
    fun stringIsNotNumeric() {
        assertThat((BuiltinType.STRING as BuiltinType).isNumeric, `is`(false))
    }

    @Test
    fun binaryIsNotNumeric() {
        assertThat((BuiltinType.BINARY as BuiltinType).isNumeric, `is`(false))
    }

    @Test
    fun voidIsNotNumeric() {
        assertThat((BuiltinType.VOID as BuiltinType).isNumeric, `is`(false))
    }
}