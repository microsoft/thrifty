/*
 * Thrifty
 *
 * Copyright (c) Benjamin Bader
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
package com.bendb.thrifty.schema

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

class ThriftTypeTest {
    @Test
    fun byteAndI8AreSynonyms() {
        val ctr = AtomicInteger(0)

        val v = object : ThriftType.Visitor<Unit> {
            override fun visitVoid(voidType: BuiltinType) {
            }

            override fun visitBool(boolType: BuiltinType) {
            }

            override fun visitByte(byteType: BuiltinType) {
                ctr.incrementAndGet()
            }

            override fun visitI16(i16Type: BuiltinType) {
            }

            override fun visitI32(i32Type: BuiltinType) {
            }

            override fun visitI64(i64Type: BuiltinType) {
            }

            override fun visitDouble(doubleType: BuiltinType) {
            }

            override fun visitString(stringType: BuiltinType) {
            }

            override fun visitBinary(binaryType: BuiltinType) {
            }

            override fun visitEnum(enumType: EnumType) {
            }

            override fun visitList(listType: ListType) {
            }

            override fun visitSet(setType: SetType) {
            }

            override fun visitMap(mapType: MapType) {
            }

            override fun visitStruct(structType: StructType) {
            }

            override fun visitTypedef(typedefType: TypedefType) {
            }

            override fun visitService(serviceType: ServiceType) {
            }
        }

        BuiltinType.I8.accept(v)
        BuiltinType.BYTE.accept(v)

        BuiltinType.I8.isBuiltin shouldBe true
        ctr.get() shouldBe 2
    }

    @Test
    fun typesWithSameNameAreEqual() {
        val one = BuiltinType.get("i32")
        val two = BuiltinType.get("i32")

        one shouldBe two
    }

    @Test
    fun annotationsDoNotAffectEquality() {
        val one = BuiltinType.get("i32")!!.withAnnotations(Collections.singletonMap("test", "one"))
        val two = BuiltinType.get("i32")!!.withAnnotations(Collections.singletonMap("test", "two"))

        one shouldBe two
    }

    @Test
    fun withAnnotationsMergesAnnotations() {
        val one = BuiltinType.get("i32")!!.withAnnotations(Collections.singletonMap("i32", "bar"))
        val two = one.withAnnotations(Collections.singletonMap("baz", "quux"))

        two.annotations["i32"] shouldBe "bar"
        two.annotations["baz"] shouldBe "quux"
    }
}
