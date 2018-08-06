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
import com.microsoft.thrifty.schema.Constant
import com.microsoft.thrifty.schema.EnumType
import com.microsoft.thrifty.schema.ListType
import com.microsoft.thrifty.schema.MapType
import com.microsoft.thrifty.schema.NamespaceScope
import com.microsoft.thrifty.schema.ServiceType
import com.microsoft.thrifty.schema.SetType
import com.microsoft.thrifty.schema.StructType
import com.microsoft.thrifty.schema.ThriftType
import com.microsoft.thrifty.schema.TypedefType
import com.microsoft.thrifty.schema.UserType
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import okio.ByteString

internal val ThriftType.typeName: TypeName
    get() = accept(TypeNameVisitor)

internal val ThriftType.typeCode: Byte
    get() = accept(TypeCodeVisitor)

internal val UserType.kotlinNamespace: String
    get() = getNamespaceFor(NamespaceScope.KOTLIN, NamespaceScope.JAVA, NamespaceScope.ALL)
            ?: throw AssertionError("No JVM namespace defined for $name")

internal val Constant.kotlinNamespace: String
    get() = getNamespaceFor(NamespaceScope.KOTLIN, NamespaceScope.JAVA, NamespaceScope.ALL)
            ?: throw AssertionError("No JVM namespace defined for $name")

internal val ThriftType.typeCodeName: String
    get() = when (typeCode) {
        TType.BOOL -> "BOOL"
        TType.BYTE -> "BYTE"
        TType.I16 -> "I16"
        TType.I32 -> "I32"
        TType.I64 -> "I64"
        TType.DOUBLE -> "DOUBLE"
        TType.STRING -> "STRING"
        TType.LIST -> "LIST"
        TType.SET -> "SET"
        TType.MAP -> "MAP"
        TType.STRUCT -> "STRUCT"
        TType.VOID -> "VOID"
        else -> error("Unexpected TType value: $typeCode")
    }

private object TypeCodeVisitor : ThriftType.Visitor<Byte> {
    override fun visitVoid(voidType: BuiltinType) = TType.VOID
    override fun visitBool(boolType: BuiltinType) = TType.BOOL
    override fun visitByte(byteType: BuiltinType) = TType.BYTE
    override fun visitI16(i16Type: BuiltinType) = TType.I16
    override fun visitI32(i32Type: BuiltinType) = TType.I32
    override fun visitI64(i64Type: BuiltinType) = TType.I64
    override fun visitDouble(doubleType: BuiltinType) = TType.DOUBLE
    override fun visitString(stringType: BuiltinType) = TType.STRING
    override fun visitBinary(binaryType: BuiltinType) = TType.STRING
    override fun visitEnum(enumType: EnumType) = TType.I32
    override fun visitList(listType: ListType) = TType.LIST
    override fun visitSet(setType: SetType) = TType.SET
    override fun visitMap(mapType: MapType) = TType.MAP
    override fun visitStruct(structType: StructType) = TType.STRUCT
    override fun visitTypedef(typedefType: TypedefType) = typedefType.trueType.accept(this)
    override fun visitService(serviceType: ServiceType) = error("Services don't have a typecode")
}

private object TypeNameVisitor : ThriftType.Visitor<TypeName> {
    override fun visitVoid(voidType: BuiltinType) = UNIT

    override fun visitBool(boolType: BuiltinType) = BOOLEAN

    override fun visitByte(byteType: BuiltinType) = BYTE

    override fun visitI16(i16Type: BuiltinType) = SHORT

    override fun visitI32(i32Type: BuiltinType) = INT

    override fun visitI64(i64Type: BuiltinType) = LONG

    override fun visitDouble(doubleType: BuiltinType) = DOUBLE

    override fun visitString(stringType: BuiltinType) = String::class.asTypeName()

    override fun visitBinary(binaryType: BuiltinType) = ByteString::class.asTypeName()

    override fun visitEnum(enumType: EnumType) = userTypeName(enumType)

    override fun visitList(listType: ListType): TypeName {
        val elementType = listType.elementType.accept(this)
        return List::class.asTypeName().parameterizedBy(elementType)
    }

    override fun visitSet(setType: SetType): TypeName {
        val elementType = setType.elementType.accept(this)
        return Set::class.asTypeName().parameterizedBy(elementType)
    }

    override fun visitMap(mapType: MapType): TypeName {
        val keyType = mapType.keyType.accept(this)
        val valueType = mapType.valueType.accept(this)
        return Map::class.asTypeName().parameterizedBy(keyType, valueType)
    }

    override fun visitStruct(structType: StructType): TypeName = userTypeName(structType)

    override fun visitTypedef(typedefType: TypedefType) = userTypeName(typedefType)

    override fun visitService(serviceType: ServiceType) = userTypeName(serviceType)

    private fun userTypeName(userType: UserType): TypeName {
        return ClassName(userType.kotlinNamespace, userType.name)
    }

}