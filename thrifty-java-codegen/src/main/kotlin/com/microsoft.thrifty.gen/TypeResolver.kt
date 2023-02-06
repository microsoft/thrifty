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

import com.microsoft.thrifty.TType
import com.microsoft.thrifty.schema.BuiltinType
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
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName

import java.util.LinkedHashMap

/**
 * Utility for getting JavaPoet [TypeName] and [TType] codes from
 * [ThriftType] instances.
 */
internal class TypeResolver {
    var listClass = TypeNames.ARRAY_LIST
    var setClass = TypeNames.LINKED_HASH_SET
    var mapClass = TypeNames.LINKED_HASH_MAP

    /**
     * Returns the [TType] constant representing the type-code for the given
     * [thriftType].
     */
    fun getTypeCode(thriftType: ThriftType): Byte {
        return thriftType.trueType.accept(TypeCodeVisitor)
    }

    fun getJavaClass(thriftType: ThriftType): TypeName {
        return thriftType.accept(TypeNameVisitor)
    }

    fun listOf(elementType: TypeName): ParameterizedTypeName {
        return ParameterizedTypeName.get(listClass, elementType)
    }

    fun setOf(elementType: TypeName): ParameterizedTypeName {
        return ParameterizedTypeName.get(setClass, elementType)
    }

    fun mapOf(keyType: TypeName, valueType: TypeName): ParameterizedTypeName {
        return ParameterizedTypeName.get(mapClass, keyType, valueType)
    }
}

/**
 * A Visitor that converts a [ThriftType] into a [TypeName].
 */
private object TypeNameVisitor : ThriftType.Visitor<TypeName> {
    private val nameCache = LinkedHashMap<String, ClassName>()

    override fun visitVoid(voidType: BuiltinType): TypeName {
        return TypeNames.VOID
    }

    override fun visitBool(boolType: BuiltinType): TypeName {
        return TypeNames.BOOLEAN
    }

    override fun visitByte(byteType: BuiltinType): TypeName {
        return TypeNames.BYTE
    }

    override fun visitI16(i16Type: BuiltinType): TypeName {
        return TypeNames.SHORT
    }

    override fun visitI32(i32Type: BuiltinType): TypeName {
        return TypeNames.INTEGER
    }

    override fun visitI64(i64Type: BuiltinType): TypeName {
        return TypeNames.LONG
    }

    override fun visitDouble(doubleType: BuiltinType): TypeName {
        return TypeNames.DOUBLE
    }

    override fun visitString(stringType: BuiltinType): TypeName {
        return TypeNames.STRING
    }

    override fun visitBinary(binaryType: BuiltinType): TypeName {
        return TypeNames.BYTE_STRING
    }

    override fun visitEnum(enumType: EnumType): TypeName {
        return visitUserType(enumType)
    }

    override fun visitList(listType: ListType): TypeName {
        val elementType = listType.elementType.trueType
        val elementTypeName = elementType.accept(this)
        return ParameterizedTypeName.get(TypeNames.LIST, elementTypeName)
    }

    override fun visitSet(setType: SetType): TypeName {
        val elementType = setType.elementType.trueType
        val elementTypeName = elementType.accept(this)
        return ParameterizedTypeName.get(TypeNames.SET, elementTypeName)
    }

    override fun visitMap(mapType: MapType): TypeName {
        val keyType = mapType.keyType.trueType
        val valueType = mapType.valueType.trueType

        val keyTypeName = keyType.accept(this)
        val valueTypeName = valueType.accept(this)
        return ParameterizedTypeName.get(TypeNames.MAP, keyTypeName, valueTypeName)
    }

    override fun visitStruct(structType: StructType): TypeName {
        return visitUserType(structType)
    }

    override fun visitTypedef(typedefType: TypedefType): TypeName {
        return typedefType.oldType.accept(this)
    }

    override fun visitService(serviceType: ServiceType): TypeName {
        return visitUserType(serviceType)
    }

    private fun visitUserType(userType: UserType): TypeName {
        val packageName = userType.getNamespaceFor(NamespaceScope.JAVA)
        if (packageName.isNullOrEmpty()) {
            throw AssertionError("Missing namespace.  Did you forget to add 'namespace java'?")
        }

        val key = "$packageName##${userType.name}"
        return nameCache.computeIfAbsent(key) { ClassName.get(packageName, userType.name) }
    }
}

/**
 * A Visitor that converts a [ThriftType] into a [TType]
 * constant value.
 */
private object TypeCodeVisitor : ThriftType.Visitor<Byte> {
    override fun visitBool(boolType: BuiltinType): Byte {
        return TType.BOOL
    }

    override fun visitByte(byteType: BuiltinType): Byte {
        return TType.BYTE
    }

    override fun visitI16(i16Type: BuiltinType): Byte {
        return TType.I16
    }

    override fun visitI32(i32Type: BuiltinType): Byte {
        return TType.I32
    }

    override fun visitI64(i64Type: BuiltinType): Byte {
        return TType.I64
    }

    override fun visitDouble(doubleType: BuiltinType): Byte {
        return TType.DOUBLE
    }

    override fun visitString(stringType: BuiltinType): Byte {
        return TType.STRING
    }

    override fun visitBinary(binaryType: BuiltinType): Byte {
        return TType.STRING
    }

    override fun visitVoid(voidType: BuiltinType): Byte {
        return TType.VOID
    }

    override fun visitEnum(enumType: EnumType): Byte {
        return TType.I32
    }

    override fun visitList(listType: ListType): Byte {
        return TType.LIST
    }

    override fun visitSet(setType: SetType): Byte {
        return TType.SET
    }

    override fun visitMap(mapType: MapType): Byte {
        return TType.MAP
    }

    override fun visitStruct(structType: StructType): Byte {
        return TType.STRUCT
    }

    override fun visitTypedef(typedefType: TypedefType): Byte {
        return typedefType.oldType.accept(this)
    }

    override fun visitService(serviceType: ServiceType): Byte {
        throw AssertionError("Services do not have typecodes")
    }
}
