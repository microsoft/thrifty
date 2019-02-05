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
package com.microsoft.thrifty.kgen

import com.microsoft.thrifty.schema.FieldNamingPolicy
import com.microsoft.thrifty.schema.Loader
import com.microsoft.thrifty.schema.Schema
import com.microsoft.thrifty.service.ServiceMethodCallback
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import io.kotlintest.shouldNot
import io.kotlintest.matchers.string.contain
import io.kotlintest.should
import io.kotlintest.shouldBe
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class KotlinCodeGeneratorTest {
    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `struct to data class`() {
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

    @Test
    fun `output styles work as advertised`() {
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
        gen.generate(schema).size shouldBe 1

        gen.outputStyle = KotlinCodeGenerator.OutputStyle.FILE_PER_TYPE
        gen.generate(schema).size shouldBe 2
    }

    @Test
    fun `file-per-type puts constants into a file named 'Constants'`() {
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

    @Test
    fun `empty structs get default equals, hashcode, and toString methods`() {
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
        struct.funSpecs.any { it.name == "equals" } shouldBe true
    }

    @Test
    fun `Non-empty structs are data classes`() {
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
        struct.funSpecs.any { it.name == "equals" } shouldBe false
    }

    @Test
    fun `exceptions with reserved field names get renamed fields`() {
        val thrift = """
            namespace kt com.test

            exception Fail { 1: required list<i32> Message }
        """.trimIndent()

        val schema = load(thrift)
        val specs = KotlinCodeGenerator(FieldNamingPolicy.JAVA).generate(schema)
        val xception = specs.single().members.single() as TypeSpec
        xception.propertySpecs.single().name shouldBe "message_"
    }

    @Test
    fun services() {
        val thrift = """
            namespace kt test.services

            struct Foo { 1: required string foo; }
            struct Bar { 1: required string bar; }
            exception X { 1: required string message; }
            service Svc {
              void doThingOne(1: Foo foo) throws (2: X xxxx)
              Bar doThingTwo(1: Foo foo) throws (1: X x)

            }
        """.trimIndent()

        generate(thrift).forEach { println(it) }
    }

    @Test
    fun `typedefs become typealiases`() {
        val thrift = """
            namespace kt test.typedefs

            typedef map<i32, map<string, double>> FooMap;

            struct HasMap {
              1: optional FooMap theMap;
            }
        """.trimIndent()

        generate(thrift).forEach { println(it) }
    }

    @Test
    fun `services that return typedefs`() {
        val thrift = """
            namespace kt test.typedefs

            typedef i32 TheNumber;
            service Foo {
              TheNumber doIt()
            }
        """.trimIndent()

        val file = generate(thrift).single()
        val svc = file.members.first { it is TypeSpec && it.name == "Foo" } as TypeSpec
        val method = svc.funSpecs.single()
        method.name shouldBe "doIt"
        method.parameters.single().type shouldBe ServiceMethodCallback::class
                .asTypeName()
                .parameterizedBy(ClassName("test.typedefs", "TheNumber"))
    }

    @Test
    fun `constants that are typedefs`() {
        val thrift = """
            |namespace kt test.typedefs
            |
            |typedef map<i32, i32> Weights
            |
            |const Weights WEIGHTS = {1: 2}
        """.trimMargin()

        "${generate(thrift).single()}" shouldBe """
            |package test.typedefs
            |
            |import kotlin.Int
            |import kotlin.collections.Map
            |
            |typealias Weights = Map<Int, Int>
            |
            |val WEIGHTS: Weights = mapOf(1 to 2)
            |
        """.trimMargin()
    }

    @Test
    fun `Parcelize annotations for structs and enums`() {
        val thrift = """
            |namespace kt test.parcelize
            |
            |struct Foo { 1: required i32 Number; 2: optional string Text }
            |
            |enum AnEnum { ONE; TWO; THREE }
            |
            |service Svc {
            |  Foo getFoo(1: AnEnum anEnum)
            |}
        """.trimMargin()

        val file = generate(thrift) { parcelize() }.single()
        val struct = file.members.single { it is TypeSpec && it.name == "Foo" } as TypeSpec
        val anEnum = file.members.single { it is TypeSpec && it.name == "AnEnum" } as TypeSpec
        val svc = file.members.single { it is TypeSpec && it.name == "SvcClient" } as TypeSpec

        val parcelize = ClassName("kotlinx.android.parcel", "Parcelize")

        struct.annotations.any { it.type == parcelize } shouldBe true
        anEnum.annotations.any { it.type == parcelize } shouldBe true
        svc.annotations.any { it.type == parcelize } shouldBe false
    }

    @Test
    fun `Custom map-type constants`() {
        val thrift = """
            |namespace kt test.map_consts
            |
            |const map<i32, list<string>> Maps = {1: [], 2: ["foo"]}
        """.trimMargin()

        val text = generate(thrift) { mapClassName("android.support.v4.util.ArrayMap") }
                .single()
                .toString()

        text shouldBe """
            |package test.map_consts
            |
            |import android.support.v4.util.ArrayMap
            |import kotlin.Int
            |import kotlin.String
            |import kotlin.collections.List
            |import kotlin.collections.Map
            |
            |val Maps: Map<Int, List<String>> = ArrayMap<Int, List<String>>(2).apply {
            |            put(1, emptyList())
            |            put(2, listOf("foo"))
            |        }
            |
            """.trimMargin()
    }

    @Test
    fun `suspend-fun service clients`() {
        val thrift = """
            |namespace kt test.coro
            |
            |service Svc {
            |  i32 doSomething(1: i32 foo);
            |}
        """.trimMargin()

        val file = generate(thrift) { coroutineServiceClients() }

        file.single().toString() should contain("""
            |interface Svc {
            |    suspend fun doSomething(foo: Int): Int
            |}
            |
            |class SvcClient(protocol: Protocol, listener: AsyncClientBase.Listener) : AsyncClientBase(protocol, listener),
            |        Svc {
            |    override suspend fun doSomething(foo: Int): Int = suspendCoroutine { cont ->
            |        this.enqueue(DoSomethingCall(foo, object : ServiceMethodCallback<Int> {
            |            override fun onSuccess(result: Int) {
            |                cont.resumeWith(Result.success(result))
            |            }
            |
            |            override fun onError(error: Throwable) {
            |                cont.resumeWith(Result.failure(error))
            |            }
            |        }))
            |    }
            |
        """.trimMargin())
    }


    @Test
    fun `union generate sealed`() {
        val thrift = """
            |namespace kt test.coro
            |
            |union Union {
            |  1: i32 Foo;
            |  2: i64 Bar;
            |  3: string Baz;
            |  4: i32 NotFoo;
            |}
        """.trimMargin()

        val file = generate(thrift) { coroutineServiceClients() }

        file.single().toString() should contain("""
            |sealed class Union : Struct {
        """.trimMargin())
    }

    @Test
    fun `union properties as data`() {
        val thrift = """
            |namespace kt test.coro
            |
            |union Union {
            |  1: i32 Foo;
            |  2: i64 Bar;
            |  3: string Baz;
            |  4: i32 NotFoo;
            |}
        """.trimMargin()

        val file = generate(thrift) { coroutineServiceClients() }

        file.single().toString() should contain("""
            |
            |    data class Foo(var value: Int?) : Union()
            |
            |    data class Bar(var value: Long?) : Union()
            |
            |    data class Baz(var value: String?) : Union()
            |
            |    data class NotFoo(var value: Int?) : Union()
            |
        """.trimMargin())
    }

    @Test
    fun `union has builder`() {
        val thrift = """
            |namespace kt test.coro
            |
            |union Union {
            |  1: i32 Foo;
            |  2: i64 Bar;
            |  3: string Baz;
            |  4: i32 NotFoo;
            |}
        """.trimMargin()

        val file = generate(thrift) { coroutineServiceClients() }

        file.single().toString() should contain("""
            |    class Builder : StructBuilder<Union> {
            |        private var Foo: Int?
            |
            |        private var Bar: Long?
            |
            |        private var Baz: String?
            |
            |        private var NotFoo: Int?
            |
            |        constructor() {
            |            this.Foo = null
            |            this.Bar = null
            |            this.Baz = null
            |            this.NotFoo = null
            |        }
            |
            |        constructor(source: Union) : this() {
            |            when(source) {
            |                is Foo -> this.Foo = source.value
            |                is Bar -> this.Bar = source.value
            |                is Baz -> this.Baz = source.value
            |                is NotFoo -> this.NotFoo = source.value
            |                else -> error("unpossible")
            |            }
            |        }
            |
            |        fun Foo(value: Int) = apply {
            |            this.Foo = value
            |            this.Bar = null
            |            this.Baz = null
            |            this.NotFoo = null
            |        }
            |
            |        fun Bar(value: Long) = apply {
            |            this.Foo = null
            |            this.Bar = value
            |            this.Baz = null
            |            this.NotFoo = null
            |        }
            |
            |        fun Baz(value: String) = apply {
            |            this.Foo = null
            |            this.Bar = null
            |            this.Baz = value
            |            this.NotFoo = null
            |        }
            |
            |        fun NotFoo(value: Int) = apply {
            |            this.Foo = null
            |            this.Bar = null
            |            this.Baz = null
            |            this.NotFoo = value
            |        }
            |
            |        override fun build(): Union = when {
            |            Foo != null -> Union.Foo(Foo)
            |            Bar != null -> Union.Bar(Bar)
            |            Baz != null -> Union.Baz(Baz)
            |            NotFoo != null -> Union.NotFoo(NotFoo)
            |            else -> error("unpossible")
            |        }
            |
            |        override fun reset() {
            |            this.Foo = null
            |            this.Bar = null
            |            this.Baz = null
            |            this.NotFoo = null
            |        }
            |    }
        """.trimMargin())
    }

    @Test
    fun `union wont generate builder when disabled`() {
        val thrift = """
            |namespace kt test.coro
            |
            |union Union {
            |  1: i32 Foo;
            |  2: i64 Bar;
            |  3: string Baz;
            |  4: i32 NotFoo;
            |}
        """.trimMargin()

        val file = generate(thrift) { builderlessDataClasses() }

        file.single().toString() shouldNot contain("""
            |    class Builder
        """.trimMargin())
    }

    @Ignore
    @Test
    fun `union wont generate struct when disabled`() {
        val thrift = """
            |namespace kt test.coro
            |
            |union Union {
            |  1: i32 Foo;
            |  2: i64 Bar;
            |  3: string Baz;
            |  4: i32 NotFoo;
            |}
        """.trimMargin()

        val file = generate(thrift) //{ shouldImplementStruct() }

        file.single().toString() shouldNot contain("""
            |    : Struct
        """.trimMargin())

        file.single().toString() shouldNot contain("""
            |    write
        """.trimMargin())
    }

    @Test
    fun `union generate write function`() {
        val thrift = """
            |namespace kt test.coro
            |
            |union Union {
            |  1: i32 Foo;
            |  2: i64 Bar;
            |  3: string Baz;
            |  4: i32 NotFoo;
            |}
        """.trimMargin()

        val file = generate(thrift) //{ shouldImplementStruct() }

        file.single().toString() should contain("""            |
            |        override fun write(protocol: Protocol, struct: Union) {
            |            protocol.writeStructBegin("Union")
            |            if (struct is Foo) {
            |                protocol.writeFieldBegin("Foo", 1, TType.I32)
            |                protocol.writeI32(struct.value!!)
            |                protocol.writeFieldEnd()
            |            }
            |            if (struct is Bar) {
            |                protocol.writeFieldBegin("Bar", 2, TType.I64)
            |                protocol.writeI64(struct.value!!)
            |                protocol.writeFieldEnd()
            |            }
            |            if (struct is Baz) {
            |                protocol.writeFieldBegin("Baz", 3, TType.STRING)
            |                protocol.writeString(struct.value!!)
            |                protocol.writeFieldEnd()
            |            }
            |            if (struct is NotFoo) {
            |                protocol.writeFieldBegin("NotFoo", 4, TType.I32)
            |                protocol.writeI32(struct.value!!)
            |                protocol.writeFieldEnd()
            |            }
            |            protocol.writeFieldStop()
            |            protocol.writeStructEnd()
            |        }
            |    }
        """.trimMargin())
    }

    @Test
    fun `union generate read function`() {
        val thrift = """
            |namespace kt test.coro
            |
            |union Union {
            |  1: i32 Foo;
            |  2: i64 Bar;
            |  3: string Baz;
            |  4: i32 NotFoo;
            |}
        """.trimMargin()

        val file = generate(thrift) //{ shouldImplementStruct() }

        file.single().toString() should contain("""
            |        override fun read(protocol: Protocol) = read(protocol, Builder())
            |
            |        override fun read(protocol: Protocol, builder: Builder): Union {
            |            protocol.readStructBegin()
            |            while (true) {
            |                val fieldMeta = protocol.readFieldBegin()
            |                if (fieldMeta.typeId == TType.STOP) {
            |                    break
            |                }
            |                when (fieldMeta.fieldId.toInt()) {
            |                    1 -> {
            |                        if (fieldMeta.typeId == TType.I32) {
            |                            val Foo = protocol.readI32()
            |                            builder.Foo(Foo)
            |                        } else {
            |                            ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |                        }
            |                    }
            |                    2 -> {
            |                        if (fieldMeta.typeId == TType.I64) {
            |                            val Bar = protocol.readI64()
            |                            builder.Bar(Bar)
            |                        } else {
            |                            ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |                        }
            |                    }
            |                    3 -> {
            |                        if (fieldMeta.typeId == TType.STRING) {
            |                            val Baz = protocol.readString()
            |                            builder.Baz(Baz)
            |                        } else {
            |                            ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |                        }
            |                    }
            |                    4 -> {
            |                        if (fieldMeta.typeId == TType.I32) {
            |                            val NotFoo = protocol.readI32()
            |                            builder.NotFoo(NotFoo)
            |                        } else {
            |                            ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |                        }
            |                    }
            |                    else -> ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |                }
            |                protocol.readFieldEnd()
            |            }
            |            protocol.readStructEnd()
            |            return builder.build()
            |        }
        """.trimMargin())
    }

    @Test
    fun `union generate read function without builder`() {
        val thrift = """
            |namespace kt test.coro
            |
            |union Union {
            |  1: i32 Foo;
            |  2: i64 Bar;
            |  3: string Baz;
            |  4: i32 NotFoo;
            |}
        """.trimMargin()

        val file = generate(thrift) { builderlessDataClasses() }


        file.single().toString() should contain("""
            |        override fun read(protocol: Protocol): Union {
            |            protocol.readStructBegin()
            |            var result : Union? = null
            |            while (true) {
            |                val fieldMeta = protocol.readFieldBegin()
            |                if (fieldMeta.typeId == TType.STOP) {
            |                    break
            |                }
            |                when (fieldMeta.fieldId.toInt()) {
            |                    1 -> {
            |                        if (fieldMeta.typeId == TType.I32) {
            |                            val Foo = protocol.readI32()
            |                            result = Foo(Foo)
            |                        } else {
            |                            ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |                        }
            |                    }
            |                    2 -> {
            |                        if (fieldMeta.typeId == TType.I64) {
            |                            val Bar = protocol.readI64()
            |                            result = Bar(Bar)
            |                        } else {
            |                            ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |                        }
            |                    }
            |                    3 -> {
            |                        if (fieldMeta.typeId == TType.STRING) {
            |                            val Baz = protocol.readString()
            |                            result = Baz(Baz)
            |                        } else {
            |                            ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |                        }
            |                    }
            |                    4 -> {
            |                        if (fieldMeta.typeId == TType.I32) {
            |                            val NotFoo = protocol.readI32()
            |                            result = NotFoo(NotFoo)
            |                        } else {
            |                            ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |                        }
            |                    }
            |                    else -> ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |                }
            |                protocol.readFieldEnd()
            |            }
            |            protocol.readStructEnd()
            |            if (null == result) {
            |                throw IllegalStateException("unreadable")
            |            } else {
            |                return result
            |            }
            |        }
        """.trimMargin())
    }

    @Test
    fun `union generate Adapter with builder`() {
        val thrift = """
            |namespace kt test.coro
            |
            |union Union {
            |  1: i32 Foo;
            |  2: i64 Bar;
            |  3: string Baz;
            |  4: i32 NotFoo;
            |}
        """.trimMargin()

        val file = generate(thrift)

        file.single().toString() should contain("""
            |    private class UnionAdapter : Adapter<Union, Builder> {
        """.trimMargin())
    }

    @Test
    fun `union generate Adapter`() {
        val thrift = """
            |namespace kt test.coro
            |
            |union Union {
            |  1: i32 Foo;
            |  2: i64 Bar;
            |  3: string Baz;
            |  4: i32 NotFoo;
            |}
        """.trimMargin()

        val file = generate(thrift) { builderlessDataClasses() }

        file.single().toString() should contain("""
            |    private class UnionAdapter : Adapter<Union> {
        """.trimMargin())
    }

    @Test
    fun `empty union generate non-sealed class`() {
        val thrift = """
            |namespace kt test.coro
            |
            |union Union {
            |}
        """.trimMargin()

        val file = generate(thrift) { coroutineServiceClients() }

        file.single().toString() should contain("""
            |class Union() : Struct {
        """.trimMargin())
    }

    @Test
    fun `struct with union`() {
        val thrift = """
            |namespace kt test.coro
            |
            |struct Bonk {
            |  1: string message;
            |  2: i32 type;
            |}
            |
            |union UnionStruct {
            |  1: Bonk Struct
            |}
        """.trimMargin()

        val file = generate(thrift) { coroutineServiceClients() }

        file.single().toString() should contain("""
            |sealed class UnionStruct : Struct {
            |    override fun write(protocol: Protocol) {
            |        ADAPTER.write(protocol, this)
            |    }
            |
            |    data class Struct(var value: Bonk?) : UnionStruct()
            |
            |    class Builder : StructBuilder<UnionStruct> {
            |        private var Struct: Bonk?
            |
            |        constructor() {
            |            this.Struct = null
            |        }
            |
            |        constructor(source: UnionStruct) : this() {
            |            when(source) {
            |                is Struct -> this.Struct = source.value
            |                else -> error("unpossible")
            |            }
            |        }
            |
            |        fun Struct(value: Bonk) = apply {
            |            this.Struct = value
            |        }
            |
            |        override fun build(): UnionStruct = when {
            |            Struct != null -> UnionStruct.Struct(Struct)
            |            else -> error("unpossible")
            |        }
            |
            |        override fun reset() {
            |            this.Struct = null
            |        }
            |    }
            |
            |    private class UnionStructAdapter : Adapter<UnionStruct, Builder> {
            |        override fun read(protocol: Protocol) = read(protocol, Builder())
            |
            |        override fun read(protocol: Protocol, builder: Builder): UnionStruct {
            |            protocol.readStructBegin()
            |            while (true) {
            |                val fieldMeta = protocol.readFieldBegin()
            |                if (fieldMeta.typeId == TType.STOP) {
            |                    break
            |                }
            |                when (fieldMeta.fieldId.toInt()) {
            |                    1 -> {
            |                        if (fieldMeta.typeId == TType.STRUCT) {
            |                            val Struct = Bonk.ADAPTER.read(protocol)
            |                            builder.Struct(Struct)
            |                        } else {
            |                            ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |                        }
            |                    }
            |                    else -> ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |                }
            |                protocol.readFieldEnd()
            |            }
            |            protocol.readStructEnd()
            |            return builder.build()
            |        }
            |
            |        override fun write(protocol: Protocol, struct: UnionStruct) {
            |            protocol.writeStructBegin("UnionStruct")
            |            if (struct is Struct) {
            |                protocol.writeFieldBegin("Struct", 1, TType.STRUCT)
            |                Bonk.ADAPTER.write(protocol, struct.value!!)
            |                protocol.writeFieldEnd()
            |            }
            |            protocol.writeFieldStop()
            |            protocol.writeStructEnd()
            |        }
            |    }
        """.trimMargin())
    }

    private fun generate(thrift: String, config: (KotlinCodeGenerator.() -> KotlinCodeGenerator)? = null): List<FileSpec> {
        val configOrDefault = config ?: { this }
        return KotlinCodeGenerator()
                .run(configOrDefault)
                .generate(load(thrift))
    }

    private fun load(thrift: String): Schema {
        val file = tempDir.newFile("test.thrift").also { it.writeText(thrift) }
        val loader = Loader().apply { addThriftFile(file.toPath()) }
        return loader.load()
    }
}
