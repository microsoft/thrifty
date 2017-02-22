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
package com.microsoft.thrifty.schema.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.ErrorReporter;
import com.microsoft.thrifty.schema.Location;
import com.microsoft.thrifty.schema.NamespaceScope;
import com.microsoft.thrifty.schema.Requiredness;
import okio.Okio;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class ThriftParserTest {
    @Test
    public void namespaces() {
        String thrift =
                "namespace java com.microsoft.thrifty.parser\n" +
                "namespace cpp microsoft.thrifty\n" +
                "namespace * microsoft.thrifty\n" +
                "php_namespace 'single_quoted_namespace'\n" +
                "php_namespace \"double_quoted_namespace\"";

        Location location = Location.get("", "namespaces.thrift");
        ThriftFileElement file = parse(thrift, location);

        ThriftFileElement expected = ThriftFileElement.builder(location)
                .namespaces(ImmutableList.<NamespaceElement>builder()
                        .add(NamespaceElement.builder(location.at(1, 1))
                                .scope(NamespaceScope.JAVA)
                                .namespace("com.microsoft.thrifty.parser")
                                .build())
                        .add(NamespaceElement.builder(location.at(2, 1))
                                .scope(NamespaceScope.CPP)
                                .namespace("microsoft.thrifty")
                                .build())
                        .add(NamespaceElement.builder(location.at(3, 1))
                                .scope(NamespaceScope.ALL)
                                .namespace("microsoft.thrifty")
                                .build())
                        .add(NamespaceElement.builder(location.at(4, 1))
                                .scope(NamespaceScope.PHP)
                                .namespace("single_quoted_namespace")
                                .build())
                        .add(NamespaceElement.builder(location.at(5, 1))
                                .scope(NamespaceScope.PHP)
                                .namespace("double_quoted_namespace")
                                .build())
                        .build())
                .build();

        assertThat(file, equalTo(expected));
    }

    @Test
    public void includes() {
        String thrift =
                "include 'inc/common.thrift'\n" +
                "include \".././parent.thrift\"\n" +
                "cpp_include 'inc/boost.hpp'\n" +
                "\n" +
                "namespace * microsoft";

        Location location = Location.get("", "includes.thrift");
        ThriftFileElement expected = ThriftFileElement.builder(location)
                .includes(ImmutableList.<IncludeElement>builder()
                        .add(IncludeElement.create(location.at(1, 1), false, "inc/common.thrift"))
                        .add(IncludeElement.create(location.at(2, 1), false, ".././parent.thrift"))
                        .add(IncludeElement.create(location.at(3, 1), true, "inc/boost.hpp"))
                        .build())
                .namespaces(ImmutableList.<NamespaceElement>builder()
                        .add(NamespaceElement.builder(location.at(5, 1))
                                .scope(NamespaceScope.ALL)
                                .namespace("microsoft")
                                .build())
                        .build())
                .build();

        assertThat(parse(thrift, location), equalTo(expected));
    }

    @Test
    public void simpleTypedefs() {
        String thrift =
                "typedef i32 MyInt\n" +
                "typedef string MyString\n" +
                "typedef binary PrivateKey\n";

        Location location = Location.get("", "typedefs.thrift");

        ThriftFileElement expected = ThriftFileElement.builder(location)
                .typedefs(ImmutableList.<TypedefElement>builder()
                        .add(TypedefElement.builder(location.at(1, 1))
                                .oldType(TypeElement.scalar(location.at(1, 9), "i32", null))
                                .newName("MyInt")
                                .build())
                        .add(TypedefElement.builder(location.at(2, 1))
                                .oldType(TypeElement.scalar(location.at(2, 9), "string", null))
                                .newName("MyString")
                                .build())
                        .add(TypedefElement.builder(location.at(3, 1))
                                .oldType(TypeElement.scalar(location.at(3, 9), "binary", null))
                                .newName("PrivateKey")
                                .build())
                        .build())
                .build();

        assertThat(parse(thrift, location), equalTo(expected));
    }

    @Test
    public void containerTypedefs() {
        String thrift =
                "typedef list<i32> IntList\n" +
                "typedef set<string> Names\n" +
                "typedef map < i16,set<binary > > BlobMap\n";

        Location location = Location.get("", "containerTypedefs.thrift");

        ThriftFileElement expected = ThriftFileElement.builder(location)
                .typedefs(ImmutableList.<TypedefElement>builder()
                        .add(TypedefElement.builder(location.at(1, 1))
                                .oldType(TypeElement.list(
                                        location.at(1, 9),
                                        TypeElement.scalar(location.at(1, 14), "i32", null), null))
                                .newName("IntList")
                                .build())
                        .add(TypedefElement.builder(location.at(2, 1))
                                .oldType(TypeElement.set(
                                        location.at(2, 9),
                                        TypeElement.scalar(location.at(2, 13), "string", null),
                                        null))
                                .newName("Names")
                                .build())
                        .add(TypedefElement.builder(location.at(3, 1))
                                .oldType(TypeElement.map(
                                        location.at(3, 9),
                                        TypeElement.scalar(location.at(3, 15), "i16", null),
                                        TypeElement.set(
                                                location.at(3, 19),
                                                TypeElement.scalar(location.at(3, 23), "binary", null),
                                                null),
                                        null))
                                .newName("BlobMap")
                                .build())
                        .build())
                .build();

        assertThat(parse(thrift, location), equalTo(expected));
    }

    @Test
    public void emptyStruct() {
        String thrift = "struct Empty {}";
        Location location = Location.get("", "empty.thrift");

        ThriftFileElement expected = ThriftFileElement.builder(location)
                .structs(ImmutableList.<StructElement>builder()
                        .add(StructElement.builder(location.at(1, 1))
                                .name("Empty")
                                .type(StructElement.Type.STRUCT)
                                .fields(ImmutableList.<FieldElement>of())
                                .build())
                        .build())
                .build();

        assertThat(parse(thrift, location), is(expected));
    }

    @Test
    public void simpleStruct() {
        String thrift = "struct Simple {\n" +
                "  /** This field is optional */\n" +
                "  1:i32 foo,\n" +
                "  // This next field is required\n" +
                "  2: required string bar   // and has trailing doc\n" +
                "}";

        Location location = Location.get("", "simple.thrift");

        ThriftFileElement expected = ThriftFileElement.builder(location)
                .structs(ImmutableList.<StructElement>builder()
                        .add(StructElement.builder(location.at(1, 1))
                                .name("Simple")
                                .type(StructElement.Type.STRUCT)
                                .fields(ImmutableList.<FieldElement>builder()
                                        .add(FieldElement.builder(location.at(3, 3))
                                                .name("foo")
                                                .fieldId(1)
                                                .requiredness(Requiredness.DEFAULT)
                                                .type(TypeElement.scalar(location.at(3, 5), "i32", null))
                                                .documentation("This field is optional\n")
                                                .build())
                                        .add(FieldElement.builder(location.at(5, 3))
                                                .name("bar")
                                                .fieldId(2)
                                                .requiredness(Requiredness.REQUIRED)
                                                .type(TypeElement.scalar(location.at(5, 15), "string", null))
                                                .documentation("This next field is required\nand has trailing doc\n")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        assertThat(parse(thrift, location), is(expected));
    }

    @Test
    public void trailingFieldDoc() {
        String thrift = "" +
                "// This struct demonstrates trailing comments\n" +
                "struct TrailingDoc {\n" +
                "  1: required string standard, // cpp-style\n" +
                "  2: required string python,    # py-style\n" +
                "  3: optional binary knr;      /** K&R-style **/\n" +
                "}";

        Location location = Location.get("", "trailing.thrift");
        ThriftFileElement expected = ThriftFileElement.builder(location)
                .structs(ImmutableList.<StructElement>builder()
                        .add(StructElement.builder(location.at(2, 1))
                                .documentation("This struct demonstrates trailing comments\n")
                                .name("TrailingDoc")
                                .type(StructElement.Type.STRUCT)
                                .fields(ImmutableList.<FieldElement>builder()
                                        .add(FieldElement.builder(location.at(3, 3))
                                                .fieldId(1)
                                                .requiredness(Requiredness.REQUIRED)
                                                .type(TypeElement.scalar(location.at(3, 15), "string", null))
                                                .name("standard")
                                                .documentation("cpp-style\n")
                                                .build())
                                        .add(FieldElement.builder(location.at(4, 3))
                                                .fieldId(2)
                                                .requiredness(Requiredness.REQUIRED)
                                                .type(TypeElement.scalar(location.at(4, 15), "string", null))
                                                .name("python")
                                                .documentation("py-style\n")
                                                .build())
                                        .add(FieldElement.builder(location.at(5, 3))
                                                .fieldId(3)
                                                .requiredness(Requiredness.OPTIONAL)
                                                .type(TypeElement.scalar(location.at(5, 15), "binary", null))
                                                .name("knr")
                                                .documentation("K&R-style *\n")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        assertThat(parse(thrift, location), is(expected));
    }

    @Test
    public void duplicateFieldIds() {
        String thrift = "struct Nope {\n" +
                "1: string foo;\n" +
                "1: string bar;\n" +
                "}";

        try {
            parse(thrift, Location.get("", "duplicateIds.thrift"));
            fail("Structs with duplicate field IDs should fail to parse");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("duplicate field ID:"));
        }
    }

    @Test
    public void implicitDuplicateFieldIds() {
        String thrift = "struct StillNope {\n" +
                "string foo;\n" +
                "string bar;\n" +
                "1: bytes baz\n" +
                "}";

        try {
            parse(thrift, Location.get("", "duplicateImplicitIds.thrift"));
            fail("Structs with duplicate implicit field IDs should fail to parse");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("duplicate field ID: 1"));
        }
    }

    @Test
    public void weirdFieldPermutations() {
        String thrift = "struct WeirdButLegal {\n" +
                "byte minimal\n" +
                "byte minimalWithSeparator,\n" +
                "byte minimalWithOtherSeparator;\n" +
                "required byte requiredWithoutSeparator\n" +
                "required byte requiredWithComma,\n" +
                "required byte requiredWithSemicolon;\n" +
                "optional i16 optionalWithoutSeparator\n" +
                "optional i16 optionalWithComma,\n" +
                "optional i16 optionalWithSemicolon;\n" +
                "10: i32 implicitOptional\n" +
                "11: i32 implicitOptionalWithComma,\n" +
                "12: i32 implicitOptionalWithSemicolon;\n" +
                "13: required i64 requiredId\n" +
                "14: required i64 requiredIdWithComma,\n" +
                "15: required i64 requiredIdWithSemicolon;\n" +
                "}";

        Location location = Location.get("", "weird.thrift");

        StructElement expectedStruct = StructElement.builder(location.at(1, 1))
                .name("WeirdButLegal")
                .type(StructElement.Type.STRUCT)
                .fields(ImmutableList.<FieldElement>builder()
                        .add(FieldElement.builder(location.at(2, 1))
                                .fieldId(1)
                                .requiredness(Requiredness.DEFAULT)
                                .type(TypeElement.scalar(location.at(2, 1), "byte", null))
                                .name("minimal")
                                .build())
                        .add(FieldElement.builder(location.at(3, 1))
                                .fieldId(2)
                                .requiredness(Requiredness.DEFAULT)
                                .type(TypeElement.scalar(location.at(3, 1), "byte", null))
                                .name("minimalWithSeparator")
                                .build())
                        .add(FieldElement.builder(location.at(4, 1))
                                .fieldId(3)
                                .requiredness(Requiredness.DEFAULT)
                                .type(TypeElement.scalar(location.at(4, 1), "byte", null))
                                .name("minimalWithOtherSeparator")
                                .build())
                        .add(FieldElement.builder(location.at(5, 1))
                                .fieldId(4)
                                .requiredness(Requiredness.REQUIRED)
                                .type(TypeElement.scalar(location.at(5, 10), "byte", null))
                                .name("requiredWithoutSeparator")
                                .build())
                        .add(FieldElement.builder(location.at(6, 1))
                                .fieldId(5)
                                .requiredness(Requiredness.REQUIRED)
                                .type(TypeElement.scalar(location.at(6, 10), "byte", null))
                                .name("requiredWithComma")
                                .build())
                        .add(FieldElement.builder(location.at(7, 1))
                                .fieldId(6)
                                .requiredness(Requiredness.REQUIRED)
                                .type(TypeElement.scalar(location.at(7, 10), "byte", null))
                                .name("requiredWithSemicolon")
                                .build())
                        .add(FieldElement.builder(location.at(8, 1))
                                .fieldId(7)
                                .requiredness(Requiredness.OPTIONAL)
                                .type(TypeElement.scalar(location.at(8, 10), "i16", null))
                                .name("optionalWithoutSeparator")
                                .build())
                        .add(FieldElement.builder(location.at(9, 1))
                                .fieldId(8)
                                .requiredness(Requiredness.OPTIONAL)
                                .type(TypeElement.scalar(location.at(9, 10), "i16", null))
                                .name("optionalWithComma")
                                .build())
                        .add(FieldElement.builder(location.at(10, 1))
                                .fieldId(9)
                                .requiredness(Requiredness.OPTIONAL)
                                .type(TypeElement.scalar(location.at(10, 10), "i16", null))
                                .name("optionalWithSemicolon")
                                .build())
                        .add(FieldElement.builder(location.at(11, 1))
                                .fieldId(10)
                                .requiredness(Requiredness.DEFAULT)
                                .type(TypeElement.scalar(location.at(11, 5), "i32", null))
                                .name("implicitOptional")
                                .build())
                        .add(FieldElement.builder(location.at(12, 1))
                                .fieldId(11)
                                .requiredness(Requiredness.DEFAULT)
                                .type(TypeElement.scalar(location.at(12, 5), "i32", null))
                                .name("implicitOptionalWithComma")
                                .build())
                        .add(FieldElement.builder(location.at(13, 1))
                                .fieldId(12)
                                .requiredness(Requiredness.DEFAULT)
                                .type(TypeElement.scalar(location.at(13, 5), "i32", null))
                                .name("implicitOptionalWithSemicolon")
                                .build())
                        .add(FieldElement.builder(location.at(14, 1))
                                .fieldId(13)
                                .requiredness(Requiredness.REQUIRED)
                                .type(TypeElement.scalar(location.at(14, 14), "i64", null))
                                .name("requiredId")
                                .build())
                        .add(FieldElement.builder(location.at(15, 1))
                                .fieldId(14)
                                .requiredness(Requiredness.REQUIRED)
                                .type(TypeElement.scalar(location.at(15, 14), "i64", null))
                                .name("requiredIdWithComma")
                                .build())
                        .add(FieldElement.builder(location.at(16, 1))
                                .fieldId(15)
                                .requiredness(Requiredness.REQUIRED)
                                .type(TypeElement.scalar(location.at(16, 14), "i64", null))
                                .name("requiredIdWithSemicolon")
                                .build())
                        .build())
                .build();

        ThriftFileElement expected = ThriftFileElement.builder(location)
                .structs(ImmutableList.of(expectedStruct))
                .build();

        assertThat(parse(thrift, location), equalTo(expected));
    }

    @Test
    public void invalidFieldIds() {
        String thrift = "struct NegativeId { -1: required i32 nope }";
        try {
            parse(thrift);
            fail("Should not parse a struct with a negative field ID");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("field ID must be greater than zero"));
        }

        thrift = "struct ZeroId {\n" +
                "  0: optional i64 stillNope\n" +
                "}";
        try {
            parse(thrift);
            fail("Should not parse a struct with a zero field ID");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("field ID must be greater than zero"));
        }
    }

    @Test
    public void services() {
        // NOTE: the service defined below is *not a legal Thrift service*.
        //       'oneway' functions must return void, and may not throw.
        //       We don't do this level of semantic validation here.
        String thrift = "" +
                "service Svc {\n" +
                "  FooResult foo(1:FooRequest request, 2: optional FooMeta meta)\n" +
                "  oneway BarResult bar() throws (1:FooException foo, 2:BarException bar)\n" +
                "}";

        Location location = Location.get("", "simpleService.thrift");
        ThriftFileElement expected = ThriftFileElement.builder(location)
                .services(ImmutableList.of(ServiceElement.builder(location.at(1, 1))
                        .name("Svc")
                        .functions(ImmutableList.<FunctionElement>builder()
                                .add(FunctionElement.builder(location.at(2, 3))
                                        .name("foo")
                                        .returnType(TypeElement.scalar(location.at(2, 3), "FooResult", null))
                                        .oneWay(false)
                                        .params(ImmutableList.<FieldElement>builder()
                                                .add(FieldElement.builder(location.at(2, 17))
                                                        .fieldId(1)
                                                        .requiredness(Requiredness.REQUIRED)
                                                        .name("request")
                                                        .type(TypeElement.scalar(location.at(2, 19), "FooRequest", null))
                                                        .build())
                                                .add(FieldElement.builder(location.at(2, 39))
                                                        .fieldId(2)
                                                        .requiredness(Requiredness.OPTIONAL)
                                                        .name("meta")
                                                        .type(TypeElement.scalar(location.at(2, 51), "FooMeta", null))
                                                        .build())
                                                .build())
                                        .build())
                                .add(FunctionElement.builder(location.at(3, 3))
                                        .name("bar")
                                        .oneWay(true)
                                        .returnType(TypeElement.scalar(location.at(3, 10), "BarResult", null))
                                        .exceptions(ImmutableList.<FieldElement>builder()
                                                .add(FieldElement.builder(location.at(3, 34))
                                                        .fieldId(1)
                                                        .name("foo")
                                                        .requiredness(Requiredness.DEFAULT)
                                                        .type(TypeElement.scalar(location.at(3, 36), "FooException", null))
                                                        .build())
                                                .add(FieldElement.builder(location.at(3, 54))
                                                        .fieldId(2)
                                                        .name("bar")
                                                        .requiredness(Requiredness.DEFAULT)
                                                        .type(TypeElement.scalar(location.at(3, 56), "BarException", null))
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build()))
                .build();

        assertThat(parse(thrift, location), is(expected));
    }

    @Test
    public void serviceWithNewlineBeforeThrows() {
        String thrift = "" +
                "service Svc {\n" +
                "  void foo()\n" +
                "    throws (1: Blargh blah)\n" +
                "  i32 bar()\n" +
                "}";

        Location loc = Location.get("", "services.thrift");
        ThriftFileElement expected = ThriftFileElement.builder(loc)
                .services(ImmutableList.of(ServiceElement.builder(loc.at(1, 1))
                        .name("Svc")
                        .functions(ImmutableList.of(
                                FunctionElement.builder(loc.at(2, 3))
                                        .name("foo")
                                        .returnType(TypeElement.scalar(loc.at(2, 3), "void", null))
                                        .exceptions(ImmutableList.of(FieldElement.builder(loc.at(3, 13))
                                                .fieldId(1)
                                                .name("blah")
                                                .requiredness(Requiredness.DEFAULT)
                                                .type(TypeElement.scalar(loc.at(3, 16), "Blargh", null))
                                                .build()))
                                        .build(),
                                FunctionElement.builder(loc.at(4, 3))
                                        .name("bar")
                                        .returnType(TypeElement.scalar(loc.at(4, 3), "i32", null))
                                        .build()))
                        .build()))
                .build();

        assertThat(parse(thrift, loc), equalTo(expected));
    }

    @Test
    public void unions() {
        String thrift = "" +
                "union Normal {\n" +
                "  2: i16 foo,\n" +
                "  4: i32 bar\n" +
                "}\n";

        Location location = Location.get("", "union.thrift");

        ThriftFileElement expected = ThriftFileElement.builder(location)
                .unions(ImmutableList.of(StructElement.builder(location.at(1, 1))
                        .name("Normal")
                        .type(StructElement.Type.UNION)
                        .fields(ImmutableList.<FieldElement>builder()
                                .add(FieldElement.builder(location.at(2, 3))
                                        .fieldId(2)
                                        .requiredness(Requiredness.DEFAULT)
                                        .name("foo")
                                        .type(TypeElement.scalar(location.at(2, 6), "i16", null))
                                        .build())
                                .add(FieldElement.builder(location.at(3, 3))
                                        .fieldId(4)
                                        .requiredness(Requiredness.DEFAULT)
                                        .name("bar")
                                        .type(TypeElement.scalar(location.at(3, 6), "i32", null))
                                        .build())
                                .build())
                        .build())).build();

        assertThat(parse(thrift, location), is(expected));
    }

    @Test
    public void unionCannotHaveRequiredField() {
        String thrift = "\n" +
                "union Normal {\n" +
                "  3: optional i16 foo,\n" +
                "  5: required i32 bar\n" +
                "}\n";

        try {
            parse(thrift, Location.get("", "unionWithRequired.thrift"));
            fail("Union cannot have a required field");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("unions cannot have required fields"));
        }
    }

    @Test
    public void unionCannotHaveMultipleDefaultValues() {
        String thrift = "\n" +
                "union Normal {\n" +
                "  3: i16 foo = 1,\n" +
                "  5: i32 bar = 2\n" +
                "}\n";

        try {
            parse(thrift, Location.get("", "unionWithRequired.thrift"));
            fail("Union cannot have a more than one default value");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("unions can have at most one default value"));
        }
    }

    @Test
    public void unionsCanHaveOneDefaultValue() {
        String thrift = "" +
                "union Default {\n" +
                "  1: i16 foo,\n" +
                "  2: i16 bar,\n" +
                "  3: i16 baz = 0x0FFF,\n" +
                "  4: i16 quux\n" +
                "}";

        Location location = Location.get("", "unionWithDefault.thrift");

        ThriftFileElement expected = ThriftFileElement.builder(location)
                .unions(ImmutableList.of(StructElement.builder(location.at(1, 1))
                        .name("Default")
                        .type(StructElement.Type.UNION)
                        .fields(ImmutableList.<FieldElement>builder()
                                .add(FieldElement.builder(location.at(2, 3))
                                        .fieldId(1)
                                        .name("foo")
                                        .requiredness(Requiredness.DEFAULT)
                                        .type(TypeElement.scalar(location.at(2, 6), "i16", null))
                                        .build())
                                .add(FieldElement.builder(location.at(3, 3))
                                        .fieldId(2)
                                        .name("bar")
                                        .requiredness(Requiredness.DEFAULT)
                                        .type(TypeElement.scalar(location.at(3, 6), "i16", null))
                                        .build())
                                .add(FieldElement.builder(location.at(4, 3))
                                        .fieldId(3)
                                        .name("baz")
                                        .requiredness(Requiredness.DEFAULT)
                                        .type(TypeElement.scalar(location.at(4, 6), "i16", null))
                                        .constValue(ConstValueElement.integer(location.at(4, 16), "0x0FFF", 0xFFF))
                                        .build())
                                .add(FieldElement.builder(location.at(5, 3))
                                        .fieldId(4)
                                        .name("quux")
                                        .requiredness(Requiredness.DEFAULT)
                                        .type(TypeElement.scalar(location.at(5, 6), "i16", null))
                                        .build())
                                .build())
                        .build()))
                .build();

        assertThat(parse(thrift, location), is(expected));
    }

    @Test
    public void simpleConst() {
        String thrift = "const i64 DefaultStatusCode = 200";
        Location location = Location.get("", "simpleConst.thrift");

        ThriftFileElement expected = ThriftFileElement.builder(location)
                .constants(ImmutableList.of(ConstElement.builder(location.at(1, 1))
                        .name("DefaultStatusCode")
                        .type(TypeElement.scalar(location.at(1, 7), "i64", null))
                        .value(ConstValueElement.integer(location.at(1, 31), "200", 200))
                        .build()))
                .build();

        assertThat(parse(thrift, location), is(expected));
    }

    @Test
    public void veryLargeConst() {
        String thrift = "const i64 Yuuuuuge = 0xFFFFFFFFFF";
        Location location = Location.get("", "veryLargeConst.thrift");

        ThriftFileElement expected = ThriftFileElement.builder(location)
                .constants(ImmutableList.of(ConstElement.builder(location.at(1, 1))
                        .name("Yuuuuuge")
                        .type(TypeElement.scalar(location.at(1, 7), "i64", null))
                        .value(ConstValueElement.integer(location.at(1, 22), "0xFFFFFFFFFF", 0xFFFFFFFFFFL))
                        .build()))
                .build();

        assertThat(parse(thrift, location), is(expected));
    }

    @Test
    public void listConst() {
        String thrift = "const list<string> Names = [\"foo\" \"bar\", \"baz\"; \"quux\"]";
        Location location = Location.get("", "listConst.thrift");

        ConstElement element = ConstElement.builder(location.at(1, 1))
                .name("Names")
                .type(TypeElement.list(
                        location.at(1, 7),
                        TypeElement.scalar(location.at(1, 12), "string", null),
                        null))
                .value(ConstValueElement.list(
                        location.at(1, 28),
                        "[\"foo\"\"bar\",\"baz\";\"quux\"]",
                        Arrays.asList(
                                ConstValueElement.literal(location.at(1, 29), "\"foo\"", "foo"),
                                ConstValueElement.literal(location.at(1, 35), "\"bar\"", "bar"),
                                ConstValueElement.literal(location.at(1, 42), "\"baz\"", "baz"),
                                ConstValueElement.literal(location.at(1, 49), "\"quux\"", "quux"))))
                .build();

        ThriftFileElement expected = ThriftFileElement.builder(location)
                .constants(ImmutableList.of(element))
                .build();

        assertThat(parse(thrift, location), is(expected));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapConst() {
        String thrift = "const map<string, string> Headers = {\n" +
                "  \"foo\": \"bar\",\n" +
                "  \"baz\": \"quux\";\n" +
                "}";

        Location location = Location.get("", "mapConst.thrift");
        ConstElement mapConst = ConstElement.builder(location.at(1, 1))
                .name("Headers")
                .type(TypeElement.map(
                        location.at(1, 7),
                        TypeElement.scalar(location.at(1, 11), "string", null),
                        TypeElement.scalar(location.at(1, 19), "string", null),
                        null))
                .value(ConstValueElement.map(location.at(1, 37),
                        "{\"foo\":\"bar\",\"baz\":\"quux\";}",
                        ImmutableMap.of(
                                ConstValueElement.literal(location.at(2, 3), "\"foo\"", "foo"),
                                ConstValueElement.literal(location.at(2, 10), "\"bar\"", "bar"),

                                ConstValueElement.literal(location.at(3, 3), "\"baz\"", "baz"),
                                ConstValueElement.literal(location.at(3, 10), "\"quux\"", "quux"))))
                .build();

        ThriftFileElement expected = ThriftFileElement.builder(location)
                .constants(ImmutableList.of(mapConst))
                .build();

        assertThat(parse(thrift, location), is(expected));
    }

    @Test
    public void structFieldWithConstValue() {
        String thrift = "struct Foo {\n" +
                "  100: i32 num = 1\n" +
                "}";

        Location location = Location.get("", "structWithConstValue.thrift");

        ThriftFileElement expected = ThriftFileElement.builder(location)
                .structs(ImmutableList.of(StructElement.builder(location.at(1, 1))
                        .name("Foo")
                        .type(StructElement.Type.STRUCT)
                        .fields(ImmutableList.of(FieldElement.builder(location.at(2, 3))
                                .fieldId(100)
                                .requiredness(Requiredness.DEFAULT)
                                .type(TypeElement.scalar(location.at(2, 8), "i32", null))
                                .name("num")
                                .constValue(ConstValueElement.integer(location.at(2, 18), "1", 1))
                                .build()))
                        .build()))
                .build();

        assertThat(parse(thrift, location), is(expected));
    }

    @Test
    public void canParseOfficialTestCase() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream stream = classLoader.getResourceAsStream("cases/TestThrift.thrift");
        String thrift = Okio.buffer(Okio.source(stream)).readUtf8();
        Location location = Location.get("cases", "TestThrift.thrift");

        // Not crashing is good enough here.  We'll be more strict with this file in the loader test.
        parse(thrift, location);
    }

    @Test
    public void bareEnums() throws Exception {
        String thrift = "enum Enum {\n" +
                "  FOO,\n" +
                "  BAR\n" +
                "}";

        Location location = Location.get("", "bareEnums.thrift");
        ThriftFileElement expected = ThriftFileElement.builder(location)
                .enums(ImmutableList.of(EnumElement.builder(location.at(1, 1))
                        .name("Enum")
                        .members(ImmutableList.<EnumMemberElement>builder()
                                .add(EnumMemberElement.builder(location.at(2, 3))
                                        .name("FOO")
                                        .value(0)
                                        .build())
                                .add(EnumMemberElement.builder(location.at(3, 3))
                                        .name("BAR")
                                        .value(1)
                                        .build())
                                .build())
                        .build()))
                .build();

        assertThat(parse(thrift, location), is(expected));
    }

    @Test
    public void enumWithLargeGaps() throws Exception {
        String thrift = "enum Gaps {\n" +
                "  SMALL = 10,\n" +
                "  MEDIUM = 100,\n" +
                "  ALSO_MEDIUM,\n" +
                "  LARGE = 5000\n" +
                "}";

        Location location = Location.get("", "enumWithLargeGaps.thrift");
        ThriftFileElement expected = ThriftFileElement.builder(location)
                .enums(ImmutableList.of(EnumElement.builder(location.at(1, 1))
                        .name("Gaps")
                        .members(ImmutableList.<EnumMemberElement>builder()
                                .add(EnumMemberElement.builder(location.at(2, 3))
                                        .name("SMALL")
                                        .value(10)
                                        .build())
                                .add(EnumMemberElement.builder(location.at(3, 3))
                                        .name("MEDIUM")
                                        .value(100)
                                        .build())
                                .add(EnumMemberElement.builder(location.at(4, 3))
                                        .name("ALSO_MEDIUM")
                                        .value(101)
                                        .build())
                                .add(EnumMemberElement.builder(location.at(5, 3))
                                        .name("LARGE")
                                        .value(5000)
                                        .build())
                                .build())
                        .build()))
                .build();

        assertThat(parse(thrift, location), is(expected));
    }

    @Test
    public void defaultValuesCanClash() throws Exception {
        String thrift = "enum Enum {\n" +
                "  FOO = 5,\n" +
                "  BAR = 4,\n" +
                "  BAZ\n" +
                "}";

        try {
            parse(thrift);
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("duplicate enum value"));
        }
    }

    @Test
    public void annotationsOnNamespaces() throws Exception {
        String thrift = "namespace java com.test (foo = 'bar')";
        ThriftFileElement file = parse(thrift);
        NamespaceElement ns = file.namespaces().get(0);

        AnnotationElement ann = ns.annotations();
        assertNotNull(ann);
        assertThat(ann.values().get("foo"), is("bar"));
    }

    @Test
    public void annotationsOnTypedefs() throws Exception {
        String thrift = "namespace java com.test\n" +
                "\n" +
                "typedef i32 StatusCode (boxed = 'false');";

        ThriftFileElement file = parse(thrift);
        TypedefElement typedef = file.typedefs().get(0);
        AnnotationElement ann = typedef.annotations();

        assertNotNull(ann);
        assertThat(ann.get("boxed"), is("false"));
    }

    @Test
    public void annotationsOnEnums() throws Exception {
        String thrift = "enum Foo {} (bar = 'baz')";
        ThriftFileElement file = parse(thrift);
        EnumElement e = file.enums().get(0);
        AnnotationElement ann = e.annotations();

        assertNotNull(ann);
        assertThat(ann.values().get("bar"), is("baz"));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void annotationsOnEnumMembers() throws Exception {
        String thrift = "" +
                "enum Foo {\n" +
                "  BAR (bar = 'abar'),\n" +
                "  BAZ = 1 (baz = 'abaz'),\n" +
                "  QUX (qux = 'aqux')\n" +
                "  WOO\n" +
                "}";

        ThriftFileElement file = parse(thrift);

        EnumElement anEnum = file.enums().get(0);
        EnumMemberElement bar = anEnum.members().get(0);
        EnumMemberElement baz = anEnum.members().get(1);
        EnumMemberElement qux = anEnum.members().get(2);
        EnumMemberElement woo = anEnum.members().get(3);

        assertThat(bar.annotations().get("bar"), is("abar"));
        assertThat(baz.annotations().get("baz"), is("abaz"));
        assertThat(qux.annotations().get("qux"), is("aqux"));
        assertNull(woo.annotations());
    }

    @Test
    public void annotationsOnServices() throws Exception {
        String thrift = "" +
                "service Svc {" +
                "  void foo(1: i32 bar)" +
                "} (async = 'true', java.races = 'false')";
        ThriftFileElement file = parse(thrift);
        ServiceElement svc = file.services().get(0);
        AnnotationElement ann = svc.annotations();

        assertNotNull(ann);
        assertThat(ann.size(), is(2));
        assertThat(ann.get("async"), is("true"));
        assertThat(ann.get("java.races"), is("false"));
    }

    @Test
    public void annotationsOnFunctions() throws Exception {
        String thrift = "service Svc {\n" +
                "  void nothrow() (test = 'a'),\n" +
                "  void nosep() (test = 'b')\n" +
                "  i32 hasThrow() throws(1: string what) (test = 'c');\n" +
                "}";

        ThriftFileElement file = parse(thrift);
        ServiceElement svc = file.services().get(0);
        AnnotationElement a = svc.functions().get(0).annotations();
        AnnotationElement b = svc.functions().get(1).annotations();
        AnnotationElement c = svc.functions().get(2).annotations();

        assertNotNull(a);
        assertNotNull(b);
        assertNotNull(c);

        assertThat(a.get("test"), is("a"));
        assertThat(b.get("test"), is("b"));
        assertThat(c.get("test"), is("c"));
    }

    @Test
    public void annotationsOnStructs() throws Exception {
        String thrift = "" +
                "struct Str {\n" +
                "  1: i32 hi,\n" +
                "  2: optional string there,\n" +
                "} (layout = 'sequential')";

        ThriftFileElement file = parse(thrift);
        StructElement struct = file.structs().get(0);
        AnnotationElement ann = struct.annotations();

        assertNotNull(ann);
        assertThat(ann.get("layout"), is("sequential"));
    }

    @Test
    public void annotationsOnUnions() throws Exception {
        String thrift = "" +
                "union Union {\n" +
                "  1: i32 hi,\n" +
                "  2: optional string there,\n" +
                "} (layout = 'padded')";

        ThriftFileElement file = parse(thrift);
        StructElement union = file.unions().get(0);
        AnnotationElement ann = union.annotations();

        assertNotNull(ann);
        assertThat(ann.get("layout"), is("padded"));
    }

    @Test
    public void annotationsOnExceptions() throws Exception {
        String thrift = "" +
                "exception Exception {\n" +
                "  1: required i32 boom,\n" +
                "} (java.runtime_exception)";

        ThriftFileElement file = parse(thrift);
        StructElement exception = file.exceptions().get(0);
        AnnotationElement ann = exception.annotations();

        assertNotNull(ann);
        assertThat(ann.get("java.runtime_exception"), is("true"));
    }

    @Test
    public void annotationsOnFields() {
        String thrift = "struct Str {\n" +
                "  1: i32 what (what = 'what'),\n" +
                "  2: binary data (compression = 'zlib') // doc\n" +
                "  3: optional i8 bits (synonym = 'byte')\n" +
                "}";

        ThriftFileElement file = parse(thrift);
        StructElement struct = file.structs().get(0);

        AnnotationElement anno = struct.fields().get(0).annotations();
        assertNotNull(anno);
        assertThat(anno.get("what"), is("what"));

        anno = struct.fields().get(1).annotations();
        assertNotNull(anno);
        assertThat(anno.get("compression"), is("zlib"));

        anno = struct.fields().get(2).annotations();
        assertNotNull(anno);
        assertThat(anno.get("synonym"), is("byte"));
    }

    @Test
    public void annotationsOnFieldTypes() {
        String thrift = "struct Str {\n" +
                "  1: map<string, i32> (python.immutable) foo\n" +
                "}";

        ThriftFileElement file = parse(thrift);
        StructElement struct = file.structs().get(0);
        FieldElement field = struct.fields().get(0);
        AnnotationElement anno = field.type().annotations();

        assertNotNull(anno);
        assertThat(anno.get("python.immutable"), is("true"));
    }

    @Test
    public void annotationsOnConsecutiveDefinitions() throws Exception {
        String thrift = "" +
                "namespace java com.foo.bar (ns = 'ok')\n" +
                "enum Foo {} (enumAnno = 'yep')\n" +
                "";

        parse(thrift);
    }

    @Test
    public void newlinesAreTricky() {
        // We must take care not to confuse the return type of the second
        // function with a possible 'throws' clause from the not-definitively-finished
        // first function.
        String thrift = "" +
                "typedef i32 typeof_int\n" +
                "service Stupid {\n" +
                "  i32 foo()\n" +
                "  typeof_int bar()\n" +
                "}";

        parse(thrift);
    }

    @Test
    public void fieldsWithoutSeparatorsDoNotConsumeNextFieldsDocumentation() {
        String thrift = "struct SomeRequest {\n" +
                "    /** Here's a comment. */\n" +
                "    1: required UUID clientUuid\n" +
                "\n" +
                "    /** Here's a longer comment. */\n" +
                "    2: optional string someOtherField\n" +
                "}";

        ThriftFileElement element = parse(thrift);
        StructElement struct = element.structs().get(0);
        FieldElement clientUuid = struct.fields().get(0);
        FieldElement someOtherField = struct.fields().get(1);

        assertThat(clientUuid.documentation(), is("Here's a comment.\n"));
        assertThat(someOtherField.documentation(), is("Here's a longer comment.\n"));
    }

    @Test
    public void trailingDocWithoutSeparatorWithAnnotationOnNewLine() {
        String thrift = "struct SomeRequest {\n" +
                "    /** Here's a comment. */\n" +
                "    1: required UUID clientUuid\n" +
                "         (bork = \"bork\")  // this belongs to clientUuid\n" +
                "\n" +
                "    /**\n" +
                "     * Here's a longer comment.\n" +
                "     * One two lines.\n" +
                "     */\n" +
                "    2: optional string someOtherField\n" +
                "}";

        ThriftFileElement element = parse(thrift);
        StructElement struct = element.structs().get(0);
        FieldElement clientUuid = struct.fields().get(0);
        FieldElement someOtherField = struct.fields().get(1);

        assertThat(clientUuid.documentation(), containsString("this belongs to clientUuid"));
        assertThat(someOtherField.documentation(), containsString("Here's a longer comment."));
    }

    @Test
    public void enumJavadocWithoutSeparators() {
        String thrift = "/**\n" +
                " * Some Javadoc\n" +
                " */\n" +
                "enum Value {\n" +
                "    /**\n" +
                "     * This is not trailing doc.\n" +
                "     */\n" +
                "    FIRST\n" +
                "    /**\n" +
                "     * Neither is this.\n" +
                "     */\n" +
                "    SECOND\n" +
                "}";

        Location loc = Location.get("foo", "bar.thrift");
        EnumElement expected = EnumElement.builder(loc.at(4,1))
                .documentation("Some Javadoc\n")
                .name("Value")
                .members(ImmutableList.<EnumMemberElement>builder()
                        .add(EnumMemberElement.builder(loc.at(8, 5))
                                .name("FIRST")
                                .value(0)
                                .documentation("This is not trailing doc.\n")
                                .build())
                        .add(EnumMemberElement.builder(loc.at(12, 5))
                                .name("SECOND")
                                .value(1)
                                .documentation("Neither is this.\n")
                                .build())
                        .build())
                .build();

        ThriftFileElement file = parse(thrift, loc);

        assertThat(file.enums().get(0), equalTo(expected));
    }

    @Test
    public void structsCanOmitAndReorderFieldIds() {
        ThriftFileElement element = parse("" +
                "struct Struct {\n" +
                "  required string foo;\n" +
                "  required string bar;\n" +
                "  5: required string baz\n" +
                "  required string qux;\n" +
                "  4: required string barfo\n" +
                "  required string beefy\n" +
                "}");

        StructElement struct = element.structs().get(0);
        ImmutableList<FieldElement> fields = struct.fields();
        assertThat(fields.get(0).fieldId(), equalTo(1));
        assertThat(fields.get(1).fieldId(), equalTo(2));
        assertThat(fields.get(2).fieldId(), equalTo(5));
        assertThat(fields.get(3).fieldId(), equalTo(6));
        assertThat(fields.get(4).fieldId(), equalTo(4));
        assertThat(fields.get(5).fieldId(), equalTo(7));
    }
    
    private static ThriftFileElement parse(String thrift) {
        return parse(thrift, Location.get("", ""));
    }

    private static ThriftFileElement parse(String thrift, Location location) {
        return ThriftParser.parse(location, thrift, new ErrorReporter());
    }

}
