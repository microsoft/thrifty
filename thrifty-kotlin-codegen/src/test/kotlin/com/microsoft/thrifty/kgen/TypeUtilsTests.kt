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

import com.microsoft.thrifty.TType
import com.microsoft.thrifty.schema.BuiltinType
import com.squareup.kotlinpoet.*
import io.kotlintest.shouldBe
import okio.ByteString
import org.junit.Test

class TypeUtilsTests {
    @Test fun `typeCode of builtins`() {
        BuiltinType.BOOL.typeCode shouldBe TType.BOOL
        BuiltinType.BYTE.typeCode shouldBe TType.BYTE
        BuiltinType.I8.typeCode shouldBe TType.BYTE
        BuiltinType.I16.typeCode shouldBe TType.I16
        BuiltinType.I32.typeCode shouldBe TType.I32
        BuiltinType.I64.typeCode shouldBe TType.I64
        BuiltinType.DOUBLE.typeCode shouldBe TType.DOUBLE
        BuiltinType.STRING.typeCode shouldBe TType.STRING
        BuiltinType.BINARY.typeCode shouldBe TType.STRING // binary == string, when serialized.
        BuiltinType.VOID.typeCode shouldBe TType.VOID
    }

    @Test fun `typeName of builtins`() {
        BuiltinType.BOOL.typeName shouldBe BOOLEAN
        BuiltinType.BYTE.typeName shouldBe BYTE
        BuiltinType.I8.typeName shouldBe BYTE
        BuiltinType.I16.typeName shouldBe SHORT
        BuiltinType.I32.typeName shouldBe INT
        BuiltinType.I64.typeName shouldBe LONG
        BuiltinType.DOUBLE.typeName shouldBe DOUBLE
        BuiltinType.STRING.typeName shouldBe String::class.asTypeName()
        BuiltinType.BINARY.typeName shouldBe ByteString::class.asTypeName()
        BuiltinType.VOID.typeName shouldBe UNIT
    }
}