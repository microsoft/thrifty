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
import com.microsoft.thrifty.schema.parser.ConstValueElement;
import com.microsoft.thrifty.schema.parser.EnumMemberElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConstantTest {
    @Mock Linker linker;
    Location loc = Location.get("", "");

    @Before
    public void setup() {
        when(linker.lookupSymbol(anyString())).thenReturn(null);
    }

    @Test
    public void boolLiteral() {
        Constant.validate(linker, ConstValueElement.identifier(loc, "true"), ThriftType.BOOL);
        Constant.validate(linker, ConstValueElement.identifier(loc, "false"), ThriftType.BOOL);
        try {
            Constant.validate(linker, ConstValueElement.literal(loc, "nope"), ThriftType.BOOL);
            fail("Invalid identifier should not validate as a bool");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void boolConstant() {
        Constant c = mock(Constant.class);
        when(c.name()).thenReturn("aBool");
        when(c.type()).thenReturn(ThriftType.BOOL);

        when(linker.lookupSymbol("aBool")).thenReturn(c);

        Constant.validate(linker, ConstValueElement.identifier(loc, "aBool"), ThriftType.BOOL);
    }

    @Test
    public void boolWithWrongTypeOfConstant() {
        Constant c = mock(Constant.class);
        when(c.name()).thenReturn("aBool");
        when(c.type()).thenReturn(ThriftType.STRING);

        when(linker.lookupSymbol("aBool")).thenReturn(c);

        try {
            Constant.validate(linker, ConstValueElement.identifier(loc, "aBool"), ThriftType.BOOL);
            fail("Wrongly-typed constant should not validate");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void boolWithNonConstantIdentifier() {
        StructType s = mock(StructType.class);
        when(s.name()).thenReturn("someStruct");
        when(s.type()).thenReturn(ThriftType.get("someStruct", Collections.<NamespaceScope, String>emptyMap()));

        when(linker.lookupSymbol("someStruct")).thenReturn(s);

        try {
            Constant.validate(linker, ConstValueElement.identifier(loc, "someStruct"), ThriftType.BOOL);
            fail("Non-constant identifier should not validate");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void boolWithConstantHavingBoolTypedefValue() {
        Constant c = mock(Constant.class);
        when(c.name()).thenReturn("aBool");
        when(c.type()).thenReturn(ThriftType.typedefOf(ThriftType.BOOL, "Truthiness"));

        when(linker.lookupSymbol("aBool")).thenReturn(c);

        Constant.validate(linker, ConstValueElement.identifier(loc, "aBool"), ThriftType.BOOL);
    }

    @Test
    public void typedefWithCorrectLiteral() {
        ThriftType td = ThriftType.typedefOf(ThriftType.STRING, "Message");
        ConstValueElement value = ConstValueElement.literal(loc, "y helo thar");

        Constant.validate(linker, value, td);
    }

    @Test
    public void inRangeInt() {
        ConstValueElement value = ConstValueElement.integer(loc, 10);
        ThriftType type = ThriftType.I32;

        Constant.validate(linker, value, type);
    }

    @Test
    public void tooLargeInt() {
        ConstValueElement value = ConstValueElement.integer(loc, (long) Integer.MAX_VALUE + 1);
        ThriftType type = ThriftType.I32;

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
        ThriftType type = ThriftType.I32;

        try {
            Constant.validate(linker, value, type);
            fail("out-of-range i32 should fail");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("out of range for type i32"));
        }
    }

    @Test
    public void enumWithMember() {
        ThriftType tt = ThriftType.enumType("TestEnum", Collections.<NamespaceScope, String>emptyMap());
        ImmutableList.Builder<EnumType.Member> members = ImmutableList.builder();
        members.add(new EnumType.Member(EnumMemberElement.builder(loc).name("TEST").value(1).build()));

        EnumType et = mock(EnumType.class);
        when(et.name()).thenReturn("TestEnum");
        when(et.type()).thenReturn(tt);
        when(et.members()).thenReturn(members.build());

        when(linker.lookupSymbol(tt)).thenReturn(et);
        when(linker.lookupSymbol("TestEnum")).thenReturn(et);

        Constant.validate(linker, ConstValueElement.identifier(loc, "TestEnum.TEST"), tt);
    }

    @Test
    public void enumWithNonMemberIdentifier() {
        ThriftType tt = ThriftType.enumType("TestEnum", Collections.<NamespaceScope, String>emptyMap());
        ImmutableList.Builder<EnumType.Member> members = ImmutableList.builder();
        members.add(new EnumType.Member(EnumMemberElement.builder(loc).name("TEST").value(1).build()));

        EnumType et = mock(EnumType.class);
        when(et.name()).thenReturn("TestEnum");
        when(et.type()).thenReturn(tt);
        when(et.members()).thenReturn(members.build());

        when(linker.lookupSymbol("TestEnum")).thenReturn(et);

        try {
            Constant.validate(linker, ConstValueElement.identifier(loc, "TestEnum.NON_MEMBER"), tt);
            fail("Non-member identifier should fail");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void listOfInts() {
        ThriftType list = ThriftType.list(ThriftType.I32);
        ConstValueElement listValue = ConstValueElement.list(loc, Arrays.asList(
                ConstValueElement.integer(loc, 0),
                ConstValueElement.integer(loc, 1),
                ConstValueElement.integer(loc, 2)
        ));

        Constant.validate(linker, listValue, list);
    }

    @Test
    public void heterogeneousList() {
        ThriftType list = ThriftType.list(ThriftType.I32);
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
    public void setOfInts() {
        ThriftType setType = ThriftType.set(ThriftType.I32);
        ConstValueElement listValue = ConstValueElement.list(loc, Arrays.asList(
                ConstValueElement.integer(loc, 0),
                ConstValueElement.integer(loc, 1),
                ConstValueElement.integer(loc, 2)
        ));

        Constant.validate(linker, listValue, setType);
    }
}