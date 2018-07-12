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

import com.google.common.base.Joiner;
import okio.BufferedSink;
import okio.Okio;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

public class LoaderTest {
    @Rule public TemporaryFolder tempDir = new TemporaryFolder();

    @Test
    public void basicTest() throws Exception {
        String thrift = "\n" +
                "namespace java com.microsoft.thrifty.test\n" +
                "\n" +
                "enum TestEnum {\n" +
                "  ONE = 1,\n" +
                "  TWO = 2\n" +
                "}\n" +
                "\n" +
                "typedef i32 Int\n" +
                "\n" +
                "struct S {\n" +
                "  1: required Int n\n" +
                "}\n" +
                "\n" +
                "service Svc {\n" +
                "  oneway void sayHello(1: S arg1)\n" +
                "}";

        Schema schema = load(thrift);

        assertThat(schema.enums().size(), is(1));
        assertThat(schema.structs().size(), is(1));
        assertThat(schema.services().size(), is(1));

        EnumType et = schema.enums().get(0);
        assertThat(et.name(), is("TestEnum"));
        assertThat(et.members().get(0).name(), is("ONE"));
        assertThat(et.members().get(1).name(), is("TWO"));

        StructType st = schema.structs().get(0);
        assertThat(st.name(), is("S"));
        assertThat(st.fields().size(), is(1));

        Field field = st.fields().get(0);
        assertThat(field.id(), is(1));
        assertThat(field.required(), is(true));
        assertThat(field.name(), is("n"));

        ThriftType fieldType = field.type();
        assertThat(fieldType.isTypedef(), is(true));
        assertThat(fieldType.name(), is("Int"));
        assertThat(fieldType.getTrueType(), is(BuiltinType.I32));

        ServiceType svc = schema.services().get(0);
        assertThat(svc.name(), is("Svc"));
        assertThat(svc.methods().size(), is(1));

        ServiceMethod method = svc.methods().get(0);
        assertThat(method.name(), is("sayHello"));
        assertThat(method.oneWay(), is(true));
        assertThat(method.parameters().size(), is(1));
        assertThat(method.exceptions().size(), is(0));

        Field param = method.parameters().get(0);
        assertThat(param.name(), is("arg1"));
        assertThat(param.type().name(), is("S"));
        assertThat(param.type(), equalTo(st));
    }

    @Test
    public void oneInclude() throws Exception {
        String included = "\n" +
                "namespace java com.microsoft.thrifty.test.include\n" +
                "\n" +
                "enum TestEnum {\n" +
                "  ONE,\n" +
                "  TWO\n" +
                "}";

        File f = tempDir.newFile("toInclude.thrift");
        writeTo(f, included);

        String name = f.getName().split("\\.")[0];

        String thrift = "\n" +
                "namespace java com.microsoft.thrifty.test.include\n" +
                "include '" + f.getName() + "'\n" +
                "\n" +
                "typedef " + name + ".TestEnum Ordinal\n" +
                "\n" +
                "struct TestStruct {\n" +
                "  1: " + name + ".TestEnum foo\n" +
                "}";

        File f1 = tempDir.newFile();
        writeTo(f1, thrift);

        Schema schema = load(f, f1);

        EnumType et = schema.enums().get(0);
        assertThat(et.name(), is("TestEnum"));

        TypedefType td = schema.typedefs().get(0);
        assertThat(td.oldType(), equalTo(et));
    }

    @Test
    public void includedTypesMustBeScoped() throws Exception {
        File f = tempDir.newFile("toInclude.thrift");
        File f1 = tempDir.newFile();

        String included = "\n" +
                "namespace java com.microsoft.thrifty.test.scopedInclude\n" +
                "\n" +
                "enum TestEnum {\n" +
                "  ONE,\n" +
                "  TWO\n" +
                "}";

        String thrift = "\n" +
                "namespace java com.microsoft.thrifty.test.scopedInclude\n" +
                "include '" + f.getName() + "'\n" +
                "\n" +
                "struct TestStruct {\n" +
                "  1: TestEnum foo\n" +
                "}";

        writeTo(f, included);
        writeTo(f1, thrift);

        try {
            load(f, f1);
            fail();
        } catch (LoadFailedException e) {
            assertHasError(e, "Failed to resolve type 'TestEnum'");
        }
    }

    @Test
    public void includedConstants() throws Exception {
        File producer = tempDir.newFile("p.thrift");
        File consumer = tempDir.newFile("c.thrift");

        String producerThrift = "" +
                "const i32 foo = 10";

        String consumerThrift = "" +
                "include 'p.thrift'\n" +
                "\n" +
                "struct Bar {\n" +
                "  1: required i32 field = p.foo\n" +
                "}\n";

        writeTo(producer, producerThrift);
        writeTo(consumer, consumerThrift);

        load(producer, consumer);
    }

    @Test
    public void includedConstantsMustBeScoped() throws Exception {
        File producer = tempDir.newFile("p.thrift");
        File consumer = tempDir.newFile("c.thrift");

        String producerThrift = "" +
                "const i32 foo = 10";

        String consumerThrift = "" +
                "include 'p.thrift'\n" +
                "\n" +
                "struct Bar {\n" +
                "  1: required i32 field = foo\n" +
                "}\n";

        writeTo(producer, producerThrift);
        writeTo(consumer, consumerThrift);

        try {
            load(producer, consumer);
            fail("Expected a LoadFailedException due to an unqualified use of an imported constant");
        } catch (LoadFailedException e) {
            assertHasError(e, "Unrecognized const identifier");
        }
    }

    @Test
    public void crazyIncludes() throws Exception {
        File f1 = tempDir.newFile("a.thrift");
        File f2 = tempDir.newFile("b.thrift");
        File f3 = tempDir.newFile("c.thrift");

        String a = "namespace java com.microsoft.thrifty.test.crazyIncludes\n" +
                "\n" +
                "enum A {\n" +
                "  ONE, TWO, THREE\n" +
                "}";

        String b = "include '" + f1.getCanonicalPath() + "'\n" +
                "namespace java com.microsoft.thrifty.test.crazyIncludes\n" +
                "\n" +
                "struct B {\n" +
                "  1: a.A a = a.A.ONE\n" +
                "}";

        String c = "include '" + f2.getCanonicalPath() + "'\n" +
                "\n" +
                "namespace java com.microsoft.thrifty.test.crazyIncludes\n" +
                "\n" +
                "struct C {\n" +
                "  100: required b.B b,\n" +
                "}";

        writeTo(f1, a);
        writeTo(f2, b);
        writeTo(f3, c);

        Loader loader = new Loader();
        loader.addThriftFile(f1.toPath());
        loader.addThriftFile(f2.toPath());
        loader.addThriftFile(f3.toPath());

        loader.load();
    }

    @Test
    public void circularInclude() throws Exception {
        File f1 = tempDir.newFile();
        File f2 = tempDir.newFile();
        File f3 = tempDir.newFile();

        writeTo(f1, "include '" + f2.getName() + "'");
        writeTo(f2, "include '" + f3.getName() + "'");
        writeTo(f3, "include '" + f1.getName() + "'");

        try {
            load(f1, f2, f3);
            fail("Circular includes should fail to load");
        } catch (LoadFailedException e) {
            assertHasError(e, "Circular include");
        }
    }

    @Test
    public void circularTypedefs() throws Exception {
        String thrift = "" +
                "typedef A B\n" +
                "typedef B A";

        try {
            load(thrift);
            fail("Circular typedefs should fail to link");
        } catch (LoadFailedException e) {
            assertHasError(e, "Unresolvable typedef");
        }
    }

    @Test
    public void containerTypedefs() throws Exception {
        String thrift = "" +
                "typedef i32 StatusCode\n" +
                "typedef string Message\n" +
                "typedef map<StatusCode, Message> Messages";

        Schema schema = load(thrift);

        TypedefType code = schema.typedefs().get(0);
        TypedefType msg = schema.typedefs().get(1);
        TypedefType map = schema.typedefs().get(2);

        assertThat(code.name(), is("StatusCode"));
        assertThat(code.oldType().isBuiltin(), is(true));
        assertThat(code.oldType().name(), is("i32"));

        assertThat(msg.name(), is("Message"));
        assertThat(msg.oldType().isBuiltin(), is(true));
        assertThat(msg.oldType().name(), is("string"));

        assertThat(map.name(), is("Messages"));
        assertThat(map.oldType().isMap(), is(true));

        MapType mt = (MapType) map.oldType();
        assertThat(mt.keyType(), equalTo(code));
        assertThat(mt.valueType(), equalTo(msg));
    }

    @Test
    public void crazyNesting() throws Exception {
        String thrift = "namespace java com.microsoft.thrifty.compiler.testcases\n" +
                "\n" +
                "typedef string EmailAddress\n" +
                "\n" +
                "struct Strugchur {\n" +
                "  1: required map<EmailAddress, ReceiptStatus> data = {\"foo@bar.com\": 0, \"baz@quux.com\": ReceiptStatus.READ}\n" +
                "  2: required list<map<EmailAddress, set<ReceiptStatus>>> crazy = [{\"ben@thrifty.org\": [ReceiptStatus.UNSENT, ReceiptStatus.SENT]}]\n" +
                "}\n" +
                "\n" +
                "enum ReceiptStatus {\n" +
                "  UNSENT,\n" +
                "  SENT,\n" +
                "  READ\n" +
                "}";

        Schema schema = load(thrift);

        assertThat(schema.structs().get(0).fields().size(), is(2));
    }

    @Test
    public void missingType() throws Exception {
        String thrift = "" +
                "struct Nope {\n" +
                "  1: required list<Undefined> nope\n" +
                "}";

        try {
            load(thrift);
            fail();
        } catch (LoadFailedException e) {
            assertHasError(e, "Failed to resolve type 'Undefined'");
        }
    }

    @Test
    public void circularFieldReferences() throws Exception {
        String thrift = "" +
                "struct A {\n" +
                "  1: optional B b;\n" +
                "}\n" +
                "\n" +
                "struct B {\n" +
                "  1: optional A a;\n" +
                "}\n";

        load(thrift);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void canLoadAndLinkOfficialTestThrift() throws Exception {
        URL url = getClass().getClassLoader().getResource("cases/TestThrift.thrift");
        File file = new File(url.getFile());

        Loader loader = new Loader();
        loader.addThriftFile(file.toPath());

        loader.load();
    }

    @Test
    public void includesWithRelativePaths() throws Exception {
        tempDir.newFolder("b");
        File f1 = tempDir.newFile("a.thrift");
        File f2 = tempDir.newFile(Joiner.on(File.separatorChar).join("b", "b.thrift"));

        String a = "namespace java com.microsoft.thrifty.test.includesWithRelativePaths\n" +
                "\n" +
                "enum A {\n" +
                "  ONE, TWO, THREE\n" +
                "}";

        String b = "include '../a.thrift'\n" +
                "\n" +
                "namespace java com.microsoft.thrifty.test.includesWithRelativePaths\n" +
                "\n" +
                "struct B {\n" +
                "  1: a.A a = a.A.ONE\n" +
                "}";

        writeTo(f1, a);
        writeTo(f2, b);

        load(f1, f2);
    }

    @Test
    public void typedefsWithAnnotations() throws Exception {
        String thrift = "namespace java typedef.annotations\n" +
                "\n" +
                "typedef i64 (js.type = \"Date\") Date";

        Schema schema = load(thrift);

        TypedefType td = schema.typedefs().get(0);
        assertThat(td.oldType().annotations(), hasEntry("js.type", "Date"));
    }

    @Test
    public void annotationsOnFieldType() throws Exception {
        String thrift = "namespace java struct.field.annotations\n" +
                "\n" +
                // Ensure that the first time the linker sees set<i32>, it has no annotations
                "const set<i32> NUMZ = [1];\n" +
                "\n" +
                "struct Foo {\n" +
                "  1: required set<i32> (thrifty.test = \"bar\") nums;\n" +
                "}";

        Schema schema = load(thrift);
        StructType struct = schema.structs().get(0);
        Field field = struct.fields().get(0);

        assertThat(field.type().annotations(), hasEntry("thrifty.test", "bar"));
    }

    @Test
    public void serviceThatExtendsServiceIsValid() throws Exception {
        String thrift = "" +
                "namespace java service.valid\n" +
                "\n" +
                "service Base {\n" +
                "  void foo()\n" +
                "}\n" +
                "\n" +
                "service Derived extends Base {\n" +
                "  void bar()\n" +
                "}\n";

        Schema schema = load(thrift);
        ServiceType base = schema.services().get(0);
        ServiceType derived = schema.services().get(1);

        assertThat(base.name(), is("Base"));
        assertThat(derived.name(), is("Derived"));

        assertThat(base, equalTo(derived.extendsService()));
    }

    @Test
    public void serviceWithDuplicateMethodsIsInvalid() throws Exception {
        String thrift = "" +
                "namespace java service.invalid\n" +
                "\n" +
                "service Svc {\n" +
                "  void test()\n" +
                "  void test(1: string msg)\n" +
                "}\n";

        try {
            load(thrift);
            fail("Services cannot have more than one method with the same name");
        } catch (LoadFailedException e) {
            // good
        }
    }

    @Test
    public void serviceWithDuplicateMethodFromBaseServiceIsInvalid() throws Exception {
        String thrift = "" +
                "namespace java service.invalid\n" +
                "\n" +
                "service Base {\n" +
                "  void test()\n" +
                "}\n" +
                "\n" +
                "service Derived extends Base {\n" +
                "  void test()\n" +
                "}\n";

        try {
            load(thrift);
            fail("Service cannot override methods inherited from base services");
        } catch (LoadFailedException e) {
            // good
        }
    }

    @Test
    public void serviceMethodWithDuplicateFieldIdsIsInvalid() throws Exception {
        String thrift = "" +
                "namespace java service.invalid\n" +
                "\n" +
                "service NoGood {\n" +
                "  void test(1: i32 foo; 1: i64 bar)\n" +
                "}\n";

        try {
            load(thrift);
            fail("Methods having multiple parameters with the same ID are invalid");
        } catch (LoadFailedException e) {
            // good
        }
    }

    @Test
    public void serviceThatExtendsNonServiceIsInvalid() throws Exception {
        String thrift = "" +
                "namespace java service.invalid\n" +
                "\n" +
                "service Foo extends i32 {\n" +
                "}\n";

        try {
            load(thrift);
            fail("Service extending non-service types should not validate");
        } catch (LoadFailedException expected) {
            // hooray
        }
    }

    @Test
    public void onewayVoidMethodIsValid() throws Exception {
        String thrift = "" +
                "namespace java service.oneway.valid\n" +
                "\n" +
                "service OnewayService {\n" +
                "  oneway void test();\n" +
                "}\n";

        Schema schema = load(thrift);
        ServiceType service = schema.services().get(0);
        ServiceMethod method = service.methods().get(0);

        assertThat(method.oneWay(), is(true));
        assertThat(method.returnType(), equalTo(BuiltinType.VOID));
    }

    @Test
    public void onewayMethodWithReturnTypeIsInvalid() throws Exception {
        String thrift = "" +
                "namespace java service.oneway.valid\n" +
                "\n" +
                "service OnewayService {\n" +
                "  oneway i32 test();\n" +
                "}\n";

        try {
            load(thrift);
            fail("Oneway methods cannot have non-void return types");
        } catch (LoadFailedException e) {
            // yay
        }
    }

    @Test
    public void onewayMethodWithThrowsIsInvalid() throws Exception {
        String thrift = "" +
                "namespace java service.oneway.valid\n" +
                "\n" +
                "exception MyError {\n" +
                "  1: i32 what\n" +
                "  2: string why\n" +
                "}\n" +
                "\n" +
                "service OnewayService {\n" +
                "  oneway void test() throws (1: MyError error);\n" +
                "}\n";

        try {
            load(thrift);
            fail("Oneway methods cannot throw exceptions");
        } catch (LoadFailedException e) {
            // yay
        }
    }

    @Test
    public void throwsClauseWithNonExceptionBuiltinTypeIsInvalid() throws Exception {
        String thrift = "" +
                "namespace java service.throws.invalid\n" +
                "\n" +
                "service ThrowsStrings {" +
                "  void test() throws (1: string not_an_exception)\n" +
                "}\n";

        try {
            load(thrift);
            fail("Methods that declare throws of non-exception types are invalid");
        } catch (LoadFailedException e) {
            // good
        }
    }

    @Test
    public void throwsClauseWithListTypeIsInvalid() throws Exception {
        String thrift = "" +
                "namespace java service.throws.invalid\n" +
                "\n" +
                "service ThrowsList {\n" +
                "  void test() throws (1: list<i32> nums)\n" +
                "}\n";

        try {
            load(thrift);
            fail("Methods that declare throws of non-exception types are invalid");
        } catch (LoadFailedException e) {
            // good
        }
    }

    @Test
    public void throwsClauseWithDuplicateFieldIdsIsInvalid() throws Exception {
        String thrift = "" +
                "namespace java service.throws.invalid\n" +
                "\n" +
                "exception Foo {}\n" +
                "\n" +
                "exception Bar {}\n" +
                "\n" +
                "service DuplicateExnIds {\n" +
                "  void test() throws (1: Foo foo, 1: Bar bar)\n" +
                "}\n";

        try {
            load(thrift);
            fail("Methods with multiple exceptions having the same ID are invalid");
        } catch (LoadFailedException e) {
            // good
        }
    }

    @Test
    public void throwsClauseWithNonExceptionUserTypeIsInvalid() throws Exception {
        String thrift = "" +
                "namespace java service.throws.invalid\n" +
                "\n" +
                "struct UserType {\n" +
                "  1: string foo\n" +
                "}\n" +
                "\n" +
                "service OnewayService {\n" +
                "  void test() throws (1: UserType also_not_an_exception);\n" +
                "}\n";

        try {
            load(thrift);
            fail("Methods that declare throws of non-exception user types are invalid");
        } catch (LoadFailedException e) {
            // good
        }
    }

    @Test
    public void throwsClauseWithExceptionTypeIsValid() throws Exception {
        String thrift = "" +
                "namespace java service.throws.valid\n" +
                "\n" +
                "exception UserType {\n" +
                "  1: string foo\n" +
                "}\n" +
                "\n" +
                "service OnewayService {\n" +
                "  void test() throws (1: UserType error);\n" +
                "}\n";

        Schema schema = load(thrift);
        StructType struct = schema.exceptions().get(0);

        assertThat(struct.isException(), is(true));

        ServiceType service = schema.services().get(0);
        ServiceMethod method = service.methods().get(0);
        Field field = method.exceptions().get(0);
        ThriftType type = field.type();

        assertThat(type, equalTo(struct));
    }

    @Test
    public void circularInheritanceDetected() throws Exception {
        String thrift = "" +
                "namespace java thrifty.services\n" +
                "\n" +
                "service A extends B {}\n" +
                "\n" +
                "service B extends C {}\n" +
                "\n" +
                "service C extends A {}\n";

        try {
            load(thrift);
            fail("Service inheritance cannot form a cycle");
        } catch (LoadFailedException e) {
            // Make sure that we identify the cycle
            assertThat(e.getMessage(), containsString("A -> B -> C -> A"));
        }
    }

    @Test
    public void constAndTypeWithSameName() throws Exception {
        String thrift = "" +
                "namespace java thrifty.constants\n" +
                "\n" +
                "const Foo Foo = Foo.BAR;\n" +
                "\n" +
                "enum Foo {\n" +
                "  BAR,\n" +
                "  BAZ\n" +
                "}\n" +
                "\n" +
                "struct FooBar {\n" +
                "  1: required Foo Foo = Foo,\n" +
                "  2: required Foo Bar = Foo.BAR,\n" +
                "}\n";

        load(thrift);
    }

    @Test
    public void booleanConstValidation() throws Exception {
        String thrift = "" +
                "namespace java thrifty.constants\n" +
                "\n" +
                "const bool B = true;\n" +
                "\n" +
                "struct Foo {\n" +
                "  1: required bool value = B\n" +
                "}\n";

        load(thrift);
    }

    @Test
    public void typedefOfBooleanConstValidation() throws Exception {
        // TODO: Does this actually work in the Apache compiler?
        String thrift = "" +
                "namespace java thrifty.constants\n" +
                "\n" +
                "typedef bool MyBool" +
                "\n" +
                "const MyBool B = true;\n" +
                "\n" +
                "struct Foo {\n" +
                "  1: required bool value = B\n" +
                "}\n";

        load(thrift);
    }

    @Test
    public void importedEnumConstantValue() throws Exception {
        String imported = "" +
                "namespace java src\n" +
                "\n" +
                "enum Foo {\n" +
                "  One = 1\n" +
                "}\n";

        String target = "" +
                "namespace java target\n" +
                "\n" +
                "include 'src.thrift'\n" +
                "\n" +
                "struct Bar {\n" +
                "  1: required src.Foo foo = Foo.One\n" +
                "  3: required src.Foo baz = 1\n" +
                "  4: required src.Foo quux = src.Foo.One\n" +
                "}\n";

        File src = tempDir.newFile("src.thrift");
        File dest = tempDir.newFile("dest.thrift");
        writeTo(src, imported);
        writeTo(dest, target);

        load(src, dest);
    }

    @Test
    public void bareEnumDoesNotPassValidation() throws Exception {
        String thrift = "" +
                "enum Foo {\n" +
                "  One\n" +
                "}\n" +
                "\n" +
                "struct Bar {\n" +
                "  1: required Foo foo = One\n" +
                "}\n";

        try {
            load(thrift);
            fail("Expected a LoadFailedException from validating a bare enum member in a constant");
        } catch (LoadFailedException e) {
            assertHasError(e, "Unqualified name 'One' is not a valid enum constant");
        }
    }

    @Test
    @SuppressWarnings({"ConstantConditions", "deprecation"})
    public void addingNullStringFileThrows() {
        String file = null;
        Loader loader = new Loader();
        try {
            loader.addThriftFile(file);
            fail("Expected an NPE, but nothing was thrown");
        } catch (NullPointerException e) {
            assertThat(e, hasMessage(containsString("file")));
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void addingNullPathFileThrows() {
        Loader loader = new Loader();
        Path nullFile = null;
        try {
            loader.addThriftFile(nullFile);
            fail("Expected an NPE, but nothing was thrown");
        } catch (NullPointerException e) {
            assertThat(e, hasMessage(containsString("file")));
        }
    }

    @Test
    @SuppressWarnings({"ConstantConditions", "deprecation"})
    public void addIncludeFileSmokeTest() throws IOException, LoadFailedException {
        Loader loader = new Loader();
        File thriftFile = tempDir.newFile("example.thrift");
        loader.addIncludePath(tempDir.getRoot().toPath());
        String thrift = "" +
                "namespace java example\n" +
                "\n" +
                "typedef string Uuid" +
                "\n";
        writeTo(thriftFile, thrift);
        Schema schema = loader.load();
        assertThat(schema.typedefs(), hasSize(1));
        TypedefType typedef = schema.typedefs().get(0);
        assertThat(typedef.name(), equalTo("Uuid"));
    }

    @Test
    @SuppressWarnings({"ConstantConditions", "deprecation"})
    public void addNullIncludeFileThrows() {
        Loader loader = new Loader();
        File nullFile = null;
        try {
            loader.addIncludePath(nullFile);
            fail("Expected an NPE, but nothing was thrown");
        } catch (NullPointerException e) {
            assertThat(e, hasMessage(containsString("path")));
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void addNullIncludePathThrows() {
        Loader loader = new Loader();
        Path nullPath = null;
        try {
            loader.addIncludePath(nullPath);
            fail("Expected an NPE, but nothing was thrown");
        } catch (NullPointerException e) {
            assertThat(e, hasMessage(containsString("path")));
        }
    }

    @Test
    public void addingNonExistentFileThrows() throws Exception {
        Path folder = tempDir.newFolder().toPath();
        Path doesNotExist = folder.resolve("nope.thrift");
        Loader loader = new Loader();
        try {
            loader.addThriftFile(doesNotExist);
            fail("Expected an IllegalArgumentException, but nothing was thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e, hasMessage(equalTo("thrift file must be a regular file")));
        }
    }

    @Test
    public void addingNonExistentIncludeDirectoryThrows() {
        Path doesNotExist = tempDir.getRoot().toPath().resolve("notCreatedYet");
        Loader loader = new Loader();
        try {
            loader.addIncludePath(doesNotExist);
            fail("Expected an IllegalArgumentException, but nothing was thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e, hasMessage(equalTo("path must be a directory")));
        }
    }

    @Test
    public void loadingWithoutFilesOrIncludesThrows() {
        Loader loader = new Loader();
        try {
            loader.load();
            fail("Expected a LoadFailedException, but nothing was thrown");
        } catch (LoadFailedException expected) {
            Throwable cause = expected.getCause();
            assertThat(
                    cause,
                    hasMessage(equalTo("No files and no include paths containing Thrift files were provided")));
        }
    }

    @Test
    public void loadingProgramWithDuplicateSymbolsThrows() throws Exception {
        String thrift = "" +
                "namespace java duplicateSymbols;\n" +
                "\n" +
                "struct Foo {\n" +
                "  1: required string foo;\n" +
                "}\n" +
                "\n" +
                "enum Foo {\n" +
                "  FOO\n" +
                "}\n";

        try {
            load(thrift);
            fail();
        } catch (LoadFailedException expected) {
            assertThat(expected, hasMessage(containsString("Duplicate symbols: Foo defined at")));
        }
    }

    @Test
    public void loadingProgramWithDuplicatedConstantNamesThrows() throws Exception {
        String thrift = "" +
                "namespace java duplicateConstants;\n" +
                "\n" +
                "const i32 Foo = 10 \n" +
                "\n" +
                "const string Foo = 'bar'\n";

        try {
            load(thrift);
            fail();
        } catch (LoadFailedException expected) {
            assertThat(expected, hasMessage(containsString("Duplicate symbols: Foo defined at")));
        }
    }

    @Test
    public void constRefToConstantList() throws Exception {
        String thrift = "" +
                "const list<i32> SOME_LIST = [1, 2, 3, 4, 5];\n" +
                "\n" +
                "struct Sample {\n" +
                "  1: required list<i32> value = SOME_LIST;\n" +
                "}\n";

        load(thrift);
    }

    @Test
    public void constRefToConstantListOfIncorrectType() throws Exception {
        String thrift = "" +
                "const list<i32> SOME_LIST = [1, 2, 3, 4, 5];\n" +
                "\n" +
                "struct Incorrect {\n" +
                "  1: required list<string> value = SOME_LIST;\n" +
                "}\n";

        try {
            load(thrift);
            fail();
        } catch (LoadFailedException expected) {
            assertThat(expected, hasMessage(containsString("Expected a value with type list<string>")));
        }
    }

    @Test
    public void nonListConstRefOnListTypeThrows() throws Exception {
        String thrift = "" +
                "struct Nope {\n" +
                "  1: required set<binary> bytes = 3.14159;\n" +
                "}\n";

        try {
            load(thrift);
            fail();
        } catch (LoadFailedException expected) {
            assertThat(expected, hasMessage(containsString("Expected a list literal, got: 3.14159")));
        }
    }

    @Test
    public void constRefToConstantMap() throws Exception {
        String thrift = "" +
                "const map<i8, i8> MAP = {1: 2}\n" +
                "\n" +
                "struct ByteMapHolder {\n" +
                "  1: required map<i8, i8> theMap = MAP;\n" +
                "}\n";

        load(thrift);
    }

    @Test
    public void constRefToConstMapOfIncorrectTypeThrows() throws Exception {
        String thrift = "" +
                "const map<i16, i16> MAP = {3: 4}\n" +
                "\n" +
                "struct ByteMapHolder {\n" +
                "  1: required map<i8, i8> theMap = MAP;\n" +
                "}\n";

        try {
            load(thrift);
            fail();
        } catch (LoadFailedException expected) {
            assertThat(expected, hasMessage(containsString("Expected a value with type map<i8, i8>")));
        }
    }

    @Test
    public void constRefToNonMapTypeThrows() throws Exception {
        String thrift = "" +
                "struct StillNope {\n" +
                "  1: required map<binary, binary> cache = 'foo';\n" +
                "}\n";

        try {
            load(thrift);
            fail();
        } catch (LoadFailedException expected) {
            assertThat(expected, hasMessage(containsString("Expected a map literal, got: foo")));
        }
    }

    private Schema load(String thrift) throws Exception {
        File f = tempDir.newFile();
        writeTo(f, thrift);

        Loader loader = new Loader();
        loader.addThriftFile(f.toPath());
        return loader.load();
    }

    private Schema load(File ...files) throws Exception {
        Loader loader = new Loader();
        for (File f : files) {
            loader.addThriftFile(f.toPath());
        }
        return loader.load();
    }

    private static void writeTo(File file, String content) throws IOException {
        BufferedSink sink = Okio.buffer(Okio.sink(file));
        sink.writeUtf8(content);
        sink.flush();
        sink.close();
    }

    private static void assertHasError(LoadFailedException exception, String expectedMessage) {
        for (ErrorReporter.Report report : exception.errorReporter().reports()) {
            if (report.message().contains(expectedMessage)) {
                return;
            }
        }
        throw new AssertionError("Expected a reported error containing '" + expectedMessage + "'");
    }
}
