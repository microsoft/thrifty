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
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.ConstValueElement;
import com.microsoft.thrifty.schema.parser.EnumMemberElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConstantTest {
    @Mock Linker linker;
    @Mock Program program;
    Location loc = Location.Companion.get("", "");

    @Before
    public void setup() {
        when(program.namespaces()).thenReturn(ImmutableMap.<NamespaceScope, String>of());
    }

    @Test
    public void boolLiteral() {
        Constant.validate(linker, ConstValueElement.identifier(loc, "true", "true"), BuiltinType.BOOL);
        Constant.validate(linker, ConstValueElement.identifier(loc, "false", "false"), BuiltinType.BOOL);
        try {
            Constant.validate(linker, ConstValueElement.literal(loc, "nope", "nope"), BuiltinType.BOOL);
            fail("Invalid identifier should not validate as a bool");
        } catch (IllegalStateException expected) {
            assertThat(
                    expected.getMessage(),
                    containsString("Expected 'true', 'false', '1', '0', or a bool constant"));
        }
    }

    @Test
    public void boolConstant() {
        Constant c = mock(Constant.class);
        when(c.name()).thenReturn("aBool");
        when(c.type()).thenReturn(BuiltinType.BOOL);

        when(linker.lookupConst("aBool")).thenReturn(c);

        Constant.validate(linker, ConstValueElement.identifier(loc, "aBool", "aBool"), BuiltinType.BOOL);
    }

    @Test
    public void boolWithWrongTypeOfConstant() {
        Constant c = mock(Constant.class);
        when(c.name()).thenReturn("aBool");
        when(c.type()).thenReturn(BuiltinType.STRING);

        when(linker.lookupConst("aBool")).thenReturn(c);

        try {
            Constant.validate(linker, ConstValueElement.identifier(loc, "aBool", "aBool"), BuiltinType.BOOL);
            fail("Wrongly-typed constant should not validate");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void boolWithNonConstantIdentifier() {
        StructType s = mock(StructType.class);
        when(s.name()).thenReturn("someStruct");

        try {
            Constant.validate(linker, ConstValueElement.identifier(loc, "someStruct", "someStruct"), BuiltinType.BOOL);
            fail("Non-constant identifier should not validate");
        } catch (IllegalStateException expected) {
            assertThat(
                    expected.getMessage(),
                    containsString("Expected 'true', 'false', '1', '0', or a bool constant; got: someStruct"));
        }
    }

    @Test
    public void boolWithConstantHavingBoolTypedefValue() {
        TypedefType td = mock(TypedefType.class);
        when(td.name()).thenReturn("Truthiness");
        when(td.getTrueType()).thenReturn(BuiltinType.BOOL);

        Constant c = mock(Constant.class);
        when(c.name()).thenReturn("aBool");
        when(c.type()).thenReturn(td);

        when(linker.lookupConst("aBool")).thenReturn(c);

        Constant.validate(linker, ConstValueElement.identifier(loc, "aBool", "aBool"), BuiltinType.BOOL);
    }

    @Test
    public void typedefWithCorrectLiteral() {
        //ThriftType td = typedefOf("string", "Message");
        TypedefType td = mock(TypedefType.class);
        when(td.getTrueType()).thenReturn(BuiltinType.STRING);
        when(td.name()).thenReturn("Message");

        ConstValueElement value = ConstValueElement.literal(loc, "\"y helo thar\"", "y helo thar");

        Constant.validate(linker, value, td);
    }

    @Test
    public void inRangeInt() {
        ConstValueElement value = ConstValueElement.integer(loc, "10", 10);
        ThriftType type = BuiltinType.I32;

        Constant.validate(linker, value, type);
    }

    @Test
    public void tooLargeInt() {
        ConstValueElement value = ConstValueElement.integer(
                loc, String.valueOf((long) Integer.MAX_VALUE + 1), (long) Integer.MAX_VALUE + 1);
        ThriftType type = BuiltinType.I32;

        try {
            Constant.validate(linker, value, type);
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
            Constant.validate(linker, value, type);
            fail("out-of-range i32 should fail");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("out of range for type i32"));
        }
    }

    @Test
    public void enumWithMember() {
        ImmutableList.Builder<EnumMember> members = ImmutableList.builder();
        members.add(new EnumMember(new EnumMemberElement(loc, "TEST", 1)));

        EnumType et = mock(EnumType.class);
        when(et.name()).thenReturn("TestEnum");
        when(et.members()).thenReturn(members.build());
        when(et.getTrueType()).thenReturn(et);
        when(et.isEnum()).thenReturn(true);

        Constant.validate(linker, ConstValueElement.identifier(loc, "TestEnum.TEST", "TestEnum.TEST"), et);
    }

    @Test
    public void enumWithNonMemberIdentifier() {
        ImmutableList.Builder<EnumMember> members = ImmutableList.builder();
        members.add(new EnumMember(new EnumMemberElement(loc, "TEST", 1)));

        EnumType et = mock(EnumType.class);
        when(et.name()).thenReturn("TestEnum");
        when(et.members()).thenReturn(members.build());
        when(et.getTrueType()).thenReturn(et);
        when(et.isEnum()).thenReturn(true);

        try {
            Constant.validate(linker, ConstValueElement.identifier(loc, "TestEnum.NON_MEMBER", "TestEnum.NON_MEMBER"), et);
            fail("Non-member identifier should fail");
        } catch (IllegalStateException expected) {
            assertThat(
                    expected.getMessage(),
                    containsString("'TestEnum.NON_MEMBER' is not a member of enum type TestEnum: members=[TEST]"));
        }
    }

    @Test
    public void unqualifiedEnumMember() {
        ImmutableList.Builder<EnumMember> members = ImmutableList.builder();
        members.add(new EnumMember(new EnumMemberElement(loc, "TEST", 1)));

        EnumType et = mock(EnumType.class);
        when(et.name()).thenReturn("TestEnum");
        when(et.members()).thenReturn(members.build());
        when(et.getTrueType()).thenReturn(et);
        when(et.isEnum()).thenReturn(true);

        try {
            Constant.validate(linker, ConstValueElement.identifier(loc, "TEST", "TEST"), et);
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

        Constant.validate(linker, listValue, list);
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
            Constant.validate(linker, listValue, list);
            fail("Heterogeneous lists should fail validation");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void typedefOfEnum() {
        EnumMember member = new EnumMember(new EnumMemberElement(loc, "FOO", 1));
        ImmutableList<EnumMember> members = ImmutableList.of(member);

        EnumType et = mock(EnumType.class);
        when(et.name()).thenReturn("AnEnum");
        when(et.members()).thenReturn(members);
        when(et.getTrueType()).thenReturn(et);
        when(et.isEnum()).thenReturn(true);

        TypedefType typedefType = mock(TypedefType.class);
        when(typedefType.name()).thenReturn("Id");
        when(typedefType.oldType()).thenReturn(et);
        when(typedefType.getTrueType()).thenReturn(et);

        ConstValueElement value = ConstValueElement.identifier(loc, "AnEnum.FOO", "AnEnum.FOO");

        Constant.validate(linker, value, typedefType);
    }

    @Test
    public void typedefOfWrongEnum() {
        EnumMember member = new EnumMember(new EnumMemberElement(loc, "FOO", 1));
        ImmutableList<EnumMember> members = ImmutableList.of(member);

        EnumType et = mock(EnumType.class);
        when(et.name()).thenReturn("AnEnum");
        when(et.members()).thenReturn(members);
        when(et.getTrueType()).thenReturn(et);
        when(et.isEnum()).thenReturn(true);

        EnumMember wrongMember = new EnumMember(new EnumMemberElement(loc, "BAR", 2));

        EnumType wt = mock(EnumType.class);
        when(wt.name()).thenReturn("DifferentEnum");
        when(wt.members()).thenReturn(ImmutableList.of(wrongMember));
        when(wt.isEnum()).thenReturn(true);
        when(wt.getTrueType()).thenReturn(wt);

        TypedefType typedefType = mock(TypedefType.class);
        when(typedefType.name()).thenReturn("Id");
        when(typedefType.oldType()).thenReturn(wt);
        when(typedefType.getTrueType()).thenReturn(wt);

        ConstValueElement value = ConstValueElement.identifier(loc, "AnEnum.FOO", "AnEnum.FOO");

        try {
            Constant.validate(linker, value, typedefType);
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

        Constant.validate(linker, listValue, setType);
    }
}
