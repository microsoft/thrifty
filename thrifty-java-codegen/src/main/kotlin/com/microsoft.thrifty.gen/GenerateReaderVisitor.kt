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

import com.microsoft.thrifty.Adapter
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
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName

import java.util.ArrayDeque
import java.util.Deque

/**
 * Generates Java code to read a field's value from an open Protocol object.
 *
 * Assumptions:
 * We are inside of [Adapter.read].  Further, we are
 * inside of a single case block for a single field.  There are variables
 * in scope named "protocol" and "builder", representing the connection and
 * the struct builder.
 */
internal open class GenerateReaderVisitor(
        private val resolver: TypeResolver,
        private val read: MethodSpec.Builder,
        private val fieldName: String,
        private val fieldType: ThriftType
) : ThriftType.Visitor<Unit> {

    private val nameStack: Deque<String> = ArrayDeque<String>()

    private var scope: Int = 0

    fun generate() {
        val fieldTypeCode = resolver.getTypeCode(fieldType)
        val codeName = TypeNames.getTypeCodeName(fieldTypeCode)
        read.beginControlFlow("if (field.typeId == \$T.\$L)", TypeNames.TTYPE, codeName)

        nameStack.push("value")
        fieldType.accept(this)
        nameStack.pop()

        useReadValue("value")

        read.nextControlFlow("else")
        read.addStatement("\$T.skip(protocol, field.typeId)", TypeNames.PROTO_UTIL)
        read.endControlFlow()

    }

    protected open fun useReadValue(localName: String) {
        read.addStatement("builder.\$N(\$N)", fieldName, localName)
    }

    override fun visitBool(boolType: BuiltinType) {
        read.addStatement("\$T \$N = protocol.readBool()", TypeNames.BOOLEAN.unbox(), nameStack.peek())
    }

    override fun visitByte(bytetype: BuiltinType) {
        read.addStatement("\$T \$N = protocol.readByte()", TypeNames.BYTE.unbox(), nameStack.peek())
    }

    override fun visitI16(i16Type: BuiltinType) {
        read.addStatement("\$T \$N = protocol.readI16()", TypeNames.SHORT.unbox(), nameStack.peek())
    }

    override fun visitI32(i32Type: BuiltinType) {
        read.addStatement("\$T \$N = protocol.readI32()", TypeNames.INTEGER.unbox(), nameStack.peek())
    }

    override fun visitI64(i64Type: BuiltinType) {
        read.addStatement("\$T \$N = protocol.readI64()", TypeNames.LONG.unbox(), nameStack.peek())
    }

    override fun visitDouble(doubleType: BuiltinType) {
        read.addStatement("\$T \$N = protocol.readDouble()", TypeNames.DOUBLE.unbox(), nameStack.peek())
    }

    override fun visitString(stringType: BuiltinType) {
        read.addStatement("\$T \$N = protocol.readString()", TypeNames.STRING, nameStack.peek())
    }

    override fun visitBinary(binaryType: BuiltinType) {
        read.addStatement("\$T \$N = protocol.readBinary()", TypeNames.BYTE_STRING, nameStack.peek())
    }

    override fun visitVoid(voidType: BuiltinType) {
        throw AssertionError("Cannot read void")
    }

    override fun visitEnum(enumType: EnumType) {
        val target = nameStack.peek()
        val qualifiedJavaName = getFullyQualifiedJavaName(enumType)
        val intName = "i32_$scope"

        read.addStatement("int \$L = protocol.readI32()", intName)
        read.addStatement("$1L $2N = $1L.findByValue($3L)", qualifiedJavaName, target, intName)
        read.beginControlFlow("if (\$N == null)", target!!)
        read.addStatement(
                "throw new $1T($2T.PROTOCOL_ERROR, $3S + $4L)",
                TypeNames.THRIFT_EXCEPTION,
                TypeNames.THRIFT_EXCEPTION_KIND,
                "Unexpected value for enum-type " + enumType.name() + ": ",
                intName)
        read.endControlFlow()
    }

    override fun visitList(listType: ListType) {
        val elementType = resolver.getJavaClass(listType.elementType().trueType)
        val genericListType = ParameterizedTypeName.get(TypeNames.LIST, elementType)
        val listImplType = resolver.listOf(elementType)

        val listInfo = "listMetadata$scope"
        val idx = "i$scope"
        val item = "item$scope"

        read.addStatement("\$T \$N = protocol.readListBegin()", TypeNames.LIST_META, listInfo)
        read.addStatement("\$T \$N = new \$T(\$N.size)", genericListType, nameStack.peek(), listImplType, listInfo)
        read.beginControlFlow("for (int $1N = 0; $1N < $2N.size; ++$1N)", idx, listInfo)

        pushScope {
            nameStack.push(item)

            listType.elementType().trueType.accept(this)

            nameStack.pop()
        }

        read.addStatement("\$N.add(\$N)", nameStack.peek(), item)
        read.endControlFlow()
        read.addStatement("protocol.readListEnd()")
    }

    override fun visitSet(setType: SetType) {
        val elementType = resolver.getJavaClass(setType.elementType().trueType)
        val genericSetType = ParameterizedTypeName.get(TypeNames.SET, elementType)
        val setImplType = resolver.setOf(elementType)

        val setInfo = "setMetadata$scope"
        val idx = "i$scope"
        val item = "item$scope"

        read.addStatement("\$T \$N = protocol.readSetBegin()", TypeNames.SET_META, setInfo)
        read.addStatement("\$T \$N = new \$T(\$N.size)", genericSetType, nameStack.peek(), setImplType, setInfo)
        read.beginControlFlow("for (int $1N = 0; $1N < $2N.size; ++$1N)", idx, setInfo)

        pushScope {
            nameStack.push(item)

            setType.elementType().accept(this)

            nameStack.pop()
        }

        read.addStatement("\$N.add(\$N)", nameStack.peek(), item)
        read.endControlFlow()
        read.addStatement("protocol.readSetEnd()")
    }

    override fun visitMap(mapType: MapType) {
        val keyType = resolver.getJavaClass(mapType.keyType().trueType)
        val valueType = resolver.getJavaClass(mapType.valueType().trueType)
        val genericMapType = ParameterizedTypeName.get(TypeNames.MAP, keyType, valueType)
        val mapImplType = resolver.mapOf(keyType, valueType)

        val mapInfo = "mapMetadata$scope"
        val idx = "i$scope"
        val key = "key$scope"
        val value = "value$scope"

        pushScope {
            read.addStatement("\$T \$N = protocol.readMapBegin()", TypeNames.MAP_META, mapInfo)
            read.addStatement("\$T \$N = new \$T(\$N.size)", genericMapType, nameStack.peek(), mapImplType, mapInfo)
            read.beginControlFlow("for (int $1N = 0; $1N < $2N.size; ++$1N)", idx, mapInfo)

            nameStack.push(key)
            mapType.keyType().accept(this)
            nameStack.pop()

            nameStack.push(value)
            mapType.valueType().accept(this)
            nameStack.pop()

            read.addStatement("\$N.put(\$N, \$N)", nameStack.peek(), key, value)

            read.endControlFlow()
            read.addStatement("protocol.readMapEnd()")
        }
    }

    override fun visitStruct(userType: StructType) {
        val qualifiedJavaName = getFullyQualifiedJavaName(userType)
        read.addStatement("$1L $2N = $1L.ADAPTER.read(protocol)", qualifiedJavaName, nameStack.peek())
    }

    override fun visitTypedef(typedefType: TypedefType) {
        // throw AssertionError?
        typedefType.trueType.accept(this)
    }

    override fun visitService(serviceType: ServiceType) {
        throw AssertionError("Cannot read a service")
    }

    private fun getFullyQualifiedJavaName(type: UserType): String {
        if (type.isBuiltin || type.isList || type.isMap || type.isSet || type.isTypedef) {
            throw AssertionError("Only user and enum types are supported")
        }

        val packageName = type.getNamespaceFor(NamespaceScope.JAVA)
        return packageName + "." + type.name()
    }

    private inline fun pushScope(fn: () -> Unit) {
        ++scope
        try {
            fn()
        } finally {
            --scope
        }
    }
}
