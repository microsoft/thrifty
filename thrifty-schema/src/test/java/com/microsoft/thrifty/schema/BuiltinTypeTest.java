package com.microsoft.thrifty.schema;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class BuiltinTypeTest {
    @Test
    public void boolIsNotNumeric() {
        assertThat(((BuiltinType) BuiltinType.BOOL).isNumeric(), is(false));
    }

    @Test
    public void byteIsNumeric() {
        assertThat(((BuiltinType) BuiltinType.BYTE).isNumeric(), is(true));
    }

    @Test
    public void i8IsNumeric() {
        assertThat(((BuiltinType) BuiltinType.I8).isNumeric(), is(true));
    }

    @Test
    public void i16IsNumeric() {
        assertThat(((BuiltinType) BuiltinType.I16).isNumeric(), is(true));
    }

    @Test
    public void i32IsNumeric() {
        assertThat(((BuiltinType) BuiltinType.I32).isNumeric(), is(true));
    }

    @Test
    public void i64IsNumeric() {
        assertThat(((BuiltinType) BuiltinType.I64).isNumeric(), is(true));
    }

    @Test
    public void doubleIsNumeric() {
        assertThat(((BuiltinType) BuiltinType.DOUBLE).isNumeric(), is(true));
    }

    @Test
    public void stringIsNotNumeric() {
        assertThat(((BuiltinType) BuiltinType.STRING).isNumeric(), is(false));
    }

    @Test
    public void binaryIsNotNumeric() {
        assertThat(((BuiltinType) BuiltinType.BINARY).isNumeric(), is(false));
    }

    @Test
    public void voidIsNotNumeric() {
        assertThat(((BuiltinType) BuiltinType.VOID).isNumeric(), is(false));
    }
}