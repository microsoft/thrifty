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
package com.microsoft.thrifty.schema.parser

import com.microsoft.thrifty.schema.ErrorReporter
import com.microsoft.thrifty.schema.Location
import com.microsoft.thrifty.schema.NamespaceScope
import com.microsoft.thrifty.schema.Requiredness
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.startWith
import okio.buffer
import okio.source
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ThriftParserTest {

    @BeforeEach
    fun setup() {
        ThriftyParserPlugins.setUUIDProvider(object : ThriftyParserPlugins.UUIDProvider {
            override fun call(): UUID {
                return TEST_UUID
            }
        })
    }

    @AfterEach
    fun tearDown() {
        ThriftyParserPlugins.reset()
    }

    @Test
    fun namespaces() {
        val thrift = "namespace java com.microsoft.thrifty.parser\n" +
                "namespace cpp microsoft.thrifty\n" +
                "namespace * microsoft.thrifty\n" +
                "php_namespace 'single_quoted_namespace'\n" +
                "php_namespace \"double_quoted_namespace\""

        val location = Location.get("", "namespaces.thrift")
        val file = parse(thrift, location)

        val expected = ThriftFileElement(
                location = location,
                namespaces = listOf(
                        NamespaceElement(
                                location = location.at(1, 1),
                                scope = NamespaceScope.JAVA,
                                namespace = "com.microsoft.thrifty.parser"
                        ),
                        NamespaceElement(
                                location = location.at(2, 1),
                                scope = NamespaceScope.CPP,
                                namespace = "microsoft.thrifty"
                        ),
                        NamespaceElement(
                                location = location.at(3, 1),
                                scope = NamespaceScope.ALL,
                                namespace = "microsoft.thrifty"
                        ),
                        NamespaceElement(
                                location = location.at(4, 1),
                                scope = NamespaceScope.PHP,
                                namespace = "single_quoted_namespace"
                        ),
                        NamespaceElement(
                                location = location.at(5, 1),
                                scope = NamespaceScope.PHP,
                                namespace = "double_quoted_namespace"
                        )
                )
        )

        file shouldBe expected
    }

    @Test
    fun includes() {
        val thrift = "include 'inc/common.thrift'\n" +
                "include \".././parent.thrift\"\n" +
                "cpp_include 'inc/boost.hpp'\n" +
                "\n" +
                "namespace * microsoft"

        val location = Location.get("", "includes.thrift")
        val expected = ThriftFileElement(
                location = location,
                includes = listOf(
                        IncludeElement(location.at(1, 1), false, "inc/common.thrift"),
                        IncludeElement(location.at(2, 1), false, ".././parent.thrift"),
                        IncludeElement(location.at(3, 1), true, "inc/boost.hpp")
                ),
                namespaces = listOf(
                        NamespaceElement(location.at(5, 1), NamespaceScope.ALL, "microsoft")
                )
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun simpleTypedefs() {
        val thrift = "typedef i32 MyInt\n" +
                "typedef string MyString\n" +
                "typedef binary PrivateKey\n"

        val location = Location.get("", "typedefs.thrift")

        val expected = ThriftFileElement(
                location = location,
                typedefs = listOf(
                        TypedefElement(
                                location = location.at(1, 1),
                                oldType = ScalarTypeElement(location.at(1, 9), "i32", null),
                                newName = "MyInt"),
                        TypedefElement(
                                location = location.at(2, 1),
                                oldType = ScalarTypeElement(location.at(2, 9), "string", null),
                                newName = "MyString"),
                        TypedefElement(
                                location = location.at(3, 1),
                                oldType = ScalarTypeElement(location.at(3, 9), "binary", null),
                                newName = "PrivateKey")
                )
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun containerTypedefs() {
        val thrift = "typedef list<i32> IntList\n" +
                "typedef set<string> Names\n" +
                "typedef map < i16,set<binary > > BlobMap\n"

        val location = Location.get("", "containerTypedefs.thrift")

        val expected = ThriftFileElement(
                location = location,
                typedefs = listOf(
                        TypedefElement(
                                location = location.at(1, 1),
                                oldType = ListTypeElement(
                                        location.at(1, 9),
                                        ScalarTypeElement(location.at(1, 14), "i32")),
                                newName = "IntList"
                        ),
                        TypedefElement(
                                location = location.at(2, 1),
                                oldType = SetTypeElement(
                                        location.at(2, 9),
                                        ScalarTypeElement(location.at(2, 13), "string")),
                                newName = "Names"
                        ),
                        TypedefElement(
                                location = location.at(3, 1),
                                oldType = MapTypeElement(
                                        location.at(3, 9),
                                        ScalarTypeElement(location.at(3, 15), "i16"),
                                        SetTypeElement(
                                                location.at(3, 19),
                                                ScalarTypeElement(location.at(3, 23), "binary"))),
                                newName = "BlobMap"
                        )
                    )
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun emptyStruct() {
        val thrift = "struct Empty {}"
        val location = Location.get("", "empty.thrift")

        val expected = ThriftFileElement(
                location = location,
                structs = listOf(
                        StructElement(
                                location = location.at(1, 1),
                                name = "Empty",
                                type = StructElement.Type.STRUCT,
                                fields = emptyList()
                        )
                )
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun simpleStruct() {
        val thrift = "struct Simple {\n" +
                "  /** This field is optional */\n" +
                "  1:i32 foo,\n" +
                "  // This next field is required\n" +
                "  2: required string bar   // and has trailing doc\n" +
                "}"

        val location = Location.get("", "simple.thrift")

        val expected = ThriftFileElement(
                location = location,
                structs = listOf(
                        StructElement(
                                location = location.at(1, 1),
                                name = "Simple",
                                type = StructElement.Type.STRUCT,
                                fields = listOf(
                                        FieldElement(
                                                location = location.at(3, 3),
                                                fieldId = 1,
                                                type = ScalarTypeElement(location.at(3, 5), "i32"),
                                                name = "foo",
                                                requiredness = Requiredness.DEFAULT,
                                                documentation = "This field is optional\n"),
                                        FieldElement(
                                                location = location.at(5, 3),
                                                fieldId = 2,
                                                type = ScalarTypeElement(location.at(5, 15), "string"),
                                                name = "bar",
                                                requiredness = Requiredness.REQUIRED,
                                                documentation = "This next field is required\nand has trailing doc\n")
                                )
                        )

                )
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun trailingFieldDoc() {
        val thrift = "" +
                "// This struct demonstrates trailing comments\n" +
                "struct TrailingDoc {\n" +
                "  1: required string standard, // cpp-style\n" +
                "  2: required string python,    # py-style\n" +
                "  3: optional binary knr;      /** K&R-style **/\n" +
                "}"

        val location = Location.get("", "trailing.thrift")
        val expected = ThriftFileElement(
                location = location,
                structs = listOf(
                        StructElement(
                                location = location.at(2, 1),
                                name = "TrailingDoc",
                                type = StructElement.Type.STRUCT,
                                fields = listOf(
                                        FieldElement(
                                                location = location.at(3, 3),
                                                fieldId = 1,
                                                type = ScalarTypeElement(location.at(3, 15), "string"),
                                                name = "standard",
                                                requiredness = Requiredness.REQUIRED,
                                                documentation = "cpp-style\n"),
                                        FieldElement(
                                                location = location.at(4, 3),
                                                fieldId = 2,
                                                type = ScalarTypeElement(location.at(4, 15), "string"),
                                                name = "python",
                                                requiredness = Requiredness.REQUIRED,
                                                documentation = "py-style\n"),
                                        FieldElement(
                                                location = location.at(5, 3),
                                                fieldId = 3,
                                                type = ScalarTypeElement(location.at(5, 15), "binary"),
                                                name = "knr",
                                                requiredness = Requiredness.OPTIONAL,
                                                documentation = "K&R-style *\n")),
                                documentation = "This struct demonstrates trailing comments\n")
                )
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun duplicateFieldIds() {
        val thrift = "struct Nope {\n" +
                "1: string foo;\n" +
                "1: string bar;\n" +
                "}"

        val e = shouldThrow<IllegalStateException> { parse(thrift, Location.get("", "duplicateIds.thrift")) }
        e.message shouldContain "duplicate field ID:"
    }

    @Test
    fun implicitDuplicateFieldIds() {
        val thrift = "struct StillNope {\n" +
                "string foo;\n" +
                "string bar;\n" +
                "1: bytes baz\n" +
                "}"

        val e = shouldThrow<IllegalStateException> { parse(thrift, Location.get("", "duplicateImplicitIds.thrift")) }
        e.message shouldContain "duplicate field ID: 1"
    }

    @Test
    fun weirdFieldPermutations() {
        val thrift = "struct WeirdButLegal {\n" +
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
                "}"

        val location = Location.get("", "weird.thrift")

        val expectedStruct = StructElement(
                location = location.at(1, 1),
                name = "WeirdButLegal",
                type = StructElement.Type.STRUCT,
                fields = listOf(
                        FieldElement(
                                location = location.at(2, 1),
                                fieldId = 1,
                                type = ScalarTypeElement(location.at(2, 1), "byte", null),
                                name = "minimal",
                                requiredness = Requiredness.DEFAULT),
                        FieldElement(
                                location = location.at(3, 1),
                                fieldId = 2,
                                type = ScalarTypeElement(location.at(3, 1), "byte", null),
                                name = "minimalWithSeparator",
                                requiredness = Requiredness.DEFAULT),
                        FieldElement(
                                location = location.at(4, 1),
                                fieldId = 3,
                                type = ScalarTypeElement(location.at(4, 1), "byte", null),
                                name = "minimalWithOtherSeparator",
                                requiredness = Requiredness.DEFAULT),
                        FieldElement(
                                location = location.at(5, 1),
                                fieldId = 4,
                                type = ScalarTypeElement(location.at(5, 10), "byte", null),
                                name = "requiredWithoutSeparator",
                                requiredness = Requiredness.REQUIRED),
                        FieldElement(
                                location = location.at(6, 1),
                                fieldId = 5,
                                type = ScalarTypeElement(location.at(6, 10), "byte", null),
                                name = "requiredWithComma",
                                requiredness = Requiredness.REQUIRED),
                        FieldElement(
                                location = location.at(7, 1),
                                fieldId = 6,
                                type = ScalarTypeElement(location.at(7, 10), "byte", null),
                                name = "requiredWithSemicolon",
                                requiredness = Requiredness.REQUIRED),
                        FieldElement(
                                location = location.at(8, 1),
                                fieldId = 7,
                                type = ScalarTypeElement(location.at(8, 10), "i16", null),
                                name = "optionalWithoutSeparator",
                                requiredness = Requiredness.OPTIONAL),
                        FieldElement(
                                location = location.at(9, 1),
                                fieldId = 8,
                                type = ScalarTypeElement(location.at(9, 10), "i16", null),
                                name = "optionalWithComma",
                                requiredness = Requiredness.OPTIONAL),
                        FieldElement(
                                location = location.at(10, 1),
                                fieldId = 9,
                                type = ScalarTypeElement(location.at(10, 10), "i16", null),
                                name = "optionalWithSemicolon",
                                requiredness = Requiredness.OPTIONAL),
                        FieldElement(
                                location = location.at(11, 1),
                                fieldId = 10,
                                type = ScalarTypeElement(location.at(11, 5), "i32", null),
                                name = "implicitOptional",
                                requiredness = Requiredness.DEFAULT),
                        FieldElement(
                                location = location.at(12, 1),
                                fieldId = 11,
                                type = ScalarTypeElement(location.at(12, 5), "i32", null),
                                name = "implicitOptionalWithComma",
                                requiredness = Requiredness.DEFAULT),
                        FieldElement(
                                location = location.at(13, 1),
                                fieldId = 12,
                                type = ScalarTypeElement(location.at(13, 5), "i32", null),
                                name = "implicitOptionalWithSemicolon",
                                requiredness = Requiredness.DEFAULT),
                        FieldElement(
                                location = location.at(14, 1),
                                fieldId = 13,
                                type = ScalarTypeElement(location.at(14, 14), "i64", null),
                                name = "requiredId",
                                requiredness = Requiredness.REQUIRED),
                        FieldElement(
                                location = location.at(15, 1),
                                fieldId = 14,
                                type = ScalarTypeElement(location.at(15, 14), "i64", null),
                                name = "requiredIdWithComma",
                                requiredness = Requiredness.REQUIRED),
                        FieldElement(
                                location = location.at(16, 1),
                                fieldId = 15,
                                type = ScalarTypeElement(location.at(16, 14), "i64", null),
                                name = "requiredIdWithSemicolon",
                                requiredness = Requiredness.REQUIRED)
                )
        )

        val expected = ThriftFileElement(
                location = location,
                structs = listOf(expectedStruct)
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun invalidFieldIds() {
        val negativeIdThrift = "struct NegativeId { -1: required i32 nope }"
        val e1 = shouldThrow<IllegalStateException> { parse(negativeIdThrift) }
        e1.message shouldContain "field ID must be greater than zero"

        val zeroIdThrift = "struct ZeroId {\n" +
                "  0: optional i64 stillNope\n" +
                "}"

        val e2 = shouldThrow<IllegalStateException> { parse(zeroIdThrift) }
        e2.message shouldContain "field ID must be greater than zero"

    }

    @Test
    fun services() {
        // NOTE: the service defined below is *not a legal Thrift service*.
        //       'oneway' functions must return void, and may not throw.
        //       We don't do this level of semantic validation here.
        val thrift = "" +
                "service Svc {\n" +
                "  FooResult foo(1:FooRequest request, 2: optional FooMeta meta)\n" +
                "  oneway BarResult bar() throws (1:FooException foo, 2:BarException bar)\n" +
                "}"

        val location = Location.get("", "simpleService.thrift")
        val expected = ThriftFileElement(
                location = location,
                services = listOf(
                        ServiceElement(
                                location.at(1, 1),
                                "Svc",
                                listOf(
                                        FunctionElement(
                                                location = location.at(2, 3),
                                                name = "foo",
                                                returnType = ScalarTypeElement(location.at(2, 3), "FooResult"),
                                                params = listOf(
                                                        FieldElement(location.at(2, 17),
                                                                1,
                                                                ScalarTypeElement(location.at(2, 19), "FooRequest"),
                                                                "request",
                                                                Requiredness.REQUIRED),
                                                        FieldElement(
                                                                location.at(2, 39),
                                                                2,
                                                                ScalarTypeElement(location.at(2, 51), "FooMeta"),
                                                                "meta",
                                                                Requiredness.OPTIONAL)
                                                )
                                        ),
                                        FunctionElement(
                                                location = location.at(3, 3),
                                                name = "bar",
                                                returnType = ScalarTypeElement(location.at(3, 10), "BarResult"),
                                                exceptions = listOf(
                                                        FieldElement(location.at(3, 34),
                                                                1,
                                                                ScalarTypeElement(location.at(3, 36), "FooException"),
                                                                "foo",
                                                                Requiredness.DEFAULT),
                                                        FieldElement(location.at(3, 54),
                                                                2,
                                                                ScalarTypeElement(location.at(3, 56), "BarException"),
                                                                "bar",
                                                                Requiredness.DEFAULT)
                                                ),
                                                oneWay = true
                                        )
                                )
                        )
                )
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun serviceWithNewlineBeforeThrows() {
        val thrift = "" +
                "service Svc {\n" +
                "  void foo()\n" +
                "    throws (1: Blargh blah)\n" +
                "  i32 bar()\n" +
                "}"

        val location = Location.get("", "services.thrift")
        val expected = ThriftFileElement(
                location = location,
                services = listOf(
                        ServiceElement(
                                location = location.at(1, 1),
                                name = "Svc",
                                functions = listOf(
                                        FunctionElement(
                                                location = location.at(2, 3),
                                                name = "foo",
                                                returnType = ScalarTypeElement(location.at(2, 3), "void"),
                                                exceptions = listOf(FieldElement(
                                                        location = location.at(3, 13),
                                                        fieldId = 1,
                                                        type = ScalarTypeElement(location.at(3, 16), "Blargh"),
                                                        name = "blah",
                                                        requiredness = Requiredness.DEFAULT))),
                                        FunctionElement(
                                                location = location.at(4, 3),
                                                name = "bar",
                                                returnType = ScalarTypeElement(location.at(4, 3), "i32"))
                                ))))

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun unions() {
        val thrift = "" +
                "union Normal {\n" +
                "  2: i16 foo,\n" +
                "  4: i32 bar\n" +
                "}\n"

        val location = Location.get("", "union.thrift")

        val expected = ThriftFileElement(location,
                unions = listOf(
                        StructElement(
                                location = location.at(1, 1),
                                name = "Normal",
                                type = StructElement.Type.UNION,
                                fields = listOf(
                                        FieldElement(location.at(2, 3),
                                                2,
                                                ScalarTypeElement(location.at(2, 6), "i16"),
                                                "foo",
                                                Requiredness.DEFAULT),
                                        FieldElement(location.at(3, 3),
                                                4,
                                                ScalarTypeElement(location.at(3, 6), "i32"),
                                                "bar",
                                                Requiredness.DEFAULT)
                                )
                        )
                )
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun unionCannotHaveRequiredField() {
        val thrift = "\n" +
                "union Normal {\n" +
                "  3: optional i16 foo,\n" +
                "  5: required i32 bar\n" +
                "}\n"

        val e = shouldThrow<IllegalStateException> { parse(thrift, Location.get("", "unionWithRequired.thrift")) }
        e.message shouldContain "unions cannot have required fields"
    }

    @Test
    fun simpleConst() {
        val thrift = "const i64 DefaultStatusCode = 200"
        val location = Location.get("", "simpleConst.thrift")

        val expected = ThriftFileElement(
                location = location,
                constants = listOf(
                        ConstElement(
                                location = location.at(1, 1),
                                type = ScalarTypeElement(location.at(1, 7), "i64"),
                                name = "DefaultStatusCode",
                                value = IntValueElement(location.at(1, 31), "200", 200)
                        )
                )
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun veryLargeConst() {
        val thrift = "const i64 Yuuuuuge = 0xFFFFFFFFFF"
        val location = Location.get("", "veryLargeConst.thrift")

        val expected = ThriftFileElement(
                location,
                constants = listOf(
                        ConstElement(
                                location = location.at(1, 1),
                                type = ScalarTypeElement(location.at(1, 7), "i64", null),
                                name = "Yuuuuuge",
                                value = IntValueElement(
                                        location = location.at(1, 22),
                                        thriftText = "0xFFFFFFFFFF",
                                        value = 0xFFFFFFFFFFL
                                )
                        )
                )
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun listConst() {
        val thrift = "const list<string> Names = [\"foo\" \"bar\", \"baz\"; \"quux\"]"
        val location = Location.get("", "listConst.thrift")

        val element = ConstElement(
                location.at(1, 1),
                type = ListTypeElement(
                        location = location.at(1, 7),
                        elementType = ScalarTypeElement(location.at(1, 12), "string")),
                name = "Names",
                value = ListValueElement(
                        location = location.at(1, 28),
                        thriftText = "[\"foo\"\"bar\",\"baz\";\"quux\"]",
                        value = listOf(
                                LiteralValueElement(location.at(1, 29), "\"foo\"", "foo"),
                                LiteralValueElement(location.at(1, 35), "\"bar\"", "bar"),
                                LiteralValueElement(location.at(1, 42), "\"baz\"", "baz"),
                                LiteralValueElement(location.at(1, 49), "\"quux\"", "quux"))))

        val expected = ThriftFileElement(
                location = location,
                constants = listOf(element)
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun mapConst() {
        val thrift = "const map<string, string> Headers = {\n" +
                "  \"foo\": \"bar\",\n" +
                "  \"baz\": \"quux\";\n" +
                "}"

        val location = Location.get("", "mapConst.thrift")
        val mapConst = ConstElement(
                location = location.at(1, 1),
                type = MapTypeElement(
                        location = location.at(1, 7),
                        keyType = ScalarTypeElement(location.at(1, 11), "string", null),
                        valueType = ScalarTypeElement(location.at(1, 19), "string", null)),
                name = "Headers",
                value = MapValueElement(
                        location = location.at(1, 37),
                        thriftText = "{\"foo\":\"bar\",\"baz\":\"quux\";}",
                        value = mapOf(
                                LiteralValueElement(location.at(2, 3), "\"foo\"", "foo") to
                                LiteralValueElement(location.at(2, 10), "\"bar\"", "bar"),

                                LiteralValueElement(location.at(3, 3), "\"baz\"", "baz") to
                                        LiteralValueElement(location.at(3, 10), "\"quux\"", "quux"))))

        val expected = ThriftFileElement(
                location = location,
                constants = listOf(mapConst)
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun structFieldWithConstValue() {
        val thrift = "struct Foo {\n" +
                "  100: i32 num = 1\n" +
                "}"

        val location = Location.get("", "structWithConstValue.thrift")

        val expected = ThriftFileElement(
                location = location,
                structs = listOf(
                        StructElement(
                                location = location.at(1, 1),
                                name = "Foo",
                                type = StructElement.Type.STRUCT,
                                fields = listOf(
                                        FieldElement(
                                                location = location.at(2, 3),
                                                fieldId = 100,
                                                type = ScalarTypeElement(location.at(2, 8), "i32"),
                                                name = "num",
                                                constValue = IntValueElement(location.at(2, 18), "1", 1)
                                        )
                                )
                        )
                )
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun canParseOfficialTestCase() {
        val classLoader = javaClass.classLoader
        val stream = classLoader.getResourceAsStream("cases/TestThrift.thrift")
        val thrift = stream!!.source().buffer().readUtf8()
        val location = Location.get("cases", "TestThrift.thrift")

        // Not crashing is good enough here.  We'll be more strict with this file in the loader test.
        parse(thrift, location)
    }

    @Test
    fun bareEnums() {
        val thrift = "enum Enum {\n" +
                "  FOO,\n" +
                "  BAR\n" +
                "}"

        val location = Location.get("", "bareEnums.thrift")
        val expected = ThriftFileElement(
                location = location,
                enums = listOf(
                        EnumElement(
                                location = location.at(1, 1),
                                name = "Enum",
                                members = listOf(
                                        EnumMemberElement(location.at(2, 3), "FOO", 0),
                                        EnumMemberElement(location.at(3, 3), "BAR", 1))
                        )
                )
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun enumWithLargeGaps() {
        val thrift = "enum Gaps {\n" +
                "  SMALL = 10,\n" +
                "  MEDIUM = 100,\n" +
                "  ALSO_MEDIUM,\n" +
                "  LARGE = 5000\n" +
                "}"

        val location = Location.get("", "enumWithLargeGaps.thrift")
        val expected = ThriftFileElement(
                location = location,
                enums = listOf(
                        EnumElement(
                                location = location.at(1, 1),
                                name = "Gaps",
                                members = listOf(
                                        EnumMemberElement(location.at(2, 3), "SMALL", 10),
                                        EnumMemberElement(location.at(3, 3), "MEDIUM", 100),
                                        EnumMemberElement(location.at(4, 3), "ALSO_MEDIUM", 101),
                                        EnumMemberElement(location.at(5, 3), "LARGE", 5000)
                                )
                        )
                )
        )

        parse(thrift, location) shouldBe expected
    }

    @Test
    fun defaultValuesCanClash() {
        val thrift = "enum Enum {\n" +
                "  FOO = 5,\n" +
                "  BAR = 4,\n" +
                "  BAZ\n" +
                "}"

        val e = shouldThrow<IllegalStateException> { parse(thrift) }
        e.message shouldContain "duplicate enum value"
    }

    @Test
    fun annotationsOnNamespaces() {
        val thrift = "namespace java com.test (foo = 'bar')"
        val ns = parse(thrift).namespaces.single()
        val ann = ns.annotations!!

        ann["foo"] shouldBe "bar"
    }

    @Test
    fun annotationsOnTypedefs() {
        val thrift = "namespace java com.test\n" +
                "\n" +
                "typedef i32 StatusCode (boxed = 'false');"

        val typedef = parse(thrift).typedefs.single()
        val ann = typedef.annotations!!

        ann["boxed"] shouldBe "false"
    }

    @Test
    fun annotationsOnEnums() {
        val thrift = "enum Foo {} (bar = 'baz')"
        val enum = parse(thrift).enums.single()
        val ann = enum.annotations!!

        ann["bar"] shouldBe "baz"
    }

    @Test
    fun annotationsOnEnumMembers() {
        val thrift = "" +
                "enum Foo {\n" +
                "  BAR (bar = 'abar'),\n" +
                "  BAZ = 1 (baz = 'abaz'),\n" +
                "  QUX (qux = 'aqux')\n" +
                "  WOO\n" +
                "}"

        val enum = parse(thrift).enums.single()
        val (a1, a2, a3, a4) = enum.members.map { it.annotations }

        a1!!["bar"] shouldBe "abar"
        a2!!["baz"] shouldBe "abaz"
        a3!!["qux"] shouldBe "aqux"
        a4 shouldBe null
    }

    @Test
    fun annotationsOnServices() {
        val thrift = "" +
                "service Svc {" +
                "  void foo(1: i32 bar)" +
                "} (async = 'true', java.races = 'false')"
        val services = parse(thrift).services
        val ann = services.single().annotations!!

        ann.size shouldBe 2
        ann["async"] shouldBe "true"
        ann["java.races"] shouldBe "false"
    }

    @Test
    fun annotationsOnFunctions() {
        val thrift = "service Svc {\n" +
                "  void nothrow() (test = 'a'),\n" +
                "  void nosep() (test = 'b')\n" +
                "  i32 hasThrow() throws(1: string what) (test = 'c');\n" +
                "}"

        val services = parse(thrift).services
        val functions = services[0].functions

        val (a, b, c) = functions.map { it.annotations}

        a!!["test"] shouldBe "a"
        b!!["test"] shouldBe "b"
        c!!["test"] shouldBe "c"
    }

    @Test
    fun annotationsOnStructs() {
        val thrift = "" +
                "struct Str {\n" +
                "  1: i32 hi,\n" +
                "  2: optional string there,\n" +
                "} (layout = 'sequential')"

        val structs = parse(thrift).structs
        val ann = structs[0].annotations

        ann!!["layout"] shouldBe "sequential"
    }

    @Test
    fun annotationsOnUnions() {
        val thrift = "" +
                "union Union {\n" +
                "  1: i32 hi,\n" +
                "  2: optional string there,\n" +
                "} (layout = 'padded')"

        val unions = parse(thrift).unions
        val ann = unions[0].annotations

        ann!!["layout"] shouldBe "padded"
    }

    @Test
    fun annotationsOnExceptions() {
        val thrift = "" +
                "exception Exception {\n" +
                "  1: required i32 boom,\n" +
                "} (java.runtime_exception)"

        val exceptions = parse(thrift).exceptions
        val ann = exceptions[0].annotations

        ann!!["java.runtime_exception"] shouldBe "true"
    }

    @Test
    fun annotationsOnFields() {
        val thrift = "struct Str {\n" +
                "  1: i32 what (what = 'what'),\n" +
                "  2: binary data (compression = 'zlib') // doc\n" +
                "  3: optional i8 bits (synonym = 'byte')\n" +
                "}"

        val structs = parse(thrift).structs
        val fields = structs[0].fields
        val (a1, a2, a3) = fields.map { it.annotations!! }
        
        a1["what"] shouldBe "what"
        a2["compression"] shouldBe "zlib"
        a3["synonym"] shouldBe "byte"
    }

    @Test
    fun annotationsOnFieldTypes() {
        val thrift = "struct Str {\n" +
                "  1: map<string, i32> (python.immutable) foo\n" +
                "}"

        val structs = parse(thrift).structs
        val fields = structs[0].fields
        val anno = fields[0].type.annotations!!

        anno["python.immutable"] shouldBe "true"
    }

    @Test
    fun annotationsOnListElementTypes() {
        val thrift = """const list<i64 (type = "js")> TIMESTAMPS = [];"""
        val consts = parse(thrift).constants
        val tss = consts[0]
        val listType = tss.type as ListTypeElement
        val elementType = listType.elementType
        elementType.annotations?.get("type") shouldBe "js"
    }

    @Test
    fun annotationsOnConsecutiveDefinitions() {
        val thrift = "" +
                "namespace java com.foo.bar (ns = 'ok')\n" +
                "enum Foo {} (enumAnno = 'yep')\n" +
                ""

        parse(thrift)
    }

    @Test
    fun annotationsWithEscapedQuotesInValues() {
        val thrift = "" + "namespace java x (comment = \"what a \\\"mess\\\"\")\n"

        val namespaces = parse(thrift).namespaces
        val annotations = namespaces[0].annotations!!
        annotations["comment"] shouldBe "what a \"mess\""
    }

    @Test
    fun newlinesAreTricky() {
        // We must take care not to confuse the return type of the second
        // function with a possible 'throws' clause from the not-definitively-finished
        // first function.
        val thrift = "" +
                "typedef i32 typeof_int\n" +
                "service Stupid {\n" +
                "  i32 foo()\n" +
                "  typeof_int bar()\n" +
                "}"

        parse(thrift)
    }

    @Test
    fun fieldsWithoutSeparatorsDoNotConsumeNextFieldsDocumentation() {
        val thrift = "struct SomeRequest {\n" +
                "    /** Here's a comment. */\n" +
                "    1: required UUID clientUuid\n" +
                "\n" +
                "    /** Here's a longer comment. */\n" +
                "    2: optional string someOtherField\n" +
                "}"

        val structs = parse(thrift).structs
        val fields = structs[0].fields

        fields[0].documentation shouldBe "Here's a comment.\n"
        fields[1].documentation shouldBe "Here's a longer comment.\n"
    }

    @Test
    fun trailingDocWithoutSeparatorWithAnnotationOnNewLine() {
        val thrift = "struct SomeRequest {\n" +
                "    /** Here's a comment. */\n" +
                "    1: required UUID clientUuid\n" +
                "         (bork = \"bork\")  // this belongs to clientUuid\n" +
                "\n" +
                "    /**\n" +
                "     * Here's a longer comment.\n" +
                "     * One two lines.\n" +
                "     */\n" +
                "    2: optional string someOtherField\n" +
                "}"

        val structs = parse(thrift).structs
        val fields = structs[0].fields

        fields[0].documentation shouldContain "this belongs to clientUuid"
        fields[1].documentation shouldContain "Here's a longer comment."
    }

    @Test
    fun enumJavadocWithoutSeparators() {
        val thrift = "/**\n" +
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
                "}"

        val loc = Location.get("foo", "bar.thrift")
        val expected = EnumElement(
                location = loc.at(4, 1),
                name = "Value",
                members = listOf(
                        EnumMemberElement(loc.at(8, 5), "FIRST", 0, "This is not trailing doc.\n"),
                        EnumMemberElement(loc.at(12, 5), "SECOND", 1, "Neither is this.\n")
                ),
                documentation = "Some Javadoc\n")

        val enums = parse(thrift, loc).enums

        enums[0] shouldBe expected
    }

    @Test
    fun structsCanOmitAndReorderFieldIds() {
        val file = parse("" +
                "struct Struct {\n" +
                "  required string foo;\n" +
                "  required string bar;\n" +
                "  5: required string baz\n" +
                "  required string qux;\n" +
                "  4: required string barfo\n" +
                "  required string beefy\n" +
                "}")

        val structs = file.structs
        val fieldIds = structs[0].fields.map { it.fieldId }
        fieldIds shouldBe listOf(1, 2, 5, 6, 4, 7)
    }

    @Test
    fun commentsThatAreEmptyDoNotCrash() {
        val file = parse("" +
                "//\n" +
                "const i32 foo = 2")
        val constants = file.constants
        val documentation = constants[0].documentation
        documentation shouldBe ""
    }

    @Test
    fun `double-valued consts can have integer literal values`() {
        val thrift = """
            const double foo = 2
        """.trimIndent()

        val constants = parse(thrift).constants
        (constants.single().value as IntValueElement).value shouldBe 2L
    }

    @Test
    fun `jvm namespaces fall back`() {
        val thrift = """
            const double foo = 2
        """.trimIndent()

        val constants = parse(thrift).constants
        (constants.single().value as IntValueElement).value shouldBe 2L
    }

    @Test
    fun `jvm namespaces fall back to JVM declaration`() {
        val thrift = "namespace jvm com.microsoft.thrifty.parser"

        val location = Location.get("", "namespaces.thrift")
        val file = parse(thrift, location)

        val expected = ThriftFileElement(
            location = location,
            namespaces = listOf(
                NamespaceElement(
                    location = location.at(1, 1),
                    scope = NamespaceScope.JVM,
                    namespace = "com.microsoft.thrifty.parser"
                ),
                NamespaceElement(
                    location = location.at(1, 1),
                    scope = NamespaceScope.JAVA,
                    namespace = "com.microsoft.thrifty.parser"
                ),
                NamespaceElement(
                    location = location.at(1, 1),
                    scope = NamespaceScope.KOTLIN,
                    namespace = "com.microsoft.thrifty.parser"
                )
            )
        )

        file shouldBe expected
    }

    @Test
    fun `jvm namespaces fall back to JVM declaration but not if specified`() {
        val thrift = "namespace jvm com.microsoft.thrifty.parser\nnamespace java com.microsoft.thrifty.parser.java\n"

        val location = Location.get("", "namespaces.thrift")
        val file = parse(thrift, location)

        val expected = ThriftFileElement(
            location = location,
            namespaces = listOf(
                NamespaceElement(
                    location = location.at(1, 1),
                    scope = NamespaceScope.JVM,
                    namespace = "com.microsoft.thrifty.parser"
                ),
                NamespaceElement(
                    location = location.at(2, 1),
                    scope = NamespaceScope.JAVA,
                    namespace = "com.microsoft.thrifty.parser.java"
                ),
                NamespaceElement(
                    location = location.at(1, 1),
                    scope = NamespaceScope.KOTLIN,
                    namespace = "com.microsoft.thrifty.parser"
                )
            )
        )

        file shouldBe expected
    }

    @Test
    fun `top-level semicolons are a syntax error`() {
        val thrift = "struct Empty {};"

        val ex = shouldThrowExactly<IllegalStateException> { parse(thrift) }
        ex.message should startWith("Syntax error")
        ex.message.shouldContain("extraneous input ';'") // reenable when we have improved syntax error reporting
    }

    companion object {

        private val TEST_UUID = UUID.fromString("ecafa042-668a-4403-a6d3-70983866ffbe")

        private fun parse(thrift: String, location: Location = Location.get("", "")): ThriftFileElement {
            return ThriftParser.parse(location, thrift, ErrorReporter())
        }
    }

}
