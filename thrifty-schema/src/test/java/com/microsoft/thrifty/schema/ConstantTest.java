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
import com.microsoft.thrifty.schema.parser.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConstantTest {
    @Mock Linker linker;
    @Mock Program program;
    Location loc = Location.get("", "");

    @Before
    public void setup() {
        when(linker.lookupSymbol(anyString())).thenReturn(null);
        when(program.namespaces()).thenReturn(ImmutableMap.<NamespaceScope, String>of());
    }

    @Test
    public void boolLiteral() {
        Constant.validate(linker, ConstValueElement.identifier(loc, "true"), BuiltinThriftType.BOOL);
        Constant.validate(linker, ConstValueElement.identifier(loc, "false"), BuiltinThriftType.BOOL);
        try {
            Constant.validate(linker, ConstValueElement.literal(loc, "nope"), BuiltinThriftType.BOOL);
            fail("Invalid identifier should not validate as a bool");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void boolConstant() {
        Constant c = mock(Constant.class);
        when(c.name()).thenReturn("aBool");
        when(c.type()).thenReturn(BuiltinThriftType.BOOL);

        when(linker.lookupConst("aBool")).thenReturn(c);

        Constant.validate(linker, ConstValueElement.identifier(loc, "aBool"), BuiltinThriftType.BOOL);
    }

    @Test
    public void boolWithWrongTypeOfConstant() {
        Constant c = mock(Constant.class);
        when(c.name()).thenReturn("aBool");
        when(c.type()).thenReturn(BuiltinThriftType.STRING);

        when(linker.lookupConst("aBool")).thenReturn(c);

        try {
            Constant.validate(linker, ConstValueElement.identifier(loc, "aBool"), BuiltinThriftType.BOOL);
            fail("Wrongly-typed constant should not validate");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void boolWithNonConstantIdentifier() {
        StructType s = mock(StructType.class);
        when(s.name()).thenReturn("someStruct");

        when(linker.lookupSymbol("someStruct")).thenReturn(s);

        try {
            Constant.validate(linker, ConstValueElement.identifier(loc, "someStruct"), BuiltinThriftType.BOOL);
            fail("Non-constant identifier should not validate");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void boolWithConstantHavingBoolTypedefValue() {
        TypedefType td = mock(TypedefType.class);
        when(td.name()).thenReturn("Truthiness");
        when(td.getTrueType()).thenReturn(BuiltinThriftType.BOOL);

        Constant c = mock(Constant.class);
        when(c.name()).thenReturn("aBool");
        when(c.type()).thenReturn(td);

        when(linker.lookupConst("aBool")).thenReturn(c);

        Constant.validate(linker, ConstValueElement.identifier(loc, "aBool"), BuiltinThriftType.BOOL);
    }

    @Test
    public void typedefWithCorrectLiteral() {
        //ThriftType td = typedefOf("string", "Message");
        TypedefType td = mock(TypedefType.class);
        when(td.getTrueType()).thenReturn(BuiltinThriftType.STRING);
        when(td.name()).thenReturn("Message");

        ConstValueElement value = ConstValueElement.literal(loc, "y helo thar");

        Constant.validate(linker, value, td);
    }

    @Test
    public void inRangeInt() {
        ConstValueElement value = ConstValueElement.integer(loc, 10);
        ThriftType type = BuiltinThriftType.I32;

        Constant.validate(linker, value, type);
    }

    @Test
    public void tooLargeInt() {
        ConstValueElement value = ConstValueElement.integer(loc, (long) Integer.MAX_VALUE + 1);
        ThriftType type = BuiltinThriftType.I32;

        try {
            Constant.validate(linker, value, type);
            fail("out-of-range i32 should fail");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("out of range for type i32"));
        }
    }

    @Test
    public void tooSmallInt() {
        ConstValueElement value = ConstValueElement.integer(loc, (long) Integer.MIN_VALUE - 1);
        ThriftType type = BuiltinThriftType.I32;

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
        members.add(new EnumMember(EnumMemberElement.builder(loc).name("TEST").value(1).build()));

        EnumType et = mock(EnumType.class);
        when(et.name()).thenReturn("TestEnum");
        when(et.members()).thenReturn(members.build());
        when(et.getTrueType()).thenReturn(et);
        when(et.isEnum()).thenReturn(true);

        when(linker.lookupSymbol("TestEnum")).thenReturn(et);

        Constant.validate(linker, ConstValueElement.identifier(loc, "TestEnum.TEST"), et);
    }

    @Test
    public void enumWithNonMemberIdentifier() {
        ImmutableList.Builder<EnumMember> members = ImmutableList.builder();
        members.add(new EnumMember(EnumMemberElement.builder(loc).name("TEST").value(1).build()));

        EnumType et = mock(EnumType.class);
        when(et.name()).thenReturn("TestEnum");
        when(et.members()).thenReturn(members.build());
        when(et.getTrueType()).thenReturn(et);
        when(et.isEnum()).thenReturn(true);

        when(linker.lookupSymbol("TestEnum")).thenReturn(et);

        try {
            Constant.validate(linker, ConstValueElement.identifier(loc, "TestEnum.NON_MEMBER"), et);
            fail("Non-member identifier should fail");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void unqualifiedEnumMember() {
        ImmutableList.Builder<EnumMember> members = ImmutableList.builder();
        members.add(new EnumMember(EnumMemberElement.builder(loc).name("TEST").value(1).build()));

        EnumType et = mock(EnumType.class);
        when(et.name()).thenReturn("TestEnum");
        when(et.members()).thenReturn(members.build());
        when(et.getTrueType()).thenReturn(et);
        when(et.isEnum()).thenReturn(true);

        when(linker.lookupSymbol("TestEnum")).thenReturn(et);

        try {
            Constant.validate(linker, ConstValueElement.identifier(loc, "TEST"), et);
            fail("Expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Unqualified name 'TEST' is not a valid enum constant value"));
        }
    }

    @Test
    public void listOfInts() {
        ThriftType list = new ListType(BuiltinThriftType.I32);
        ConstValueElement listValue = ConstValueElement.list(loc, Arrays.asList(
                ConstValueElement.integer(loc, 0),
                ConstValueElement.integer(loc, 1),
                ConstValueElement.integer(loc, 2)
        ));

        Constant.validate(linker, listValue, list);
    }

    @Test
    public void heterogeneousList() {
        ThriftType list = new ListType(BuiltinThriftType.I32);
        ConstValueElement listValue = ConstValueElement.list(loc, Arrays.asList(
                ConstValueElement.integer(loc, 0),
                ConstValueElement.integer(loc, 1),
                ConstValueElement.literal(loc, "2")
        ));

        try {
            Constant.validate(linker, listValue, list);
            fail("Heterogeneous lists should fail validation");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void typedefOfEnum() {
        EnumMember member = new EnumMember(EnumMemberElement.builder(loc).name("FOO").value(1).build());
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

        when(linker.lookupSymbol("AnEnum")).thenReturn(et);

        ConstValueElement value = ConstValueElement.identifier(loc, "AnEnum.FOO");

        Constant.validate(linker, value, typedefType);
    }

    @Test
    public void typedefOfWrongEnum() {
        EnumMember member = new EnumMember(EnumMemberElement.builder(loc).name("FOO").value(1).build());
        ImmutableList<EnumMember> members = ImmutableList.of(member);

        EnumType et = mock(EnumType.class);
        when(et.name()).thenReturn("AnEnum");
        when(et.members()).thenReturn(members);
        when(et.getTrueType()).thenReturn(et);
        when(et.isEnum()).thenReturn(true);

        EnumMember wrongMember = new EnumMember(EnumMemberElement.builder(loc).name("BAR").value(2).build());

        EnumType wt = mock(EnumType.class);
        when(wt.name()).thenReturn("DifferentEnum");
        when(wt.members()).thenReturn(ImmutableList.of(wrongMember));
        when(wt.isEnum()).thenReturn(true);
        when(wt.getTrueType()).thenReturn(wt);

        TypedefType typedefType = mock(TypedefType.class);
        when(typedefType.name()).thenReturn("Id");
        when(typedefType.oldType()).thenReturn(wt);
        when(typedefType.getTrueType()).thenReturn(wt);

        when(linker.lookupSymbol("AnEnum")).thenReturn(et);
        when(linker.lookupSymbol("DifferentEnum")).thenReturn(wt);

        ConstValueElement value = ConstValueElement.identifier(loc, "AnEnum.FOO");

        try {
            Constant.validate(linker, value, typedefType);
            fail("An enum literal of type A cannot be assigned to a typedef of type B");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void setOfInts() {
        ThriftType setType = new SetType(BuiltinThriftType.I32);
        ConstValueElement listValue = ConstValueElement.list(loc, Arrays.asList(
                ConstValueElement.integer(loc, 0),
                ConstValueElement.integer(loc, 1),
                ConstValueElement.integer(loc, 2)
        ));

        Constant.validate(linker, listValue, setType);
    }

    @Test
    @Ignore
    // TODO: Reimplement me
    public void builderCreatesCorrectConstant() {
        ConstElement constructorElement = mock(ConstElement.class);
        when(constructorElement.name()).thenReturn("name");
        Constant constant = new Constant(constructorElement, ImmutableMap.<NamespaceScope, String>of());

        ConstElement constElement = mock(ConstElement.class);
        when(constElement.name()).thenReturn("name");
        Map<NamespaceScope, String> namespaces = mock(Map.class);
        ThriftType thriftType = mock(ThriftType.class);

//        Constant builderConstant = constant.toBuilder()
//                .element(constElement)
//                .namespaces(namespaces)
//                .type(thriftType)
//                .build();
//
//        assertEquals(builderConstant.namespaces(), namespaces);
//        assertEquals(builderConstant.type(), thriftType);
    }

    @Test
    @Ignore
    // TODO: Reimplement me
    public void toBuilderCreatesCorrectConstant() {
        ConstElement constructorElement = mock(ConstElement.class);
        when(constructorElement.name()).thenReturn("name");
        //Constant constant = new Constant(constructorElement, new HashMap<NamespaceScope, String>());

        //assertEquals(constant.toBuilder().build(), constant);
    }

    private TypedefType typedefOf(String oldType, String name) {
        TypedefElement element = TypedefElement.builder(loc)
                .oldType(TypeElement.scalar(loc, oldType, null))
                .newName(name)
                .build();
        return new TypedefType(program, element);
    }

    private TypeElement typeElement(String name) {
        return TypeElement.scalar(loc, name, null);
    }
}
