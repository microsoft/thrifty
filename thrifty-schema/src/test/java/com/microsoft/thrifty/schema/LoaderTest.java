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
import com.microsoft.thrifty.Struct;
import okio.BufferedSink;
import okio.Okio;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
        assertThat(fieldType.getTrueType(), is(ThriftType.I32));

        Service svc = schema.services().get(0);
        assertThat(svc.name(), is("Svc"));
        assertThat(svc.methods().size(), is(1));

        ServiceMethod method = svc.methods().get(0);
        assertThat(method.name(), is("sayHello"));
        assertThat(method.oneWay(), is(true));
        assertThat(method.paramTypes().size(), is(1));
        assertThat(method.exceptionTypes().size(), is(0));

        Field param = method.paramTypes().get(0);
        assertThat(param.name(), is("arg1"));
        assertThat(param.type().name(), is("S"));
        assertThat(param.type(), equalTo(st.type()));
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

        Loader loader = new Loader();
        loader.addThriftFile(f.getAbsolutePath());
        loader.addThriftFile(f1.getAbsolutePath());

        Schema schema = loader.load();

        EnumType et = schema.enums().get(0);
        assertThat(et.type().name(), is("TestEnum"));

        Typedef td = schema.typedefs().get(0);
        assertThat(td.oldType(), equalTo(et.type()));
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

        Loader loader = new Loader();
        loader.addThriftFile(f.getAbsolutePath());
        loader.addThriftFile(f1.getAbsolutePath());

        try {
            loader.load();
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

        Loader loader = new Loader();
        loader.addThriftFile(producer.getAbsolutePath());
        loader.addThriftFile(consumer.getAbsolutePath());

        loader.load();
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
        loader.addThriftFile(f1.getAbsolutePath());
        loader.addThriftFile(f2.getAbsolutePath());
        loader.addThriftFile(f3.getAbsolutePath());

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
            new Loader()
                    .addThriftFile(f1.getAbsolutePath())
                    .addThriftFile(f2.getAbsolutePath())
                    .addThriftFile(f3.getAbsolutePath())
                    .load();
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

        Typedef code = schema.typedefs().get(0);
        Typedef msg = schema.typedefs().get(1);
        Typedef map = schema.typedefs().get(2);

        assertThat(code.name(), is("StatusCode"));
        assertThat(code.oldType().isBuiltin(), is(true));
        assertThat(code.oldType().name(), is("i32"));

        assertThat(msg.name(), is("Message"));
        assertThat(msg.oldType().isBuiltin(), is(true));
        assertThat(msg.oldType().name(), is("string"));

        assertThat(map.name(), is("Messages"));
        assertThat(map.oldType().isMap(), is(true));

        ThriftType.MapType mt = (ThriftType.MapType) map.oldType();
        assertThat(mt.keyType(), equalTo(code.type()));
        assertThat(mt.valueType(), equalTo(msg.type()));
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
    public void canLoadAndLinkOfficialTestThrift() throws Exception {
        URL url = getClass().getClassLoader().getResource("cases/TestThrift.thrift");
        File file = new File(url.getFile());

        Loader loader = new Loader();
        loader.addThriftFile(file.getAbsolutePath());

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

        Loader loader = new Loader();
        loader.addThriftFile(f1.getAbsolutePath());
        loader.addThriftFile(f2.getAbsolutePath());

        loader.load();
    }

    @Test
    public void typedefsWithAnnotations() throws Exception {
        String thrift = "namespace java typedef.annotations\n" +
                "\n" +
                "typedef i64 (js.type = \"Date\") Date";

        Schema schema = load(thrift);

        Typedef td = schema.typedefs().get(0);
        assertThat(td.sourceTypeAnnotations(), hasEntry("js.type", "Date"));
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
        Service base = schema.services().get(0);
        Service derived = schema.services().get(1);

        assertThat(base.name(), is("Base"));
        assertThat(derived.name(), is("Derived"));

        assertThat(base.type(), equalTo(derived.extendsService()));
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
        Service service = schema.services().get(0);
        ServiceMethod method = service.methods().get(0);

        assertThat(method.oneWay(), is(true));
        assertThat(method.returnType().get(), equalTo(ThriftType.VOID));
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

        Service service = schema.services().get(0);
        ServiceMethod method = service.methods().get(0);
        Field field = method.exceptionTypes().get(0);
        ThriftType type = field.type();

        assertThat(type, equalTo(struct.type()));
    }

    @Test
    public void methodParameterNamesHaveFieldNamingPolicyApplied() throws Exception {
        String thrift = "" +
                "service Service {\n" +
                "  void foo(1: string MyParam)\n" +
                "}\n";

        File f = tempDir.newFile("svc.thrift");
        writeTo(f, thrift);

        Loader loader = new Loader(FieldNamingPolicy.JAVA);
        loader.addThriftFile(f.getAbsolutePath());

        Schema schema = loader.load();
        Service service = schema.services().get(0);
        ServiceMethod method = service.methods().get(0);
        Field param = method.paramTypes().get(0);

        assertThat(param.name(), is("myParam"));
    }

    @Test
    public void throwsElementNamesHaveFieldNamingPolicyApplied() throws Exception {
        String thrift = "" +
                "exception NoMorePowerError {\n" +
                "  1: string message\n" +
                "}\n" +
                "\n" +
                "service Service {\n" +
                "  void firePhasers() throws (1: NoMorePowerError TheUsualComplaint)\n" +
                "}\n";

        File f = tempDir.newFile("svc.thrift");
        writeTo(f, thrift);

        Loader loader = new Loader(FieldNamingPolicy.JAVA);
        loader.addThriftFile(f.getAbsolutePath());

        Schema schema = loader.load();
        Service service = schema.services().get(0);
        ServiceMethod method = service.methods().get(0);
        Field param = method.exceptionTypes().get(0);

        assertThat(param.name(), is("theUsualComplaint"));
    }

    private Schema load(String thrift) throws Exception {
        File f = tempDir.newFile();
        writeTo(f, thrift);

        Loader loader = new Loader();
        loader.addThriftFile(f.getAbsolutePath());
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
