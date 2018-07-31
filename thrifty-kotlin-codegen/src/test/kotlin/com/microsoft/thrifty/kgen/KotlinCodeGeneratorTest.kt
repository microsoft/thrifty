package com.microsoft.thrifty.kgen

import com.microsoft.thrifty.schema.FieldNamingPolicy
import com.microsoft.thrifty.schema.Loader
import com.microsoft.thrifty.schema.Schema
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import io.kotlintest.shouldBe
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

            const i32 FooNum = 42

            const string ConstStr = "wtf"

            const list<string> ConstStringList = ["wtf", "mate"]
            const map<string, list<string>> Weird = { "foo": ["a", "s", "d", "f"],
                                                      "bar": ["q", "w", "e", "r"] }
            //const binary ConstBin = "DEADBEEF"

            struct Test {
              1: required string Foo (thrifty.redacted = "1");
              2: required map<i64, string> Numbers (thrifty.obfuscated = "1");
              3: optional string Bar;
              5: optional set<list<double>> Bs = [[1.0], [2.0], [3.0], [4.0]];
              6: MyEnum enumType;
              7: set<i8> Bytes;
              8: list<list<string>> listOfStrings
            }

            struct AnotherOne {
              1: optional i32 NumBitTheDust = 900
            }
        """.trimIndent())

        val files = KotlinCodeGenerator(FieldNamingPolicy.JAVA).generate(schema)

        files.forEach { println("$it") }
    }

    @Test fun `output styles work as advertised`() {
        val thrift = """
            namespace kt com.test

            enum AnEnum {
              ONE,
              TWO
            }

            struct AStruct {
              1: optional string ssn;
            }
        """.trimIndent()

        val schema = load(thrift)
        val gen = KotlinCodeGenerator()

        // Default should be one file per namespace
        gen.outputStyle shouldBe KotlinCodeGenerator.OutputStyle.FILE_PER_NAMESPACE
        gen.generate(schema).size shouldBe  1

        gen.outputStyle = KotlinCodeGenerator.OutputStyle.FILE_PER_TYPE
        gen.generate(schema).size shouldBe 2
    }

    @Test fun `file-per-type puts constants into a file named 'Constants'`() {
        val thrift = """
            namespace kt com.test

            const i32 ONE = 1;
            const i64 TWO = 2;
            const string THREE = "three";
        """.trimIndent()

        val schema = load(thrift)
        val gen = KotlinCodeGenerator().filePerType()
        val specs = gen.generate(schema)

        specs.single().name shouldBe "Constants" // ".kt" suffix is appended when the file is written out
    }

    @Test fun `empty structs get default equals, hashcode, and toString methods`() {
        val thrift = """
            namespace kt com.test

            struct Empty {}
        """.trimIndent()

        val specs = generate(thrift)

        val struct = specs.single().members.single() as TypeSpec

        println(specs.single())

        struct.name shouldBe "Empty"
        struct.modifiers.any { it == KModifier.DATA } shouldBe false
        struct.funSpecs.any { it.name == "toString" } shouldBe true
        struct.funSpecs.any { it.name == "hashCode" } shouldBe true
        struct.funSpecs.any { it.name == "equals"   } shouldBe true
    }

    @Test fun `Non-empty structs are data classes`() {
        val thrift = """
            namespace kt com.test

            struct NonEmpty {
              1: required i32 Number
            }
        """.trimIndent()

        val specs = generate(thrift)

        val struct = specs.single().members.single() as TypeSpec

        struct.name shouldBe "NonEmpty"
        struct.modifiers.any { it == KModifier.DATA } shouldBe true
        struct.funSpecs.any { it.name == "toString" } shouldBe false
        struct.funSpecs.any { it.name == "hashCode" } shouldBe false
        struct.funSpecs.any { it.name == "equals"   } shouldBe false
    }

    private fun generate(thrift: String): List<FileSpec> {
        return KotlinCodeGenerator().generate(load(thrift))
    }

    private fun load(thrift: String): Schema {
        val file = tempDir.newFile("test.thrift")
        file.writeText(thrift)
        val loader = Loader().apply { addThriftFile(file.toPath()) }
        return loader.load()
    }
}
