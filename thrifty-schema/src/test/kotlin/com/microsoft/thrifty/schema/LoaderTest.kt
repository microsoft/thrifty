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

import com.microsoft.thrifty.schema.parser.*
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.assertions.throwables.shouldThrowMessage
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.matchers.types.beInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LoaderTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun basicTest() {
        val thrift = """
            namespace java com.microsoft.thrifty.test

            enum TestEnum {
              ONE = 1,
              TWO = 2
            }

            typedef i32 Int

            struct S {
              1: required Int n
            }

            service Svc {
              oneway void sayHello(1: S arg1)
            }
        """

        val schema = load(thrift)

        schema.enums.size shouldBe 1
        schema.structs.size shouldBe 1
        schema.services.size shouldBe 1

        val et = schema.enums[0]
        et.name shouldBe "TestEnum"
        et.members[0].name shouldBe "ONE"
        et.members[1].name shouldBe "TWO"

        val st = schema.structs[0]
        st.name shouldBe "S"
        st.fields.size shouldBe 1

        val field = st.fields[0]
        field.id shouldBe 1
        field.required shouldBe true
        field.name shouldBe "n"

        val fieldType = field.type
        fieldType.isTypedef shouldBe true
        fieldType.name shouldBe "Int"
        fieldType.trueType shouldBe BuiltinType.I32

        val svc = schema.services[0]
        svc.name shouldBe "Svc"
        svc.methods.size shouldBe 1

        val method = svc.methods[0]
        method.name shouldBe "sayHello"
        method.oneWay shouldBe true
        method.parameters.size shouldBe 1
        method.exceptions.size shouldBe 0

        val param = method.parameters[0]
        param.name shouldBe "arg1"
        param.type.name shouldBe "S"
        param.type shouldBe st
    }

    @Test
    fun oneInclude() {
        val included = """
            namespace java com.microsoft.thrifty.test.include

            enum TestEnum {
              ONE,
              TWO
            }
        """

        val f = File(tempDir, "toInclude.thrift")
        f.writeText(included)

        val thrift = """
            namespace java com.microsoft.thrifty.test.include
            include '${f.name}'

            typedef ${f.nameWithoutExtension}.TestEnum Ordinal

            struct TestStruct {
              1: ${f.nameWithoutExtension}.TestEnum foo
            }
        """

        val f1 = File.createTempFile("test", ".thrift", tempDir)
        f1.writeText(thrift)

        val schema = load(f, f1)

        val et = schema.enums[0]
        et.name shouldBe "TestEnum"

        val td = schema.typedefs[0]
        td.oldType shouldBe et
    }

    @Test
    fun includedTypesMustBeScoped() {
        val f = File(tempDir, "toInclude.thrift")
        val f1 = File.createTempFile("test", ".thrift", tempDir)

        val included = """
            namespace java com.microsoft.thrifty.test.scopedInclude

            enum TestEnum {
              ONE,
              TWO
            }
        """

        val thrift = """
            namespace java com.microsoft.thrifty.test.scopedInclude
            include '${f.name}'

            struct TestStruct {
              1: TestEnum foo
            }
        """

        f.writeText(included)
        f1.writeText(thrift)

        val e = shouldThrow<LoadFailedException> { load(f, f1) }
        assertHasError(e, "Failed to resolve type 'TestEnum'")
    }

    @Test
    fun includedConstants() {
        val producer = File(tempDir, "p.thrift")
        val consumer = File(tempDir, "c.thrift")

        val producerThrift = "const i32 foo = 10"

        val consumerThrift = """
            include 'p.thrift'

            struct Bar {
              1: required i32 field = p.foo
            }
        """

        producer.writeText(producerThrift)
        consumer.writeText(consumerThrift)

        load(producer, consumer)
    }

    @Test
    fun includedConstantsMustBeScoped() {
        val producer = File(tempDir, "p.thrift")
        val consumer = File(tempDir, "c.thrift")

        val producerThrift = "const i32 foo = 10"

        val consumerThrift = """
            include 'p.thrift'

            struct Bar {
              1: required i32 field = foo
            }
        """

        producer.writeText(producerThrift)
        consumer.writeText(consumerThrift)

        val e = shouldThrow<LoadFailedException> { load(producer, consumer) }
        assertHasError(e, "Unrecognized const identifier")
    }

    @Test
    fun crazyIncludes() {
        val nestedDir = File(tempDir, "nested").apply { mkdir() }
        val f1 = File(nestedDir, "a.thrift")
        val f2 = File(tempDir, "b.thrift")
        val f3 = File(tempDir, "c.thrift")

        val a = """
            namespace java com.microsoft.thrifty.test.crazyIncludes

            enum A {
              ONE, TWO, THREE
            }
        """

        val b = """
            include 'nested/a.thrift'
            namespace java com.microsoft.thrifty.test.crazyIncludes

            struct B {
              1: a.A a = a.A.ONE
            }
        """

        val c = """
            include 'b.thrift'

            namespace java com.microsoft.thrifty.test.crazyIncludes

            struct C {
              100: required b.B b,
            }
        """

        f1.writeText(a)
        f2.writeText(b)
        f3.writeText(c)

        val loader = Loader()
        loader.addIncludePath(tempDir.toPath())

        val schema = loader.load()

        schema.structs shouldHaveSize 2
        schema.enums shouldHaveSize 1

        val enum = schema.enums.single()
        enum.location.path shouldBe listOf("nested", "a.thrift").joinToString(File.separator)
    }

    @Test
    fun crazyIncludeReferencedConst() {
        val nestedDir = File(tempDir, "nested").apply { mkdir() }
        val f1 = File(nestedDir, "a.thrift")
        val f2 = File(tempDir, "another_a.thrift")
        val f3 = File(tempDir, "b.thrift")

        val a = """
            namespace java com.microsoft.thrifty.test.crazyIncludeReferencedConst
            
            const string HELLO = "hello"
        """

        val b = """
            namespace java com.microsoft.thrifty.test.crazyIncludeReferencedConst
            
            const string HELLO = "actually goodbye"
        """

        val c = """
            include 'another_a.thrift'
            include 'nested/a.thrift'
            namespace java com.microsoft.thrifty.test.crazyIncludeReferencedConst
            
            const string HELLO_AGAIN = a.HELLO
        """

        f1.writeText(a)
        f2.writeText(b)
        f3.writeText(c)

        val loader = Loader()
        loader.addIncludePath(tempDir.toPath())

        val schema = loader.load()

        val helloAgain = schema.constants.single { const -> const.name == "HELLO_AGAIN" }
        val referencedConstant = helloAgain.referencedConstants.single()
        referencedConstant.location.path shouldBe listOf("nested", "a.thrift").joinToString(File.separator)
    }

    @Test
    fun relativeIncludesConsiderIncludingFileLocation() {
        val thriftDir = File(tempDir, "thrift").apply { mkdir() }
        val barDir = File(thriftDir, "bar").apply { mkdir() }
        val f1 = File(barDir, "foo.thrift")
        val f2 = File(barDir, "bar.thrift")

        val foo = """
            namespace jvm test.includes.relative

            enum A {
              ONE, TWO, THREE
            }
        """

        val bar = """
            include 'foo.thrift'
            namespace jvm test.includes.relative

            struct B {
              1: foo.A a = foo.A.ONE
            }
        """

        f1.writeText(foo)
        f2.writeText(bar)

        val schema = load(f2)
        val enum = schema.enums.single()
        enum.location.path shouldBe "foo.thrift"
    }

    @Test
    fun circularInclude() {
        val f1 = File(tempDir, "A")
        val f2 = File(tempDir, "B")
        val f3 = File(tempDir, "C")
        val f4 = File(tempDir, "D")

        f1.writeText("include '${f2.name}'")
        f2.writeText("include '${f3.name}'")
        f3.writeText("include '${f4.name}'")
        f4.writeText("include '${f2.name}'")

        val e = shouldThrow<LoadFailedException> { load(f1, f2, f3, f4) }
        assertHasError(e, "Circular include; file includes itself transitively B -> D -> C -> B")
    }

    @Test
    fun circularTypedefs() {
        val thrift = """
            typedef A B
            typedef B A
        """

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        assertHasError(e, "Unresolvable typedef")
    }

    @Test
    fun containerTypedefs() {
        val thrift = """
            typedef i32 StatusCode
            typedef string Message
            typedef map<StatusCode, Message> Messages
        """

        val schema = load(thrift)

        val code = schema.typedefs[0]
        val msg = schema.typedefs[1]
        val map = schema.typedefs[2]

        code.name shouldBe "StatusCode"
        code.oldType.isBuiltin shouldBe true
        code.oldType.name shouldBe "i32"

        msg.name shouldBe "Message"
        msg.oldType.isBuiltin shouldBe true
        msg.oldType.name shouldBe "string"

        map.name shouldBe "Messages"
        map.oldType.isMap shouldBe true

        val mt = map.oldType as MapType
        mt.keyType shouldBe code
        mt.valueType shouldBe msg
    }

    @Test
    fun crazyNesting() {
        val thrift = """
            namespace java com.microsoft.thrifty.compiler.testcases

            typedef string EmailAddress

            struct Strugchur {
              1: required map<EmailAddress, ReceiptStatus> data = {"foo@bar.com": 0, "baz@quux.com": ReceiptStatus.READ}
              2: required list<map<EmailAddress, set<ReceiptStatus>>> crazy = [{"ben@thrifty.org": [ReceiptStatus.UNSENT, ReceiptStatus.SENT]}]
            }

            enum ReceiptStatus {
              UNSENT,
              SENT,
              READ
            }
        """

        val schema = load(thrift)

        schema.structs[0].fields.size shouldBe 2
    }

    @Test
    fun missingType() {
        val thrift = """
            struct Nope {
              1: required list<Undefined> nope
            }
        """

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        assertHasError(e, "Failed to resolve type 'Undefined'")
    }

    @Test
    fun circularFieldReferences() {
        val thrift = """
            struct A {
              1: optional B b;
            }

            struct B {
              1: optional A a;
            }
        """

        load(thrift)
    }

    @Test
    fun canLoadAndLinkOfficialTestThrift() {
        val url = javaClass.classLoader.getResource("cases/TestThrift.thrift")!!
        val file = File(url.file)

        val loader = Loader()
        loader.addThriftFile(file.toPath())

        loader.load()
    }

    @Test
    fun includesWithRelativePaths() {
        val dir = File(tempDir, "b").apply { mkdir() }
        val f1 = File(tempDir, "a.thrift")
        val f2 = File(dir, "b.thrift")

        val a = """
            namespace java com.microsoft.thrifty.test.includesWithRelativePaths

            enum A {
              ONE, TWO, THREE
            }
        """

        val b = """include '../a.thrift'

            namespace java com.microsoft.thrifty.test.includesWithRelativePaths

            struct B {
              1: a.A a = a.A.ONE
            }
        """

        f1.writeText(a)
        f2.writeText(b)

        load(f1, f2)
    }

    @Test
    fun typedefsWithAnnotations() {
        val thrift = """
            namespace java typedef.annotations

            typedef i64 (js.type = "Date") Date
        """

        val schema = load(thrift)

        val td = schema.typedefs[0]
        td.oldType.annotations["js.type"] shouldBe "Date"
    }

    @Test
    fun annotationsOnFieldType() {
        val thrift = """
            namespace java struct.field.annotations

            const set<i32> NUMZ = [1];

            struct Foo {
              1: required set<i32> (thrifty.test = "bar") nums;
            }
        """

        val schema = load(thrift)
        val struct = schema.structs[0]
        val field = struct.fields[0]

        field.type.annotations["thrifty.test"] shouldBe "bar"
    }

    @Test
    fun serviceThatExtendsServiceIsValid() {
        val thrift = """
            namespace java service.valid

            service Base {
              void foo()
            }

            service Derived extends Base {
              void bar()
            }
        """

        val schema = load(thrift)
        val base = schema.services[0]
        val derived = schema.services[1]

        base.name shouldBe "Base"
        derived.name shouldBe "Derived"

        base shouldBe derived.extendsService
    }

    @Test
    fun serviceWithDuplicateMethodsIsInvalid() {
        val thrift = """
            namespace java service.invalid

            service Svc {
              void test()
              void test(1: string msg)
            }
        """

        shouldThrow<LoadFailedException> { load(thrift) }
    }

    @Test
    fun serviceWithDuplicateMethodFromBaseServiceIsInvalid() {
        val thrift = """
            namespace java service.invalid

            service Base {
              void test()
            }

            service Derived extends Base {
              void test()
            }
        """

        shouldThrow<LoadFailedException> { load(thrift) }
    }

    @Test
    fun serviceMethodWithDuplicateFieldIdsIsInvalid() {
        val thrift = """
            namespace java service.invalid

            service NoGood {
              void test(1: i32 foo; 1: i64 bar)
            }
        """

        shouldThrow<LoadFailedException> { load(thrift) }
    }

    @Test
    fun serviceThatExtendsNonServiceIsInvalid() {
        val thrift = """
            namespace java service.invalid

            service Foo extends i32 {
            }
        """

        shouldThrow<LoadFailedException> { load(thrift) }
    }

    @Test
    fun onewayVoidMethodIsValid() {
        val thrift = """
            namespace java service.oneway.valid

            service OnewayService {
              oneway void test();
            }
        """

        val schema = load(thrift)
        val service = schema.services[0]
        val method = service.methods[0]

        method.oneWay shouldBe true
        method.returnType shouldBe BuiltinType.VOID
    }

    @Test
    fun onewayMethodWithReturnTypeIsInvalid() {
        val thrift = """
            namespace java service.oneway.valid

            service OnewayService {
              oneway i32 test();
            }
        """

        shouldThrow<LoadFailedException> { load(thrift) }
    }

    @Test
    fun onewayMethodWithThrowsIsInvalid() {
        val thrift = """
            namespace java service.oneway.valid

            exception MyError {
              1: i32 what
              2: string why
            }

            service OnewayService {
              oneway void test() throws (1: MyError error);
            }
        """

        shouldThrow<LoadFailedException> { load(thrift) }
    }

    @Test
    fun throwsClauseWithNonExceptionBuiltinTypeIsInvalid() {
        val thrift = """
            namespace java service.throws.invalid

            service ThrowsStrings {  void test() throws (1: string not_an_exception)
            }
        """

        shouldThrow<LoadFailedException> { load(thrift) }
    }

    @Test
    fun throwsClauseWithListTypeIsInvalid() {
        val thrift = """
            namespace java service.throws.invalid

            service ThrowsList {
              void test() throws (1: list<i32> nums)
            }
        """

        shouldThrow<LoadFailedException> { load(thrift) }
    }

    @Test
    fun throwsClauseWithDuplicateFieldIdsIsInvalid() {
        val thrift = """
            namespace java service.throws.invalid

            exception Foo {}

            exception Bar {}

            service DuplicateExnIds {
              void test() throws (1: Foo foo, 1: Bar bar)
            }
        """

        shouldThrow<LoadFailedException> { load(thrift) }
    }

    @Test
    fun throwsClauseWithNonExceptionUserTypeIsInvalid() {
        val thrift = """
            namespace java service.throws.invalid

            struct UserType {
              1: string foo
            }

            service OnewayService {
              void test() throws (1: UserType also_not_an_exception);
            }
        """

        shouldThrow<LoadFailedException> { load(thrift) }
    }

    @Test
    fun throwsClauseWithExceptionTypeIsValid() {
        val thrift = """
            namespace java service.throws.valid

            exception UserType {
              1: string foo
            }

            service OnewayService {
              void test() throws (1: UserType error);
            }
        """

        val schema = load(thrift)
        val struct = schema.exceptions[0]

        struct.isException shouldBe true

        val service = schema.services[0]
        val method = service.methods[0]
        val field = method.exceptions[0]
        val type = field.type

        type shouldBe struct
    }

    @Test
    fun circularInheritanceDetected() {
        val thrift = """
            namespace java thrifty.services

            service A extends B {}

            service B extends C {}

            service C extends A {}
        """

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        e.message shouldContain "A -> B -> C -> A"
    }

    @Test
    fun constAndTypeWithSameName() {
        val thrift = """
            namespace java thrifty.constants

            const Foo Foo = Foo.BAR;

            enum Foo {
              BAR,
              BAZ
            }

            struct FooBar {
              1: required Foo Foo = Foo,
              2: required Foo Bar = Foo.BAR,
            }
        """

        load(thrift)
    }

    @Test
    fun booleanConstValidation() {
        val thrift = """
            namespace java thrifty.constants

            const bool B = true;

            struct Foo {
              1: required bool value = B
            }
        """

        load(thrift)
    }

    @Test
    fun typedefOfBooleanConstValidation() {
        // TODO: Does this actually work in the Apache compiler?
        val thrift = """
            namespace java thrifty.constants

            typedef bool MyBool
            const MyBool B = true;

            struct Foo {
              1: required bool value = B
            }
        """

        load(thrift)
    }

    @Test
    fun importedEnumConstantValue() {
        val imported = """
            namespace java src

            enum Foo {
              One = 1
            }
        """

        val target = """
            namespace java target

            include 'src.thrift'

            struct Bar {
              1: required src.Foo foo = Foo.One
              3: required src.Foo baz = 1
              4: required src.Foo quux = src.Foo.One
            }
        """

        val src = File(tempDir, "src.thrift")
        val dest = File(tempDir, "dest.thrift")

        src.writeText(imported)
        dest.writeText(target)

        load(src, dest)
    }

    @Test
    fun bareEnumDoesNotPassValidation() {
        val thrift = """
            enum Foo {
              One
            }

            struct Bar {
              1: required Foo foo = One
            }
        """

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        assertHasError(e, "Unqualified name 'One' is not a valid enum constant")
    }

    @Test
    fun addingNonExistentFileThrows() {
        val folder = File(tempDir, "testDir").toPath()
        val doesNotExist = folder.resolve("nope.thrift")
        val loader = Loader()

        val e = shouldThrow<IllegalArgumentException> { loader.addThriftFile(doesNotExist) }
        e shouldHaveMessage "thrift file must be a regular file"
    }

    @Test
    fun addingNonExistentIncludeDirectoryThrows() {
        val doesNotExist = File(tempDir, "notCreatedYet").toPath()
        val loader = Loader()
        shouldThrowMessage("path must be a directory") { loader.addIncludePath(doesNotExist) }
    }

    @Test
    fun loadingWithoutFilesOrIncludesThrows() {
        val loader = Loader()
        val e = shouldThrow<LoadFailedException> { loader.load() }
        e.cause!! shouldHaveMessage "No files and no include paths containing Thrift files were provided"
    }

    @Test
    fun loadingProgramWithDuplicateSymbolsThrows() {
        val thrift = """
            namespace java duplicateSymbols;

            struct Foo {
              1: required string foo;
            }

            enum Foo {
              FOO
            }
        """

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        e.message shouldContain "Duplicate symbols: Foo defined at"
    }

    @Test
    fun loadingProgramWithDuplicatedConstantNamesThrows() {
        val thrift = """
            namespace java duplicateConstants;

            const i32 Foo = 10

            const string Foo = 'bar'
        """

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        e.message shouldContain "Duplicate symbols: Foo defined at"
    }

    @Test
    fun constRefToConstantList() {
        val thrift = """
            const list<i32> SOME_LIST = [1, 2, 3, 4, 5];

            struct Sample {
              1: required list<i32> value = SOME_LIST;
            }
        """

        load(thrift)
    }

    @Test
    fun constRefToConstantListOfIncorrectType() {
        val thrift = """
            const list<i32> SOME_LIST = [1, 2, 3, 4, 5];

            struct Incorrect {
              1: required list<string> value = SOME_LIST;
            }
        """

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        e.message shouldContain "Expected a value with type list<string>"
    }

    @Test
    fun nonListConstRefOnListTypeThrows() {
        val thrift = """
            struct Nope {
              1: required set<binary> bytes = 3.14159;
            }
        """

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        e.message shouldContain "Expected a list literal, got: 3.14159"
    }

    @Test
    fun constRefToConstantMap() {
        val thrift = """
            const map<i8, i8> MAP = {1: 2}

            struct ByteMapHolder {
              1: required map<i8, i8> theMap = MAP;
            }
        """

        load(thrift)
    }

    @Test
    fun constRefToConstMapOfIncorrectTypeThrows() {
        val thrift = """
            const map<i16, i16> MAP = {3: 4}

            struct ByteMapHolder {
              1: required map<i8, i8> theMap = MAP;
            }
        """

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        e.message shouldContain "Expected a value with type map<i8, i8>"
    }

    @Test
    fun addIncludeFileSmokeTest() {
        val thriftFile = File(tempDir, "example.thrift")
        thriftFile.writeText("""
            namespace java example

            typedef string Uuid
        """)

        val schema = Loader()
                .addIncludePath(tempDir.toPath())
                .load()

        schema.typedefs shouldHaveSize 1
        schema.typedefs[0].name shouldBe "Uuid"
    }

    @Test
    fun constRefToNonMapTypeThrows() {
        val thrift = """
            struct StillNope {
              1: required map<binary, binary> cache = 'foo';
            }
        """

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        e.message shouldContain "Expected a map literal, got: foo"
    }

    @Test
    fun constDoubleWithIntLiteral() {
        val thrift = """
            const double FOO = 2
        """.trimIndent()

        load(thrift)
    }

    @Test
    fun constantsWithCircularReferences() {
        val thrift = """
            struct Node {
                1: required string key;
                2: optional Node value;
            }
            
            const Node A = {
                "key": "foo",
                "value": B
            }
            
            const Node B = {
                "key": "bar",
                "value": A
            }
        """.trimIndent()

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        e.message shouldContain "Cycle detected while validating Thrift constants"
    }

    @Test
    fun constantCycleWithMutuallyDependentStructs() {
        val thrift = """
            struct TweedleDee {
              1: TweedleDum brother;
            }
            
            struct TweedleDum {
              1: TweedleDee brother;
            }
            
            const TweedleDee TWEEDLE_DEE = { "brother": TWEEDLE_DUM }
            const TweedleDum TWEEDLE_DUM = { "brother": TWEEDLE_DEE }
        """.trimIndent()

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        e.message shouldContain "Cycle detected while validating Thrift constants"
    }

    @Test
    fun populatesReferencedConstants() {
        val thrift = """
            const string A = "a";
            const string B = "b";
            const list<string> STRS = [A, B];
        """.trimIndent()

        val schema = load(thrift)
        val strs = schema.constants.last()
        strs.referencedConstants.map { it.name} shouldBe listOf("A", "B")
    }

    @Test
    fun constantInMapKey() {
        val thrift = """
            const string KEY = "foo"
            const map<string, string> MAP = {
                KEY: "bar"
            }
        """.trimIndent()

        val schema = load(thrift)
        val (key, map) = schema.constants
        map.referencedConstants shouldBe listOf(key)
    }

    @Test
    fun topologicallySortsConstants() {
        val thrift = """
            struct Node {
              1: required list<Node> n;
            }
            
            const Node A = { "n": [F] }
            const Node B = { "n": [A] }
            const Node C = { "n": [ ] }
            const Node D = { "n": [B, C] }
            const Node E = { "n": [C, F] }
            const Node F = { "n": [ ] }
        """.trimIndent()

        val schema = load(thrift)
        val order = schema.constants.map { it.name }
        order shouldBe listOf("F", "A", "C", "B", "E", "D")
    }

    @Test
    fun unionWithOneDefaultValue() {
        val thrift = """
            union HasDefault {
                1: i32 a = 1;
                2: i64 b;
                3: string c;
            }
        """.trimIndent()

        val schema = load(thrift)
        val union = schema.unions.single()
        union.fields[0].defaultValue shouldNotBe null
        union.fields[1].defaultValue shouldBe null
        union.fields[2].defaultValue shouldBe null
    }

    @Test
    fun unionWithMultipleDefaultValues() {
        val thrift = """
            union HasManyDefaults {
                1: i32 a = 1;
                2: i64 b = 2;
                3: string c;
            }
        """.trimIndent()

        shouldThrowExactly<LoadFailedException> { load(thrift) }
    }

    @Test
    fun structValuedConstant() {
        val thrift = """

            enum Membership {
                MEMBER = 0
                NON_MEMBER = 1
                UNDER_REVIEW = 2
            }
            
            struct Location {
                1: required string currencyCode
                2: required list<string> languages
                3: required Membership membership
            }
            
            struct Region {
                1: required list<Location> locations 
                2: optional bool isActive
            }

            const Region DEFAULT_REGION = {
                "locations": [{"currencyCode" : "USD", "languages" : ["English", "Spanish"], "membership" : Membership.MEMBER}],
                "isActive" : true
            }

        """.trimIndent()

        val schema = load(thrift)
        val constants = schema.constants
        constants.size shouldBe 1
        val const = constants[0]
        val map = const.value as MapValueElement
        map.value.entries.size shouldBe 2
        for ((key, value) in map.value.entries) {
            assertTrue(key is LiteralValueElement)
            val keyKey = (key as LiteralValueElement).value
            when(keyKey) {
                "locations" -> {
                    val keyVal = (value as ListValueElement).value
                    keyVal.size shouldBe 1
                    val locationMap = keyVal[0] as MapValueElement
                    locationMap.value.size shouldBe 3
                    for ((locKey, locVal) in locationMap.value.entries) {
                        when((locKey as LiteralValueElement).value) {
                            "currencyCode" -> {
                                assertTrue((locVal as LiteralValueElement).value == "USD")
                            }
                            "languages" -> {
                                val languages = (locVal as ListValueElement).value
                                languages.size shouldBe 2
                                for (language in languages) {
                                    val lang = (language as LiteralValueElement).value
                                    assertTrue(lang in listOf("English", "Spanish"))
                                }
                            }
                            "membership" -> {
                                assertTrue((locVal as IdentifierValueElement).value == "Membership.MEMBER")
                            }
                            else -> {
                                fail("Invalid const key; must be currencyCode, languages or membership")
                            }
                        }
                    }
                }
                "isActive" -> {
                    assertTrue((value as IdentifierValueElement).value == "true")
                }
                else -> {
                    fail("Invalid const key; must be either locations or isActive")
                }
            }
        }
    }

    @Test
    fun invalidStructValuedConstant() {
        val thrift = """
            
            struct Region {
                1: required string name 
                2: optional bool isActive
            }

            const UnknownRegion DEFAULT_REGION = {
                "name": "US",
                "nonActive" : true // field name does not exist in Region
            }

        """.trimIndent()

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        e.message shouldContain "Failed to resolve type 'UnknownRegion'"
    }

    @Test
    fun invalidKeyStructValuedConstant() {
        val thrift = """
            
            struct Region {
                1: required string name 
                2: optional bool isActive
            }

            const Region DEFAULT_REGION = {
                // 1 is not a string; invalid definition
                1 : "US",
                "isActive" : true
            }

        """.trimIndent()

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        e.message shouldContain "Region struct const keys must be string"
    }

    @Test
    fun invalidFieldStructValuedConstant() {
        val thrift = """
            
            struct Region {
                1: required string name 
                2: optional bool isActive
            }

            const Region DEFAULT_REGION = {
                "name": "US",
                "nonActive" : true // field name does not exist in Region
            }

        """.trimIndent()

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        e.message shouldContain "Region struct has no field nonActive"
    }

    @Test
    fun invalidEnumStructValuedConst() {
        val thrift = """

            enum Membership {
                MEMBER = 0
                NON_MEMBER = 1
                UNDER_REVIEW = 2
            }
            
            struct Location {
                1: required Membership membership
            }
            
            struct Region {
                1: required list<Location> locations 
            }

            // invalid Enum
            const Region DEFAULT_REGION = {
                "locations": [{"membership" : Membership.WRONG}]
            }

        """.trimIndent()

        val e = shouldThrow<LoadFailedException> { load(thrift) }
        e.message shouldContain "'Membership.WRONG' is not a member of enum type Membership: members=[MEMBER, NON_MEMBER, UNDER_REVIEW]"
    }

    @Test
    fun `struct const with reference to another const`() {
        val thrift = """
            |struct Example {
            |  1: required string text
            |}
            |
            |struct Container {
            |  1: required Example example
            |}
            |
            |const Example THE_EXAMPLE = {"text": "this is some text"}
            |
            |const Container THE_CONTAINER = {"example": THE_EXAMPLE}
        """.trimMargin()

        val schema = load(thrift)
        val consts = schema.constants
        val theContainer = consts.find { it.name == "THE_CONTAINER" } ?: error("Expected a constant named THE_CONTAINER")

        val elements = (theContainer.value as MapValueElement).value
        val singleValue = elements.values.single()
        singleValue should beInstanceOf<IdentifierValueElement>()

        (singleValue as IdentifierValueElement).value shouldBe "THE_EXAMPLE"
    }

    @Test
    fun `struct references are deduplicated`() {
        val thrift = """
            |const string STR = "foo"
            |
            |const list<string> STRS = [STR, STR]
        """.trimMargin()

        val schema = load(thrift)
        val (str, strs) = schema.constants

        strs.referencedConstants shouldBe listOf(str)
    }

    @Test
    fun `struct constants can omit fields with default values`() {
        val baseThrift = """
            |namespace java foo;
            |
            |struct Foo {
            |  1: required string TEXT;
            |  2: required string TEXT_WITH_DEFAULT = "foo";
            |  3: optional string OPTIONAL_TEXT;
            |}
        """.trimMargin()

        val goodThrift = """
            |namespace java bar;
            |
            |include "foo.thrift"
            |
            |const foo.Foo THE_FOO = {"TEXT": "some text"}
        """.trimMargin()

        val badThrift = """
            |namespace java baz;
            |
            |include "foo.thrift"
            |
            |const foo.Foo NOT_THE_FOO = {"OPTIONAL_TEXT": "t"}
        """.trimMargin()

        val baseFile = File(tempDir, "foo.thrift")
        val goodFile = File(tempDir, "good.thrift")
        val badFile = File(tempDir, "bad.thrift")

        baseFile.writeText(baseThrift)
        goodFile.writeText(goodThrift)
        badFile.writeText(badThrift)

        val err = shouldThrow<LoadFailedException> { load(baseFile, badFile) }
        err.message shouldContain "Some required fields are unset"


        shouldNotThrowAny { load(baseFile, goodFile) }
    }

    private fun load(thrift: String): Schema {
        val f = File.createTempFile("test", ".thrift", tempDir)
        f.writeText(thrift)

        val loader = Loader()
        loader.addThriftFile(f.toPath())
        return loader.load()
    }

    private fun load(vararg files: File): Schema {
        val loader = Loader()
        for (f in files) {
            loader.addThriftFile(f.toPath())
        }
        return loader.load()
    }

    private fun assertHasError(exception: LoadFailedException, expectedMessage: String) {
        if (exception.errorReporter.reports.none { it.message.contains(expectedMessage) }) {
            throw AssertionError("Expected a reported error containing '$expectedMessage'")
        }
    }

    private val File.nameWithoutExtension: String
        get() {
            require(isFile)
            val name = name
            val ix = name.lastIndexOf('.')
            return if (ix != -1) {
                name.substring(0, ix)
            } else {
                name
            }
        }
}
