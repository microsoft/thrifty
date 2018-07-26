package com.microsoft.thrifty.kgen

import com.microsoft.thrifty.schema.Loader
import com.microsoft.thrifty.schema.Schema
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class KotlinCodeGeneratorTest {
    @get:Rule val tempDir = TemporaryFolder()

    @Test fun `struct to data class`() {
        val schema = load("""
            namespace kt com.test

            // This is an enum
            enum MyEnum {
              MEMBER_ONE, // trailing doc
              MEMBER_TWO

              // leading doc
              MEMBER_THREE = 4
            }

            struct Test {
              1: required string Foo (thrifty.redacted = "1");
              2: required map<i64, string> Numbers (thrift.obfuscated = "1");
              3: optional string Bar;
              5: optional binary Bs;
              6: MyEnum enumType;
              7: set<i8> Bytes;
              8: list<list<string>> listOfStrings
            }

            struct AnotherOne {
              1: optional i32 NumBitTheDust
            }
        """.trimIndent())

        val files = KotlinCodeGenerator().generate(schema)

        files.forEach { println("$it") }
    }

    @Test fun testWhenEmit() {
        val function = FunSpec.builder("test")
                .addParameter("x", Int::class)
                .beginControlFlow("when (x)")
                .addCode(CodeBlock.builder()
                        .addStatement("1 -> {%>")
                        .addStatement("println(\"1\")")
                        .addStatement("%<}")
                        .build())
                .addStatement("else -> {}")
                .endControlFlow()
                .build()

        println(function.toString())
    }

    private fun load(thrift: String): Schema {
        val file = tempDir.newFile("test.thrift")
        file.writeText(thrift)
        val loader = Loader().apply { addThriftFile(file.toPath()) }
        return loader.load()
    }
}
