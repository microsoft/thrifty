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
package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableList;
import com.microsoft.thrifty.schema.parser.ConstElement;
import com.microsoft.thrifty.schema.parser.ConstValueElement;
import com.microsoft.thrifty.schema.parser.EnumElement;
import com.microsoft.thrifty.schema.parser.EnumMemberElement;
import com.microsoft.thrifty.schema.parser.ThriftFileElement;
import com.microsoft.thrifty.schema.parser.ThriftParser;
import com.microsoft.thrifty.schema.parser.TypeElement;
import com.microsoft.thrifty.schema.parser.TypedefElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConstantTest {
    @Mock SymbolTable symbolTable;
    private Location loc = Location.get("", "");

    @Test
    public void boolLiteral() {
        Constant.validate(symbolTable, ConstValueElement.identifier(loc, "true", "true"), BuiltinType.BOOL);
        Constant.validate(symbolTable, ConstValueElement.identifier(loc, "false", "false"), BuiltinType.BOOL);
        try {
            Constant.validate(symbolTable, ConstValueElement.literal(loc, "nope", "nope"), BuiltinType.BOOL);
            fail("Invalid identifier should not validate as a bool");
        } catch (IllegalStateException expected) {
            assertThat(
                    expected.getMessage(),
                    containsString("Expected 'true', 'false', '1', '0', or a bool constant"));
        }
    }

    @Test
    public void boolConstant() {
        Constant c = makeConstant(
                "aBool",
                TypeElement.scalar(loc, "string", null),
                ConstValueElement.identifier(loc, "aBool", "aBool"),
                BuiltinType.BOOL);

        when(symbolTable.lookupConst("aBool")).thenReturn(c);

        Constant.validate(symbolTable, ConstValueElement.identifier(loc, "aBool", "aBool"), BuiltinType.BOOL);
    }

    @Test
    public void boolWithWrongTypeOfConstant() {
        Constant c = makeConstant(
                "aBool",
                TypeElement.scalar(loc, "string", null),
                ConstValueElement.identifier(loc, "aBool", "aBool"),
                BuiltinType.STRING);

        when(symbolTable.lookupConst("aBool")).thenReturn(c);

        try {
            Constant.validate(symbolTable, ConstValueElement.identifier(loc, "aBool", "aBool"), BuiltinType.BOOL);
            fail("Wrongly-typed constant should not validate");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void boolWithNonConstantIdentifier() {
        try {
            Constant.validate(symbolTable, ConstValueElement.identifier(loc, "someStruct", "someStruct"), BuiltinType.BOOL);
            fail("Non-constant identifier should not validate");
        } catch (IllegalStateException expected) {
            assertThat(
                    expected.getMessage(),
                    containsString("Expected 'true', 'false', '1', '0', or a bool constant; got: someStruct"));
        }
    }

    @Test
    public void boolWithConstantHavingBoolTypedefValue() {
        TypedefType td = makeTypedef(BuiltinType.BOOL, "Truthiness");

        Constant c = makeConstant(
                "aBool",
                TypeElement.scalar(loc, "Truthiness", null),
                ConstValueElement.identifier(loc, "aBool", "aBool"),
                td);

        when(symbolTable.lookupConst("aBool")).thenReturn(c);

        Constant.validate(symbolTable, ConstValueElement.identifier(loc, "aBool", "aBool"), BuiltinType.BOOL);
    }

    @Test
    public void typedefWithCorrectLiteral() {
        TypedefType td = makeTypedef(BuiltinType.STRING, "Message");

        ConstValueElement value = ConstValueElement.literal(loc, "\"y helo thar\"", "y helo thar");

        Constant.validate(symbolTable, value, td);
    }

    @Test
    public void inRangeInt() {
        ConstValueElement value = ConstValueElement.integer(loc, "10", 10);
        ThriftType type = BuiltinType.I32;

        Constant.validate(symbolTable, value, type);
    }

    @Test
    public void tooLargeInt() {
        ConstValueElement value = ConstValueElement.integer(
                loc, String.valueOf((long) Integer.MAX_VALUE + 1), (long) Integer.MAX_VALUE + 1);
        ThriftType type = BuiltinType.I32;

        try {
            Constant.validate(symbolTable, value, type);
            fail("out-of-range i32 should fail");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("out of range for type i32"));
        }
    }

    @Test
    public void tooSmallInt() {
        ConstValueElement value = ConstValueElement.integer(
                loc, String.valueOf((long) Integer.MIN_VALUE - 1), (long) Integer.MIN_VALUE - 1);
        ThriftType type = BuiltinType.I32;

        try {
            Constant.validate(symbolTable, value, type);
            fail("out-of-range i32 should fail");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("out of range for type i32"));
        }
    }

    @Test
    public void enumWithMember() {
        List<EnumMemberElement> memberElements = Collections.singletonList(
                new EnumMemberElement(loc, "TEST", 1)
        );

        EnumElement enumElement = new EnumElement(loc, "TestEnum", memberElements);

        EnumType et = new EnumType(enumElement, Collections.emptyMap());

        Constant.validate(symbolTable, ConstValueElement.identifier(loc, "TestEnum.TEST", "TestEnum.TEST"), et);
    }

    @Test
    public void enumWithNonMemberIdentifier() {
        List<EnumMemberElement> memberElements = Collections.singletonList(
                new EnumMemberElement(loc, "TEST", 1)
        );

        EnumElement enumElement = new EnumElement(loc, "TestEnum", memberElements);

        EnumType et = new EnumType(enumElement, Collections.emptyMap());

        try {
            Constant.validate(symbolTable, ConstValueElement.identifier(loc, "TestEnum.NON_MEMBER", "TestEnum.NON_MEMBER"), et);
            fail("Non-member identifier should fail");
        } catch (IllegalStateException expected) {
            assertThat(
                    expected.getMessage(),
                    containsString("'TestEnum.NON_MEMBER' is not a member of enum type TestEnum: members=[TEST]"));
        }
    }

    @Test
    public void unqualifiedEnumMember() {
        List<EnumMemberElement> memberElements = Collections.singletonList(
                new EnumMemberElement(loc, "TEST", 1)
        );

        EnumElement enumElement = new EnumElement(loc, "TestEnum", memberElements);

        EnumType et = new EnumType(enumElement, Collections.emptyMap());

        try {
            Constant.validate(symbolTable, ConstValueElement.identifier(loc, "TEST", "TEST"), et);
            fail("Expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Unqualified name 'TEST' is not a valid enum constant value"));
        }
    }

    @Test
    public void listOfInts() {
        ThriftType list = new ListType(BuiltinType.I32);
        ConstValueElement listValue = ConstValueElement.list(loc, "[0, 1, 2]", Arrays.asList(
                ConstValueElement.integer(loc, "0", 0),
                ConstValueElement.integer(loc, "1", 1),
                ConstValueElement.integer(loc, "2", 2)
        ));

        Constant.validate(symbolTable, listValue, list);
    }

    @Test
    public void heterogeneousList() {
        ThriftType list = new ListType(BuiltinType.I32);
        ConstValueElement listValue = ConstValueElement.list(loc, "[0, 1, \"2\"]", Arrays.asList(
                ConstValueElement.integer(loc, "0", 0),
                ConstValueElement.integer(loc, "1", 1),
                ConstValueElement.literal(loc, "\"2\"", "2")
        ));

        try {
            Constant.validate(symbolTable, listValue, list);
            fail("Heterogeneous lists should fail validation");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void typedefOfEnum() {
        List<EnumMemberElement> memberElements = Collections.singletonList(
                new EnumMemberElement(loc, "FOO", 1)
        );

        EnumElement enumElement = new EnumElement(loc, "AnEnum", memberElements);

        EnumType et = new EnumType(enumElement, Collections.emptyMap());

        TypedefType typedefType = makeTypedef(et, "Id");

        ConstValueElement value = ConstValueElement.identifier(loc, "AnEnum.FOO", "AnEnum.FOO");

        Constant.validate(symbolTable, value, typedefType);
    }

    @Test
    public void typedefOfWrongEnum() {
        List<EnumMemberElement> memberElements = Collections.singletonList(
                new EnumMemberElement(loc, "FOO", 1)
        );

        EnumElement enumElement = new EnumElement(loc, "AnEnum", memberElements);

        EnumType et = new EnumType(enumElement, Collections.emptyMap());

        List<EnumMemberElement> wrongMemberElements = Collections.singletonList(
                new EnumMemberElement(loc, "BAR", 2)
        );

        EnumElement wrongEnumElement = new EnumElement(loc, "DifferentEnum", wrongMemberElements);

        EnumType wt = new EnumType(wrongEnumElement, Collections.emptyMap());

        TypedefType typedefType = makeTypedef(wt, "Id");

        ConstValueElement value = ConstValueElement.identifier(loc, "AnEnum.FOO", "AnEnum.FOO");

        try {
            Constant.validate(symbolTable, value, typedefType);
            fail("An enum literal of type A cannot be assigned to a typedef of type B");
        } catch (IllegalStateException expected) {
            assertThat(
                    expected.getMessage(),
                    is("'AnEnum.FOO' is not a member of enum type DifferentEnum: members=[BAR]"));
        }
    }

    @Test
    public void setOfInts() {
        ThriftType setType = new SetType(BuiltinType.I32);
        ConstValueElement listValue = ConstValueElement.list(loc, "[0, 1, 2]", Arrays.asList(
                ConstValueElement.integer(loc, "0", 0),
                ConstValueElement.integer(loc, "1", 1),
                ConstValueElement.integer(loc, "2", 2)
        ));

        Constant.validate(symbolTable, listValue, setType);
    }

    private Constant makeConstant(String name, TypeElement typeElement, ConstValueElement value, ThriftType thriftType) {
        ConstElement element = new ConstElement(
                loc,
                typeElement,
                name,
                value);

        try {
            Constant constant = new Constant(element, Collections.emptyMap());

            Field field = Constant.class.getDeclaredField("type");
            field.setAccessible(true);
            field.set(constant, thriftType);

            return constant;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private TypedefType makeTypedef(ThriftType oldType, String newName) {
        TypedefElement element = new TypedefElement(
                loc,
                TypeElement.scalar(loc, "does_not_matter", null),
                newName);

        try {
            TypedefType td = new TypedefType(Collections.emptyMap(), element);

            Field field = TypedefType.class.getDeclaredField("oldType");
            field.setAccessible(true);
            field.set(td, oldType);

            return td;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }
}
