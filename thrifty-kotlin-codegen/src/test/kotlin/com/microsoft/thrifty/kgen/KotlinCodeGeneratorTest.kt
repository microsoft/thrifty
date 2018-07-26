package com.microsoft.thrifty.kgen

import com.microsoft.thrifty.schema.Loader
import com.microsoft.thrifty.schema.Schema
import com.squareup.kotlinpoet.FileSpec
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class KotlinCodeGeneratorTest {
    @get:Rule val tempDir = TemporaryFolder()

    @Test fun `struct to data class`() {
        val schema = load("""
            namespace kt com.test

            struct Test {
              1: required string Foo (thrifty.redacted = "1");
              2: required map<i64, string> Numbers (thrift.obfuscated = "1");
              3: optional string Bar;
            }

            struct AnotherOne {
              1: optional i32 NumBitTheDust
            }
        """.trimIndent())

        val files = KotlinCodeGenerator().generate(schema)

        files.forEach { println("$it")}
    }

    private fun load(thrift: String): Schema {
        val file = tempDir.newFile("test.thrift")
        file.writeText(thrift)
        val loader = Loader().apply { addThriftFile(file.toPath()) }
        return loader.load()
    }
}
