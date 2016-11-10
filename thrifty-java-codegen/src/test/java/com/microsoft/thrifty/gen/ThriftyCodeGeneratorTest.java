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
package com.microsoft.thrifty.gen;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.microsoft.thrifty.schema.Loader;
import com.microsoft.thrifty.schema.Schema;
import com.squareup.javapoet.JavaFile;
import okio.BufferedSink;
import okio.Okio;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.JavaFileObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

/**
 * These tests ensure that various constructs produce valid Java code.
 * They don't test *anything* about the correctness of the code!
 *
 * Semantic tests can be found in {@code thrifty-integration-tests}.
 */
public class ThriftyCodeGeneratorTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void redactedToStringCompiles() throws Exception {
        String thrift = Joiner.on('\n').join(
                "namespace java test",
                "",
                "struct Foo {",
                "  1: required list<string> (python.immutable) ssn (redacted)",
                "}");

        Schema schema = parse("foo.thrift", thrift);

        ThriftyCodeGenerator gen = new ThriftyCodeGenerator(schema);
        ImmutableList<JavaFile> javaFiles = gen.generateTypes();

        assertThat(javaFiles).hasSize(1);

        assertAbout(javaSource())
                .that(javaFiles.get(0).toJavaFileObject())
                .compilesWithoutError();
    }

    @Test
    public void enumGeneration() throws Exception {
        String thrift = Joiner.on('\n').join(
                "namespace java enums",
                "",
                "// A generated enum",
                "enum BuildStatus {",
                "  OK = 0,",
                "  FAIL = 1",
                "}");

        Schema schema = parse("enum.thrift", thrift);
        ThriftyCodeGenerator gen = new ThriftyCodeGenerator(schema);
        ImmutableList<JavaFile> java = gen.generateTypes();

        assertThat(java).hasSize(1);

        assertAbout(javaSource())
                .that(java.get(0).toJavaFileObject())
                .compilesWithoutError();
    }

    @Test
    public void fieldWithConstInitializer() throws Exception {
        String thrift = Joiner.on('\n').join(
                "namespace java fields",
                "",
                "const i32 TEST_CONST = 5",
                "",
                "struct HasDefaultValue {",
                "  1: required i32 foo = TEST_CONST",
                "}");

        Schema schema = parse("fields.thrift", thrift);
        ThriftyCodeGenerator gen = new ThriftyCodeGenerator(schema);
        ImmutableList<JavaFile> java = gen.generateTypes();
        List<JavaFileObject> jfos = new ArrayList<>();

        boolean found = false;
        for (JavaFile javaFile : java) {
            if (javaFile.toString().contains("foo = fields.Constants.TEST_CONST;")) {
                found = true;
            }
        }

        assertThat(found).named("Const reference was found in field assignment").isTrue();

        assertThat(java).hasSize(2);
        for (JavaFile javaFile : java) {
            jfos.add(javaFile.toJavaFileObject());
        }

        assertAbout(javaSources())
                .that(jfos)
                .compilesWithoutError();
    }

    @Test
    public void deprecatedStructWithComment() throws Exception {
        String thrift = Joiner.on('\n').join(
                "namespace java deprecated",
                "",
                "/** @deprecated */",
                "struct Foo {}"
        );

        Schema schema = parse("dep.thrift", thrift);
        ThriftyCodeGenerator gen = new ThriftyCodeGenerator(schema);
        ImmutableList<JavaFile> java = gen.generateTypes();
        List<JavaFileObject> jfos = new ArrayList<>(java.size());

        for (JavaFile javaFile : java) {
            jfos.add(javaFile.toJavaFileObject());
        }

        assertAbout(javaSources()).that(jfos).compilesWithoutError();

        String file = java.get(0).toString();

        assertThat(file).contains("@Deprecated");  // note the change in case
    }

    @Test
    public void deprecatedStructWithAnnotation() throws Exception {
        String thrift = Joiner.on('\n').join(
                "namespace java deprecated",
                "",
                "struct Foo {} (deprecated)"
        );

        Schema schema = parse("dep.thrift", thrift);
        ThriftyCodeGenerator gen = new ThriftyCodeGenerator(schema);
        ImmutableList<JavaFile> java = gen.generateTypes();
        List<JavaFileObject> jfos = new ArrayList<>(java.size());

        for (JavaFile javaFile : java) {
            jfos.add(javaFile.toJavaFileObject());
        }

        assertAbout(javaSources()).that(jfos).compilesWithoutError();

        String file = java.get(0).toString();

        assertThat(file).contains("@Deprecated");
    }

    @Test
    public void deprecatedEnum() throws Exception {
        String thrift = Joiner.on('\n').join(
                "namespace java deprecated",
                "",
                "enum Foo {ONE = 1} (deprecated)"
        );

        Schema schema = parse("enum.thrift", thrift);
        ThriftyCodeGenerator gen = new ThriftyCodeGenerator(schema);
        ImmutableList<JavaFile> javaFiles = gen.generateTypes();
        JavaFile file = javaFiles.get(0);

        String java = file.toString();

        assertThat(java).contains("@Deprecated\npublic enum Foo");
    }

    @Test
    public void deprecatedEnumMember() throws Exception {
        String thrift = Joiner.on('\n').join(
                "namespace java deprecated",
                "",
                "enum Foo {",
                "  ONE = 1 (deprecated)",
                "}"
        );

        Schema schema = parse("enum.thrift", thrift);
        ThriftyCodeGenerator gen = new ThriftyCodeGenerator(schema);
        ImmutableList<JavaFile> javaFiles = gen.generateTypes();
        JavaFile file = javaFiles.get(0);

        String java = file.toString();

        assertThat(java).contains("@Deprecated\n  ONE(1)");
    }

    @Test
    public void stringConstantsAreNotUnboxed() throws Exception {
        String thrift = "" +
                "namespace java string_consts\n" +
                "\n" +
                "const string STR = 'foo'";

        // This check validates that we can successfully compile a string constant,
        // and that we don't regress on issue #77.
        //
        // The regression here would be if an UnsupportedOperationException were thrown,
        // due to a logic bug where we attempt to unbox TypeNames.STRING.
        compile("string_consts.thrift", thrift);
    }

    @Test
    public void byteConstants() throws Exception {
        String thrift = "" +
                "namespace java byte_consts\n" +
                "\n" +
                "const i8 I8 = 123";

        JavaFile file = compile("bytes.thrift", thrift).get(0);
        assertThat(file.toString()).isEqualTo("" +
                "package byte_consts;\n" +
                "\n" +
                "public final class Constants {\n" +
                "  public static final byte I8 = (byte) 123;\n" +
                "\n" +
                "  private Constants() {\n" +
                "    // no instances\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    public void shortConstants() throws Exception {
        String thrift = "" +
                "namespace java short_consts\n" +
                "\n" +
                "const i16 INT = 0xFF";

        JavaFile file = compile("shorts.thrift", thrift).get(0);
        assertThat(file.toString()).isEqualTo("" +
                "package short_consts;\n" +
                "\n" +
                "public final class Constants {\n" +
                "  public static final short INT = (short) 255;\n" +
                "\n" +
                "  private Constants() {\n" +
                "    // no instances\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    public void intConstants() throws Exception {
        String thrift = "" +
                "namespace java int_consts\n" +
                "\n" +
                "const i32 INT = 12345";

        JavaFile file = compile("ints.thrift", thrift).get(0);
        assertThat(file.toString()).isEqualTo("" +
                "package int_consts;\n" +
                "\n" +
                "public final class Constants {\n" +
                "  public static final int INT = 12345;\n" +
                "\n" +
                "  private Constants() {\n" +
                "    // no instances\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    public void longConstants() throws Exception {
        String thrift = "" +
                "namespace java long_consts\n" +
                "\n" +
                "const i64 LONG = 0xFFFFFFFFFF";

        JavaFile file = compile("longs.thrift", thrift).get(0);
        assertThat(file.toString()).isEqualTo("" +
                "package long_consts;\n" +
                "\n" +
                "public final class Constants {\n" +
                "  public static final long LONG = 1099511627775L;\n" +
                "\n" +
                "  private Constants() {\n" +
                "    // no instances\n" +
                "  }\n" +
                "}\n");
    }

    private ImmutableList<JavaFile> compile(String filename, String text) throws Exception {
        Schema schema = parse(filename, text);
        ThriftyCodeGenerator gen = new ThriftyCodeGenerator(schema).emitFileComment(false);
        return gen.generateTypes();
    }

    private Schema parse(String filename, String text) throws Exception {
        File file = tmp.newFile(filename);
        try (BufferedSink sink = Okio.buffer(Okio.sink(file))) {
            sink.writeUtf8(text);
            sink.flush();
        }

        Loader loader = new Loader();
        loader.addThriftFile(file.getCanonicalPath());

        return loader.load();
    }
}