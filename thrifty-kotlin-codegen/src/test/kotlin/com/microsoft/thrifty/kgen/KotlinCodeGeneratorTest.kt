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
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.contain
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KotlinCodeGeneratorTest {
    @TempDir
    lateinit var tempDir: File

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

        struct.annotationSpecs.any { it.typeName == parcelize } shouldBe true
        anEnum.annotationSpecs.any { it.typeName == parcelize } shouldBe true
        svc.annotationSpecs.any { it.typeName == parcelize } shouldBe false
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
            |      put(1, emptyList())
            |      put(2, listOf("foo"))
            |    }
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
            |  suspend fun doSomething(foo: Int): Int
            |}
            |
            |class SvcClient(
            |  protocol: Protocol,
            |  listener: AsyncClientBase.Listener
            |) : AsyncClientBase(protocol, listener), Svc {
            |  override suspend fun doSomething(foo: Int): Int = suspendCoroutine { cont ->
            |    this.enqueue(DoSomethingCall(foo, object : ServiceMethodCallback<Int> {
            |      override fun onSuccess(result: Int) {
            |        cont.resumeWith(Result.success(result))
            |      }
            |
            |      override fun onError(error: Throwable) {
            |        cont.resumeWith(Result.failure(error))
            |      }
            |    }))
            |  }
            |
        """.trimMargin())
    }

    @Test
    fun `omit service clients`() {
        val thrift = """
            |namespace kt test.omit_service_clients
            |
            |service Svc {
            |  i32 doSomething(1: i32 foo);
            |}
        """.trimMargin()

        val file = generate(thrift) { omitServiceClients() }

        file shouldBe emptyList()
    }

    @Test
    fun `Emit @JvmName file-per-namespace annotations`() {
        val thrift = """
            |namespace kt test.consts
            |
            |const i32 FooNum = 42
        """.trimMargin()

        val text = generate(thrift) {
                    emitJvmName()
                    filePerNamespace()
                }
                .single()
                .toString()

        text shouldBe """
            |@file:JvmName("ThriftTypes")
            |
            |package test.consts
            |
            |import kotlin.Int
            |import kotlin.jvm.JvmName
            |
            |const val FooNum: Int = 42
            |
            """.trimMargin()
    }

    @Test
    fun `Emit @JvmName file-per-type annotations`() {
        val thrift = """
            |namespace kt test.consts
            |
            |const i32 FooNum = 42
        """.trimMargin()

        val text = generate(thrift) {
                    emitJvmName()
                    filePerType()
                }
                .single()
                .toString()

        text shouldBe """
            |@file:JvmName("Constants")
            |
            |package test.consts
            |
            |import kotlin.Int
            |import kotlin.jvm.JvmName
            |
            |const val FooNum: Int = 42
            |
            """.trimMargin()
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
            |  data class Foo(
            |    val value: Int
            |  ) : Union() {
            |    override fun toString(): String = "Union(Foo=${'$'}value)"}
            |
            |  data class Bar(
            |    val value: Long
            |  ) : Union() {
            |    override fun toString(): String = "Union(Bar=${'$'}value)"}
            |
            |  data class Baz(
            |    val value: String
            |  ) : Union() {
            |    override fun toString(): String = "Union(Baz=${'$'}value)"}
            |
            |  data class NotFoo(
            |    val value: Int
            |  ) : Union() {
            |    override fun toString(): String = "Union(NotFoo=${'$'}value)"}
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

        val file = generate(thrift) { withDataClassBuilders() }

        file.single().toString() should contain("""
            |  class Builder : StructBuilder<Union> {
            |    private var value: Union? = null
            |
            |    constructor()
            |
            |    constructor(source: Union) : this() {
            |      this.value = source
            |    }
            |
            |    override fun build(): Union = value ?: error("Invalid union; at least one value is required")
            |
            |    override fun reset() {
            |      value = null
            |    }
            |
            |    fun Foo(value: Int) = apply { this.value = Union.Foo(value) }
            |
            |    fun Bar(value: Long) = apply { this.value = Union.Bar(value) }
            |
            |    fun Baz(value: String) = apply { this.value = Union.Baz(value) }
            |
            |    fun NotFoo(value: Int) = apply { this.value = Union.NotFoo(value) }
            |  }
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

        val file = generate(thrift)

        file.single().toString() shouldNot contain("""
            |    class Builder
        """.trimMargin())
    }

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
            |  : Struct
        """.trimMargin())

        file.single().toString() shouldNot contain("""
            |  write
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

        val file = generate(thrift) { withDataClassBuilders() }

        file.single().toString() should contain("""
            |    override fun read(protocol: Protocol) = read(protocol, Builder())
            |
            |    override fun read(protocol: Protocol, builder: Builder): Union {
            |      protocol.readStructBegin()
            |      while (true) {
            |        val fieldMeta = protocol.readFieldBegin()
            |        if (fieldMeta.typeId == TType.STOP) {
            |          break
            |        }
            |        when (fieldMeta.fieldId.toInt()) {
            |          1 -> {
            |            if (fieldMeta.typeId == TType.I32) {
            |              val Foo = protocol.readI32()
            |              builder.Foo(Foo)
            |            } else {
            |              ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |            }
            |          }
            |          2 -> {
            |            if (fieldMeta.typeId == TType.I64) {
            |              val Bar = protocol.readI64()
            |              builder.Bar(Bar)
            |            } else {
            |              ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |            }
            |          }
            |          3 -> {
            |            if (fieldMeta.typeId == TType.STRING) {
            |              val Baz = protocol.readString()
            |              builder.Baz(Baz)
            |            } else {
            |              ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |            }
            |          }
            |          4 -> {
            |            if (fieldMeta.typeId == TType.I32) {
            |              val NotFoo = protocol.readI32()
            |              builder.NotFoo(NotFoo)
            |            } else {
            |              ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |            }
            |          }
            |          else -> ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |        }
            |        protocol.readFieldEnd()
            |      }
            |      protocol.readStructEnd()
            |      return builder.build()
            |    }
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

        val file = generate(thrift)


        file.single().toString() should contain("""
            |    override fun read(protocol: Protocol): Union {
            |      protocol.readStructBegin()
            |      var result : Union? = null
            |      while (true) {
            |        val fieldMeta = protocol.readFieldBegin()
            |        if (fieldMeta.typeId == TType.STOP) {
            |          break
            |        }
            |        when (fieldMeta.fieldId.toInt()) {
            |          1 -> {
            |            if (fieldMeta.typeId == TType.I32) {
            |              val Foo = protocol.readI32()
            |              result = Foo(Foo)
            |            } else {
            |              ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |            }
            |          }
            |          2 -> {
            |            if (fieldMeta.typeId == TType.I64) {
            |              val Bar = protocol.readI64()
            |              result = Bar(Bar)
            |            } else {
            |              ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |            }
            |          }
            |          3 -> {
            |            if (fieldMeta.typeId == TType.STRING) {
            |              val Baz = protocol.readString()
            |              result = Baz(Baz)
            |            } else {
            |              ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |            }
            |          }
            |          4 -> {
            |            if (fieldMeta.typeId == TType.I32) {
            |              val NotFoo = protocol.readI32()
            |              result = NotFoo(NotFoo)
            |            } else {
            |              ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |            }
            |          }
            |          else -> ProtocolUtil.skip(protocol, fieldMeta.typeId)
            |        }
            |        protocol.readFieldEnd()
            |      }
            |      protocol.readStructEnd()
            |      return result ?: error("unreadable")
            |    }
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

        val file = generate(thrift) { withDataClassBuilders() }

        file.single().toString() should contain("""
            |  private class UnionAdapter : Adapter<Union, Builder> {
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

        val file = generate(thrift)

        file.single().toString() should contain("""
            |  private class UnionAdapter : Adapter<Union> {
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
            |class Union : Struct {
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

        val file = generate(thrift) { withDataClassBuilders() }

        file.single().toString() should contain("""
            |sealed class UnionStruct : Struct {
            |  override fun write(protocol: Protocol) {
            |    ADAPTER.write(protocol, this)
            |  }
            |
            |  data class Struct(
            |    val value: Bonk
            |  ) : UnionStruct() {
            |    override fun toString(): String = "UnionStruct(Struct=${'$'}value)"}
            |
            |  class Builder : StructBuilder<UnionStruct> {
        """.trimMargin())
    }

    @Test
    fun `union with default value`() {
        val thrift = """
            namespace kt test.union

            union HasDefault {
                1: i8 b;
                2: i16 short;
                3: i32 int = 16;
                4: i64 long;
            }
        """.trimIndent()

        val file = generate(thrift)

        file.single().toString() should contain("""
            |    @JvmField
            |    val DEFAULT: HasDefault = Int(16)
        """.trimMargin())
    }

    @Test
    fun `builder has correct syntax`() {
        val thrift = """
            |namespace kt test.builder
            |
            |struct Bonk {
            |  1: string message;
            |  2: i32 type;
            |}
        """.trimMargin()

        val file = generate(thrift) { withDataClassBuilders() }

        file.single().toString() should contain("""
            |    override fun build(): Bonk = Bonk(message = this.message, type = this.type)
        """.trimMargin())
    }

    @Test
    fun `enum fail on unknown value`() {
        val thrift = """
            |namespace kt test.struct
            |
            |enum TestEnum { FOO }
            |
            |struct HasEnum {
            |  1: optional TestEnum field = TestEnum.FOO;
            |}
        """.trimMargin()

        val expected = """
          1 -> {
            if (fieldMeta.typeId == TType.I32) {
              val field = protocol.readI32().let {
                TestEnum.findByValue(it) ?: throw
                    ThriftException(ThriftException.Kind.PROTOCOL_ERROR,
                    "Unexpected value for enum type TestEnum: ${'$'}it")
              }
              builder.field(field)
            } else {
              ProtocolUtil.skip(protocol, fieldMeta.typeId)
            }
          }"""

        val file = generate(thrift) { withDataClassBuilders() }
        file.single().toString() shouldContain expected
    }

    @Test
    fun `enum don't fail on unknown value`() {
        val thrift = """
            |namespace kt test.struct
            |
            |enum TestEnum { FOO }
            |
            |struct HasEnum {
            |  1: optional TestEnum field1 = TestEnum.FOO;
            |  2: required TestEnum field2 = TestEnum.FOO;
            |}
        """.trimMargin()

        val expected = """
          1 -> {
            if (fieldMeta.typeId == TType.I32) {
              val field1 = protocol.readI32().let {
                TestEnum.findByValue(it)
              }
              field1?.let {
                builder.field1(it)
              }
            } else {
              ProtocolUtil.skip(protocol, fieldMeta.typeId)
            }
          }
          2 -> {
            if (fieldMeta.typeId == TType.I32) {
              val field2 = protocol.readI32().let {
                TestEnum.findByValue(it) ?: throw
                    ThriftException(ThriftException.Kind.PROTOCOL_ERROR,
                    "Unexpected value for enum type TestEnum: ${'$'}it")
              }
              builder.field2(field2)
            } else {
              ProtocolUtil.skip(protocol, fieldMeta.typeId)
            }
          }"""

        val file = generate(thrift) {
            withDataClassBuilders()
            failOnUnknownEnumValues(false)
        }
        file.single().toString() shouldContain expected
    }

    @Test
    fun `enum don't fail on unknown value without builder`() {
        val thrift = """
            |namespace kt test.struct
            |
            |enum TestEnum { FOO }
            |
            |struct HasEnum {
            |  1: optional TestEnum field1 = TestEnum.FOO;
            |  2: required TestEnum field2 = TestEnum.FOO;
            |}
        """.trimMargin()

        val expected = """
          1 -> {
            if (fieldMeta.typeId == TType.I32) {
              val field1 = protocol.readI32().let {
                TestEnum.findByValue(it)
              }
              field1?.let {
                _local_field1 = it
              }
            } else {
              ProtocolUtil.skip(protocol, fieldMeta.typeId)
            }
          }
          2 -> {
            if (fieldMeta.typeId == TType.I32) {
              val field2 = protocol.readI32().let {
                TestEnum.findByValue(it) ?: throw
                    ThriftException(ThriftException.Kind.PROTOCOL_ERROR,
                    "Unexpected value for enum type TestEnum: ${'$'}it")
              }
              _local_field2 = field2
            } else {
              ProtocolUtil.skip(protocol, fieldMeta.typeId)
            }
          }"""

        val file = generate(thrift) { failOnUnknownEnumValues(false) }
        file.single().toString() shouldContain expected
    }

    @Test
    fun `struct built with required constructor`() {
        val thrift = """
            |namespace kt test.struct
            |
            |struct TestStruct {
            |  1: required i64 field1;
            |  2: optional bool field2;
            |}
        """.trimMargin()

        val expected = """
    constructor(field1: Long) {
      this.field1 = field1
      this.field2 = null
    }"""

        val file = generate(thrift) {
            withDataClassBuilders()
            builderRequiredConstructor()
        }
        file.single().toString() shouldContain expected
    }

    @Test
    fun `default constructor marked deprecated when required constructor enabled`() {
        val thrift = """
            |namespace kt test.struct
            |
            |struct TestStruct {
            |  1: required i64 field1;
            |  2: optional bool field2;
            |}
        """.trimMargin()

        val expected = """
    @Deprecated(
      message = "Empty constructor deprecated, use required constructor instead",
      replaceWith = ReplaceWith("Builder(field1)")
    )"""

        val file = generate(thrift) {
            withDataClassBuilders()
            builderRequiredConstructor()
        }
        file.single().toString() shouldContain expected
    }

    @Test
    fun `omit required constructor when no required parameters supplied`() {
        val thrift = """
            |namespace kt test.struct
            |
            |struct TestStruct {
            |  1: optional i64 field1;
            |  2: optional bool field2;
            |}
        """.trimMargin()

        val notExpected = "@Deprecated("

        val expected = """
    constructor() {
      this.field1 = null
      this.field2 = null
    }"""

        val file = generate(thrift) {
            withDataClassBuilders()
            builderRequiredConstructor()
        }
        file.single().toString() shouldContain expected
        file.single().toString() shouldNotContain notExpected
    }

    @Test
    fun `collection types do not use Java collections by default`() {
        val thrift = """
            |namespace kt test.lists
            |
            |const list<i32> FOO = [1, 2, 3];
            |const map<i8, i8> BAR = { 1: 2 };
            |const set<string> BAZ = ["foo", "bar", "baz"];
            |
            |struct HasCollections {
            |  1: list<string> strs;
            |  2: map<string, string> more_strs;
            |  3: set<i16> shorts;
            |}
            |
            |service HasListMethodArg {
            |  list<i8> sendThatList(1: list<i8> byteList);
            |}
        """.trimMargin()

        for (file in generate(thrift)) {
            val kt = file.toString()
            kt shouldNotContain "java.util"
        }
    }

    @Test
    fun `does not import java Exception or IOException`() {
        val thrift = """
            |namespace kt test.exceptions
            |
            |exception Foo {
            |  1: string message;
            |}
        """.trimMargin()

        for (file in generate(thrift)) {
            val kt = file.toString()

            kt shouldNotContain "import java.Exception"
            kt shouldNotContain "import java.io.IOException"
        }
    }

    @Test
    fun `uses default Throws instead of jvm Throws`() {
        val thrift = """
            |namespace kt test.throws
            |
            |service Frobbler {
            |  void frobble(1: string bizzle);
            |}
        """.trimMargin()

        for (file in generate(thrift)) {
            val kt = file.toString()

            kt shouldContain "@Throws"
            kt shouldNotContain "import kotlin.jvm.Throws"
        }
    }

    @Test
    fun `empty structs do not rely on javaClass for hashCode`() {
        val thrift = """
            |namespace kt test.empty
            |
            |struct Empty {}
        """.trimMargin()

        val file = generate(thrift).single()
        val kt = file.toString()

        kt shouldContain "hashCode(): Int = \"test.empty.Empty\".hashCode()"
        kt shouldNotContain "javaClass"
    }

    @Test
    fun `big enum generation`() {
        val thrift = """
            namespace kt test.enum
            
            enum Foo {
              FIRST_VALUE = 0,
              SECOND_VALUE = 1,
              THIRD_VALUE = 2
            }
        """.trimIndent()

        val expected = """
            |enum class Foo {
            |  FIRST_VALUE,
            |
            |  SECOND_VALUE,
            |
            |  THIRD_VALUE;
            |
            |  val value: Int
            |    get() = value()
            |
            |  fun value(): Int = when (this) {
            |    FIRST_VALUE -> 0
            |    SECOND_VALUE -> 1
            |    THIRD_VALUE -> 2
            |  }
            |
            |  companion object {
            |    fun findByValue(value: Int): Foo? = when (value) {
            |      0 -> FIRST_VALUE
            |      1 -> SECOND_VALUE
            |      2 -> THIRD_VALUE
            |      else -> null
            |    }
            |  }
            |}
        """.trimMargin()

        val notExpected = """
            enum class Foo(value: Int)
        """.trimIndent()

        val file = generate(thrift) {
            emitBigEnums()
        }
        file.single().toString() shouldContain expected
        file.single().toString() shouldNotContain notExpected
    }

    private fun generate(thrift: String, config: (KotlinCodeGenerator.() -> KotlinCodeGenerator)? = null): List<FileSpec> {
        val configOrDefault = config ?: { this }
        return KotlinCodeGenerator()
                .run(configOrDefault)
                .generate(load(thrift))
    }

    private fun load(thrift: String): Schema {
        val file = File(tempDir, "test.thrift").also { it.writeText(thrift) }
        val loader = Loader().apply { addThriftFile(file.toPath()) }
        return loader.load()
    }
}
