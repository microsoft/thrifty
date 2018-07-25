package com.microsoft.thrifty.schema

import org.junit.Test

import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertThat

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