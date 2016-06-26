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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;

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

    /**
     * Tests for duplicate names of different ThriftTypes.
     * Example: an enum and a constant can have the same name.
     * @throws Exception
     */
    @Test
    public void testDuplicateNamesDiffTypes() throws Exception {
        String thrift = Joiner.on('\n').join(
                "namespace java test",
                "",
                "const string Foo = \"foo\"",
                "enum Foo {",
                "  Bar",
                "  Bar2",
                "}");

        Schema schema = parse("foobar.thrift", thrift);

        ThriftyCodeGenerator gen = new ThriftyCodeGenerator(schema);
        ImmutableList<JavaFile> javaFiles = gen.generateTypes();

        assertThat(javaFiles).hasSize(2);

        assertAbout(javaSource())
                .that(javaFiles.get(0).toJavaFileObject())
                .compilesWithoutError();
    }

    /**
     * Tests for case where a typedef enum is set to a constant.
     * @throws Exception
     */
    @Test
    public void testTypeDefEnumAsConstant() throws Exception {
        String thrift = Joiner.on('\n').join(
                "namespace java test",
                "",
                "enum Foo {",
                "  Bar",
                "  Bar2",
                "} typedef Foo FooType",
                "const FooType foo = Foo.Bar");

        Schema schema = parse("typedef_enum.thrift", thrift);

        ThriftyCodeGenerator gen = new ThriftyCodeGenerator(schema);
        ImmutableList<JavaFile> javaFiles = gen.generateTypes();

        assertThat(javaFiles).hasSize(2);

        assertAbout(javaSource())
                .that(javaFiles.get(0).toJavaFileObject())
                .compilesWithoutError();
    }

    private Schema parse(String filename, String text) throws Exception {
        File file = tmp.newFile(filename);
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        BufferedWriter buf = new BufferedWriter(writer);
        try {
            buf.write(text);
            buf.flush();
        } finally {
            try {
                buf.close();
            } catch (IOException e) {
                // ignore
            }
            writer.close();
        }

        Loader loader = new Loader();
        loader.addThriftFile(file.getCanonicalPath());

        return loader.load();
    }
}