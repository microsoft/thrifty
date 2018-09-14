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
package com.microsoft.thrifty.gen

import com.microsoft.thrifty.schema.Loader
import com.microsoft.thrifty.schema.Schema
import com.squareup.javapoet.JavaFile
import okio.Okio
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import javax.tools.JavaFileObject
import java.io.File
import java.util.ArrayList

import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.JavaSourceSubjectFactory.javaSource
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources

/**
 * These tests ensure that various constructs produce valid Java code.
 * They don't test *anything* about the correctness of the code!
 *
 * Semantic tests can be found in `thrifty-integration-tests`.
 */
class ThriftyCodeGeneratorTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun redactedToStringCompiles() {
        val thrift = """
            namespace java test

            struct foo {
                1: required list<string> (python.immutable) ssn (redacted)
            }
        """

        val schema = parse("foo.thrift", thrift)

        val gen = ThriftyCodeGenerator(schema)
        val javaFiles = gen.generateTypes()

        assertThat(javaFiles).hasSize(1)

        assertAbout(javaSource())
                .that(javaFiles[0].toJavaFileObject())
                .compilesWithoutError()
    }

    @Test
    fun enumGeneration() {
        val thrift = """
            namespace java enums

            // a generated enum
            enum BuildStatus {
                OK = 0,
                FAIL = 1
            }
        """

        val schema = parse("enum.thrift", thrift)
        val gen = ThriftyCodeGenerator(schema)
        val java = gen.generateTypes()

        assertThat(java).hasSize(1)

        assertAbout(javaSource())
                .that(java[0].toJavaFileObject())
                .compilesWithoutError()
    }

    @Test
    fun fieldWithConstInitializer() {
        val thrift = """
            namespace java fields

            const i32 TEST_CONST = 5

            struct HasDefaultValue {
                1: required i32 foo = TEST_CONST
            }
        """

        val schema = parse("fields.thrift", thrift)
        val gen = ThriftyCodeGenerator(schema)
        val java = gen.generateTypes()
        val jfos = ArrayList<JavaFileObject>()

        var found = false
        for (javaFile in java) {
            if (javaFile.toString().contains("foo = fields.Constants.TEST_CONST;")) {
                found = true
            }
        }

        assertThat(found).named("Const reference was found in field assignment").isTrue()

        assertThat(java).hasSize(2)
        for (javaFile in java) {
            jfos.add(javaFile.toJavaFileObject())
        }

        assertAbout(javaSources())
                .that(jfos)
                .compilesWithoutError()
    }

    @Test
    fun deprecatedStructWithComment() {
        val thrift = """
            namespace java deprecated

            /** @deprecated */
            struct Foo {}
        """

        val schema = parse("dep.thrift", thrift)
        val gen = ThriftyCodeGenerator(schema)
        val java = gen.generateTypes()
        val jfos = ArrayList<JavaFileObject>(java.size)

        for (javaFile in java) {
            jfos.add(javaFile.toJavaFileObject())
        }

        assertAbout(javaSources()).that(jfos).compilesWithoutError()

        val file = java[0].toString()

        assertThat(file).contains("@Deprecated")  // note the change in case
    }

    @Test
    fun deprecatedStructWithAnnotation() {
        val thrift = """
            namespace java deprecated

            struct Foo {} (deprecated)
        """

        val schema = parse("dep.thrift", thrift)
        val gen = ThriftyCodeGenerator(schema)
        val java = gen.generateTypes()
        val jfos = ArrayList<JavaFileObject>(java.size)

        for (javaFile in java) {
            jfos.add(javaFile.toJavaFileObject())
        }

        assertAbout(javaSources()).that(jfos).compilesWithoutError()

        val file = java[0].toString()

        assertThat(file).contains("@Deprecated")
    }

    @Test
    fun deprecatedEnum() {
        val thrift = """
            namespace java deprecated

            enum Foo {ONE = 1} (deprecated)
        """

        val schema = parse("enum.thrift", thrift)
        val gen = ThriftyCodeGenerator(schema)
        val javaFiles = gen.generateTypes()
        val file = javaFiles[0]

        val java = file.toString()

        assertThat(java).contains("@Deprecated")
    }

    @Test
    fun deprecatedEnumMember() {
        val thrift = """
            namespace java deprecated

            enum Foo {
              ONE = 1 (deprecated)
            }
        """

        val schema = parse("enum.thrift", thrift)
        val gen = ThriftyCodeGenerator(schema)
        val javaFiles = gen.generateTypes()
        val file = javaFiles[0]

        val java = file.toString()

        assertThat(java).contains("@Deprecated\n  ONE(1)")
    }

    @Test
    fun nullableEnumFindByValue() {
        val thrift = """
            namespace java enums

            // a generated enum
            enum BuildStatus {
                OK = 0,
                FAIL = 1
            }
        """

        val schema = parse("enum_nullable.thrift", thrift)
        val gen = ThriftyCodeGenerator(schema).emitAndroidAnnotations(true)
        val javaFiles = gen.generateTypes()
        val file = javaFiles[0]

        val java = file.toString()

        assertThat(java).contains("@Nullable\n  public static BuildStatus findByValue")
    }

    @Test
    fun stringConstantsAreNotUnboxed() {
        val thrift = """
            namespace java string_consts

            const string STR = 'foo'
        """

        // This check validates that we can successfully compile a string constant,
        // and that we don't regress on issue #77.
        //
        // The regression here would be if an UnsupportedOperationException were thrown,
        // due to a logic bug where we attempt to unbox TypeNames.STRING.
        compile("string_consts.thrift", thrift)
    }

    @Test
    fun byteConstants() {
        val thrift = """
            namespace java byte_consts

            const i8 I8 = 123
        """

        val file = compile("bytes.thrift", thrift)[0]
        assertThat(file.toString()).isEqualTo("""
            package byte_consts;

            import javax.annotation.Generated;

            @Generated(
                value = "com.microsoft.thrifty.gen.ThriftyCodeGenerator",
                comments = "https://github.com/microsoft/thrifty"
            )
            public final class Constants {
              public static final byte I8 = (byte) 123;

              private Constants() {
                // no instances
              }
            }

            """.trimRawString())
    }

    @Test
    fun shortConstants() {
        val thrift = """
            namespace java short_consts

            const i16 INT = 0xFF
        """

        val file = compile("shorts.thrift", thrift)[0]
        assertThat(file.toString()).isEqualTo("""
            package short_consts;

            import javax.annotation.Generated;

            @Generated(
                value = "com.microsoft.thrifty.gen.ThriftyCodeGenerator",
                comments = "https://github.com/microsoft/thrifty"
            )
            public final class Constants {
              public static final short INT = (short) 0xFF;

              private Constants() {
                // no instances
              }
            }

            """.trimRawString())
    }

    @Test
    fun intConstants() {
        val thrift = """
            namespace java int_consts

            const i32 INT = 12345
        """

        val file = compile("ints.thrift", thrift)[0]
        assertThat(file.toString()).isEqualTo("""
            package int_consts;

            import javax.annotation.Generated;

            @Generated(
                value = "com.microsoft.thrifty.gen.ThriftyCodeGenerator",
                comments = "https://github.com/microsoft/thrifty"
            )
            public final class Constants {
              public static final int INT = 12345;

              private Constants() {
                // no instances
              }
            }

            """.trimRawString())
    }

    @Test
    fun longConstants() {
        val thrift = """
            namespace java long_consts

            const i64 LONG = 0xFFFFFFFFFF
        """

        val file = compile("longs.thrift", thrift)[0]
        assertThat(file.toString()).isEqualTo("""
            package long_consts;

            import javax.annotation.Generated;

            @Generated(
                value = "com.microsoft.thrifty.gen.ThriftyCodeGenerator",
                comments = "https://github.com/microsoft/thrifty"
            )
            public final class Constants {
              public static final long LONG = 0xFFFFFFFFFFL;

              private Constants() {
                // no instances
              }
            }

        """.trimRawString())
    }

    @Test
    fun numberEqualityWarningsAreSuppressedForI32() {
        val thrift = """
            namespace java number_equality

            struct HasNumber {
              1: optional i32 n;
            }
        """

        val expectedEqualsMethod = """
  @Override
  @SuppressWarnings("NumberEquality")
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null) return false;
    if (!(other instanceof HasNumber)) return false;
    HasNumber that = (HasNumber) other;
    return (this.n == that.n || (this.n != null && this.n.equals(that.n)));
  }
""".trimStart('\n')

        val file = compile("numberEquality.thrift", thrift)[0]

        assertThat(file.toString()).contains(expectedEqualsMethod)
    }

    @Test
    fun constantsWithSigilsInJavadoc() {
        val thrift = """
            namespace java sigils.consts

            // This comment has ${"$"}Dollar ${"$"}Signs
            const i32 INT = 12345
        """

        val expectedFormat = """
            package sigils.consts;

            import javax.annotation.Generated;

            @Generated(
                value = "com.microsoft.thrifty.gen.ThriftyCodeGenerator",
                comments = "https://github.com/microsoft/thrifty"
            )
            public final class Constants {
              /**
               * This comment has ${"$"}Dollar ${"$"}Signs
               *
               *
               * Generated from: %s at 4:1
               */
              public static final int INT = 12345;

              private Constants() {
                // no instances
              }
            }

        """.trimRawString()

        val thriftFile = tmp.newFile("sigils_consts.thrift")
        val javaFile = compile(thriftFile, thrift)[0]

        val javaText = javaFile.toString()
        val expected = String.format(expectedFormat, thriftFile.name)

        assertThat(javaText).isEqualTo(expected)
    }

    @Test
    fun enumsWithSigilsInJavadoc() {
        val thrift = """
            namespace java sigils.enums

            // ${"$"}Sigil here
            enum TestEnum {
              // ${"$"}Good, here's another
              FOO
            }
        """

        val expected = """
            package sigils.enums;

            import javax.annotation.Generated;

            /**
             * ${"$"}Sigil here
             */
            @Generated(
                value = "com.microsoft.thrifty.gen.ThriftyCodeGenerator",
                comments = "https://github.com/microsoft/thrifty"
            )
            public enum TestEnum {
              /**
               * ${"$"}Good, here's another
               */
              FOO(0);

              public final int value;

              TestEnum(int value) {
                this.value = value;
              }

              public static TestEnum findByValue(int value) {
                switch (value) {
                  case 0: return FOO;
                  default: return null;
                }
              }
            }

            """.trimRawString()

        val thriftFile = tmp.newFile("sigil_enums.thrift")
        val javaFile = compile(thriftFile, thrift)[0]

        assertThat(javaFile.toString()).isEqualTo(expected)
    }

    @Test
    fun mapsWithEnumKeysAndValues () {
        val thrift = """
            namespace java maps.enums

            enum Key { KEY }
            enum Value { VALUE }

            struct HasMap {
                1: optional map<Key, Value> m
            }
        """

        val expected = """
              for (int i0 = 0; i0 < mapMetadata0.size; ++i0) {
                int i32_1 = protocol.readI32();
                maps.enums.Key key0 = maps.enums.Key.findByValue(i32_1);
                if (key0 == null) {
                  throw new ThriftException(ThriftException.Kind.PROTOCOL_ERROR, "Unexpected value for enum-type Key: " + i32_1);
                }
                int i32_2 = protocol.readI32();
                maps.enums.Value value0 = maps.enums.Value.findByValue(i32_2);
                if (value0 == null) {
                  throw new ThriftException(ThriftException.Kind.PROTOCOL_ERROR, "Unexpected value for enum-type Value: " + i32_2);
                }
                value.put(key0, value0);
              }
            """

        val thriftFile = tmp.newFile("maps_enums.thrift")
        val javaFile = compile(thriftFile, thrift)[2]

        assertThat(javaFile.toString()).contains(expected)
    }

    @Test
    fun structsWithSigilsInJavadoc() {
        val thrift = """
            namespace java sigils.structs

            // ${"$"}A ${"$"}B ${"$"}C ${"$"}D ${"$"}E
            struct Foo {
              // ${"$"}F ${"$"}G ${"$"}H ${"$"}I ${"$"}J
              1: required string bar
            }
        """

        val expectedClassJavadoc = """
            /**
             * ${"$"}A ${"$"}B ${"$"}C ${"$"}D ${"$"}E
             */
            @Generated(
                value = "com.microsoft.thrifty.gen.ThriftyCodeGenerator",
                comments = "https://github.com/microsoft/thrifty"
            )
            public final class Foo implements Struct {
        """.trimRawString()

        val expectedFieldJavadoc = """
  /**
   * ${"$"}F ${"$"}G ${"$"}H ${"$"}I ${"$"}J
   */
  @ThriftField(
      fieldId = 1,
      isRequired = true
  )
  public final String bar;
"""

        val thriftFile = tmp.newFile("sigil_enums.thrift")
        val javaFile = compile(thriftFile, thrift)[0]
        assertThat(javaFile.toString()).contains(expectedClassJavadoc)
        assertThat(javaFile.toString()).contains(expectedFieldJavadoc)
    }

    private fun compile(filename: String, text: String): List<JavaFile> {
        val schema = parse(filename, text)
        val gen = ThriftyCodeGenerator(schema).emitFileComment(false)
        return gen.generateTypes()
    }

    private fun compile(file: File, text: String): List<JavaFile> {
        val schema = parse(file, text)
        val gen = ThriftyCodeGenerator(schema).emitFileComment(false)
        return gen.generateTypes()
    }

    private fun parse(filename: String, text: String): Schema {
        return parse(tmp.newFile(filename), text)
    }

    private fun parse(file: File, text: String): Schema {

        val trimmed = text.trimStart('\n').trimIndent()

        Okio.buffer(Okio.sink(file)).use { sink ->
            sink.writeUtf8(trimmed)
            sink.flush()
        }

        val loader = Loader()
        loader.addThriftFile(file.toPath().toAbsolutePath().normalize())

        return loader.load()
    }

    private fun String.trimRawString() = this.trimStart('\n').trimIndent()
}
