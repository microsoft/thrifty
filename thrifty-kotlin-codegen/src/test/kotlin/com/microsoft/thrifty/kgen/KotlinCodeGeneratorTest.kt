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
              1: required string Foo;
              2: required list<i64> Numbers;
            }
        """.trimIndent())

        val gen = KotlinCodeGenerator()
        val type = gen.generateDataClass(schema.structs[0])
        val fileSpec = FileSpec.builder("com.test", "Test.kt")
                .addType(type)
                .build()
        println(fileSpec.toString())
    }

    private fun load(thrift: String): Schema {
        val file = tempDir.newFile("test.thrift")
        file.writeText(thrift)
        val loader = Loader().apply { addThriftFile(file.toPath()) }
        return loader.load()
    }
}
