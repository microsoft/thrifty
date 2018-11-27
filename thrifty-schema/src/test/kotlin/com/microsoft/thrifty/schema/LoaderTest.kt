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

import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasEntry
import org.hamcrest.Matchers.hasSize
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage
import org.junit.rules.TemporaryFolder
import java.io.File

class LoaderTest {
    @get:Rule
    val tempDir = TemporaryFolder()

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

        assertThat(schema.enums.size, equalTo(1))
        assertThat(schema.structs.size, equalTo(1))
        assertThat(schema.services.size, equalTo(1))

        val et = schema.enums[0]
        assertThat(et.name, equalTo("TestEnum"))
        assertThat(et.members[0].name, equalTo("ONE"))
        assertThat(et.members[1].name, equalTo("TWO"))

        val st = schema.structs[0]
        assertThat(st.name, equalTo("S"))
        assertThat(st.fields.size, equalTo(1))

        val field = st.fields[0]
        assertThat(field.id, equalTo(1))
        assertThat(field.required, equalTo(true))
        assertThat(field.name, equalTo("n"))

        val fieldType = field.type
        assertThat(fieldType.isTypedef, equalTo(true))
        assertThat(fieldType.name, equalTo("Int"))
        assertThat(fieldType.trueType, equalTo(BuiltinType.I32))

        val svc = schema.services[0]
        assertThat(svc.name, equalTo("Svc"))
        assertThat(svc.methods.size, equalTo(1))

        val method = svc.methods[0]
        assertThat(method.name, equalTo("sayHello"))
        assertThat(method.oneWay, equalTo(true))
        assertThat(method.parameters.size, equalTo(1))
        assertThat(method.exceptions.size, equalTo(0))

        val param = method.parameters[0]
        assertThat(param.name, equalTo("arg1"))
        assertThat(param.type.name, equalTo("S"))
        assertThat(param.type, equalTo<ThriftType>(st))
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

        val f = tempDir.newFile("toInclude.thrift")
        f.writeText(included)

        val thrift = """
            namespace java com.microsoft.thrifty.test.include
            include '${f.name}'

            typedef ${f.nameWithoutExtension}.TestEnum Ordinal

            struct TestStruct {
              1: ${f.nameWithoutExtension}.TestEnum foo
            }
        """

        val f1 = tempDir.newFile()
        f1.writeText(thrift)

        val schema = load(f, f1)

        val et = schema.enums[0]
        assertThat(et.name, equalTo("TestEnum"))

        val td = schema.typedefs[0]
        assertThat(td.oldType, equalTo<ThriftType>(et))
    }

    @Test
    fun includedTypesMustBeScoped() {
        val f = tempDir.newFile("toInclude.thrift")
        val f1 = tempDir.newFile()

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

        try {
            load(f, f1)
            fail()
        } catch (e: LoadFailedException) {
            assertHasError(e, "Failed to resolve type 'TestEnum'")
        }

    }

    @Test
    fun includedConstants() {
        val producer = tempDir.newFile("p.thrift")
        val consumer = tempDir.newFile("c.thrift")

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
        val producer = tempDir.newFile("p.thrift")
        val consumer = tempDir.newFile("c.thrift")

        val producerThrift = "const i32 foo = 10"

        val consumerThrift = """
            include 'p.thrift'

            struct Bar {
              1: required i32 field = foo
            }
        """

        producer.writeText(producerThrift)
        consumer.writeText(consumerThrift)

        try {
            load(producer, consumer)
            fail("Expected a LoadFailedException due to an unqualified use of an imported constant")
        } catch (e: LoadFailedException) {
            assertHasError(e, "Unrecognized const identifier")
        }
    }

    @Test
    fun crazyIncludes() {
        val nestedDir = tempDir.newFolder("nested")
        val f1 = File(nestedDir, "a.thrift")
        val f2 = tempDir.newFile("b.thrift")
        val f3 = tempDir.newFile("c.thrift")

        val a = """
            namespace java com.microsoft.thrifty.test.crazyIncludes

            enum A {
              ONE, TWO, THREE
            }
        """

        val b = """
            include '${f1.canonicalPath}'
            namespace java com.microsoft.thrifty.test.crazyIncludes

            struct B {
              1: a.A a = a.A.ONE
            }
        """

        val c = """
            include '${f2.canonicalPath}'

            namespace java com.microsoft.thrifty.test.crazyIncludes

            struct C {
              100: required b.B b,
            }
        """

        f1.writeText(a)
        f2.writeText(b)
        f3.writeText(c)

        val loader = Loader()
        loader.addIncludePath(nestedDir.toPath())
        loader.addThriftFile(f2.toPath())
        loader.addThriftFile(f3.toPath())

        val schema = loader.load()

        assertThat(schema.structs, hasSize(2))
        assertThat(schema.enums, hasSize(1))
    }

    @Test
    fun circularInclude() {
        val f1 = tempDir.newFile("A")
        val f2 = tempDir.newFile("B")
        val f3 = tempDir.newFile("C")
        val f4 = tempDir.newFile("D")

        f1.writeText("include '${f2.name}'")
        f2.writeText("include '${f3.name}'")
        f3.writeText("include '${f4.name}'")
        f4.writeText("include '${f2.name}'")

        try {
            load(f1, f2, f3, f4)
            fail("Circular includes should fail to load")
        } catch (e: LoadFailedException) {
            assertHasError(e, "Circular include; file includes itself transitively B -> D -> C -> B")
        }

    }

    @Test
    fun circularTypedefs() {
        val thrift = """
            typedef A B
            typedef B A
        """

        try {
            load(thrift)
            fail("Circular typedefs should fail to link")
        } catch (e: LoadFailedException) {
            assertHasError(e, "Unresolvable typedef")
        }

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

        assertThat(code.name, equalTo("StatusCode"))
        assertThat(code.oldType.isBuiltin, equalTo(true))
        assertThat(code.oldType.name, equalTo("i32"))

        assertThat(msg.name, equalTo("Message"))
        assertThat(msg.oldType.isBuiltin, equalTo(true))
        assertThat(msg.oldType.name, equalTo("string"))

        assertThat(map.name, equalTo("Messages"))
        assertThat(map.oldType.isMap, equalTo(true))

        val mt = map.oldType as MapType
        assertThat(mt.keyType, equalTo<ThriftType>(code))
        assertThat(mt.valueType, equalTo<ThriftType>(msg))
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

        assertThat(schema.structs[0].fields.size, equalTo(2))
    }

    @Test
    fun missingType() {
        val thrift = """
            struct Nope {
              1: required list<Undefined> nope
            }
        """

        try {
            load(thrift)
            fail()
        } catch (e: LoadFailedException) {
            assertHasError(e, "Failed to resolve type 'Undefined'")
        }

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
        tempDir.newFolder("b")
        val f1 = tempDir.newFile("a.thrift")
        val f2 = tempDir.newFile("b${File.separator}b.thrift")

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
        assertThat(td.oldType.annotations, hasEntry("js.type", "Date"))
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

        assertThat(field.type.annotations, hasEntry("thrifty.test", "bar"))
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

        assertThat(base.name, equalTo("Base"))
        assertThat(derived.name, equalTo("Derived"))

        assertThat(base, equalTo(derived.extendsService))
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

        try {
            load(thrift)
            fail("Services cannot have more than one method with the same name")
        } catch (e: LoadFailedException) {
            // good
        }
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

        try {
            load(thrift)
            fail("Service cannot override methods inherited from base services")
        } catch (e: LoadFailedException) {
            // good
        }
    }

    @Test
    fun serviceMethodWithDuplicateFieldIdsIsInvalid() {
        val thrift = """
            namespace java service.invalid

            service NoGood {
              void test(1: i32 foo; 1: i64 bar)
            }
        """

        try {
            load(thrift)
            fail("Methods having multiple parameters with the same ID are invalid")
        } catch (e: LoadFailedException) {
            // good
        }
    }

    @Test
    fun serviceThatExtendsNonServiceIsInvalid() {
        val thrift = """
            namespace java service.invalid

            service Foo extends i32 {
            }
        """

        try {
            load(thrift)
            fail("Service extending non-service types should not validate")
        } catch (expected: LoadFailedException) {
            // hooray
        }
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

        assertThat(method.oneWay, equalTo(true))
        assertThat(method.returnType, equalTo(BuiltinType.VOID))
    }

    @Test
    fun onewayMethodWithReturnTypeIsInvalid() {
        val thrift = """
            namespace java service.oneway.valid

            service OnewayService {
              oneway i32 test();
            }
        """

        try {
            load(thrift)
            fail("Oneway methods cannot have non-void return types")
        } catch (e: LoadFailedException) {
            // yay
        }
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

        try {
            load(thrift)
            fail("Oneway methods cannot throw exceptions")
        } catch (e: LoadFailedException) {
            // yay
        }
    }

    @Test
    fun throwsClauseWithNonExceptionBuiltinTypeIsInvalid() {
        val thrift = """
            namespace java service.throws.invalid

            service ThrowsStrings {  void test() throws (1: string not_an_exception)
            }
        """

        try {
            load(thrift)
            fail("Methods that declare throws of non-exception types are invalid")
        } catch (e: LoadFailedException) {
            // good
        }
    }

    @Test
    fun throwsClauseWithListTypeIsInvalid() {
        val thrift = """
            namespace java service.throws.invalid

            service ThrowsList {
              void test() throws (1: list<i32> nums)
            }
        """

        try {
            load(thrift)
            fail("Methods that declare throws of non-exception types are invalid")
        } catch (e: LoadFailedException) {
            // good
        }
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

        try {
            load(thrift)
            fail("Methods with multiple exceptions having the same ID are invalid")
        } catch (e: LoadFailedException) {
            // good
        }
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

        try {
            load(thrift)
            fail("Methods that declare throws of non-exception user types are invalid")
        } catch (e: LoadFailedException) {
            // good
        }
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

        assertThat(struct.isException, equalTo(true))

        val service = schema.services[0]
        val method = service.methods[0]
        val field = method.exceptions[0]
        val type = field.type

        assertThat(type, equalTo<ThriftType>(struct))
    }

    @Test
    fun circularInheritanceDetected() {
        val thrift = """
            namespace java thrifty.services

            service A extends B {}

            service B extends C {}

            service C extends A {}
        """

        try {
            load(thrift)
            fail("Service inheritance cannot form a cycle")
        } catch (e: LoadFailedException) {
            // Make sure that we identify the cycle
            assertThat<String>(e.message, containsString("A -> B -> C -> A"))
        }
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

        val src = tempDir.newFile("src.thrift")
        val dest = tempDir.newFile("dest.thrift")

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

        try {
            load(thrift)
            fail("Expected a LoadFailedException from validating a bare enum member in a constant")
        } catch (e: LoadFailedException) {
            assertHasError(e, "Unqualified name 'One' is not a valid enum constant")
        }
    }

    @Test
    fun addingNonExistentFileThrows() {
        val folder = tempDir.newFolder().toPath()
        val doesNotExist = folder.resolve("nope.thrift")
        val loader = Loader()
        try {
            loader.addThriftFile(doesNotExist)
            fail("Expected an IllegalArgumentException, but nothing was thrown")
        } catch (e: IllegalArgumentException) {
            assertThat(e, hasMessage(equalTo("thrift file must be a regular file")))
        }
    }

    @Test
    fun addingNonExistentIncludeDirectoryThrows() {
        val doesNotExist = tempDir.root.toPath().resolve("notCreatedYet")
        val loader = Loader()
        try {
            loader.addIncludePath(doesNotExist)
            fail("Expected an IllegalArgumentException, but nothing was thrown")
        } catch (e: IllegalArgumentException) {
            assertThat(e, hasMessage(equalTo("path must be a directory")))
        }
    }

    @Test
    fun loadingWithoutFilesOrIncludesThrows() {
        val loader = Loader()
        try {
            loader.load()
            fail("Expected a LoadFailedException, but nothing was thrown")
        } catch (expected: LoadFailedException) {
            val cause = expected.cause
            assertThat<Throwable>(
                    cause,
                    hasMessage(equalTo("No files and no include paths containing Thrift files were provided")))
        }
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

        try {
            load(thrift)
            fail()
        } catch (expected: LoadFailedException) {
            assertThat(expected, hasMessage(containsString("Duplicate symbols: Foo defined at")))
        }
    }

    @Test
    fun loadingProgramWithDuplicatedConstantNamesThrows() {
        val thrift = """
            namespace java duplicateConstants;

            const i32 Foo = 10

            const string Foo = 'bar'
        """

        try {
            load(thrift)
            fail()
        } catch (expected: LoadFailedException) {
            assertThat(expected, hasMessage(containsString("Duplicate symbols: Foo defined at")))
        }
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

        try {
            load(thrift)
            fail()
        } catch (expected: LoadFailedException) {
            assertThat(expected, hasMessage(containsString("Expected a value with type list<string>")))
        }
    }

    @Test
    fun nonListConstRefOnListTypeThrows() {
        val thrift = """
            struct Nope {
              1: required set<binary> bytes = 3.14159;
            }
        """

        try {
            load(thrift)
            fail()
        } catch (expected: LoadFailedException) {
            assertThat(expected, hasMessage(containsString("Expected a list literal, got: 3.14159")))
        }
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

        try {
            load(thrift)
            fail()
        } catch (expected: LoadFailedException) {
            assertThat(expected, hasMessage(containsString("Expected a value with type map<i8, i8>")))
        }
    }

    @Test
    fun addIncludeFileSmokeTest() {
        val thriftFile = tempDir.newFile("example.thrift")
        thriftFile.writeText("""
            namespace java example

            typedef string Uuid
        """)

        val schema = Loader()
                .addIncludePath(tempDir.root.toPath())
                .load()

        assertThat(schema.typedefs, hasSize(1))

        val typedef = schema.typedefs[0]
        assertThat(typedef.name, equalTo("Uuid"))
    }

    @Test
    fun constRefToNonMapTypeThrows() {
        val thrift = """
            struct StillNope {
              1: required map<binary, binary> cache = 'foo';
            }
        """

        try {
            load(thrift)
            fail()
        } catch (expected: LoadFailedException) {
            assertThat(expected, hasMessage(containsString("Expected a map literal, got: foo")))
        }
    }

    @Test
    fun constDoubleWithIntLiteral() {
        val thrift = """
            const double FOO = 2
        """.trimIndent()

        load(thrift)
    }

    private fun load(thrift: String): Schema {
        val f = tempDir.newFile()
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