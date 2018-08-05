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
import com.microsoft.thrifty.protocol.Protocol
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
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.NameAllocator
import com.squareup.javapoet.ParameterizedTypeName
import java.util.Deque

import java.util.LinkedList

/**
 * Generates Java code to write the value of a field in a [Adapter.write]
 * implementation.
 *
 * Handles nested values like lists, sets, maps, and user types.
 *
 * @param resolver the [TypeResolver] singleton
 * @param write the [Adapter.write] method under construction
 * @param proto the name of the [Protocol] parameter to the write method
 * @param subject the name of the struct parameter to the write method
 * @param fieldName the Java name of the field being written
 */
internal class GenerateWriterVisitor(
        private val resolver: TypeResolver,
        private val write: MethodSpec.Builder,
        private val proto: String,
        subject: String,
        fieldName: String
) : ThriftType.Visitor<Unit> {

    /**
     * A stack of names, with the topmost name being the one currently
     * being written/assigned.
     */
    private val nameStack: Deque<String> = LinkedList<String>().apply {
        push("$subject.$fieldName")
    }

    /**
     * A count of nested scopes.  Used to prevent name clashes for iterator
     * and temporary names used when writing nested collections.
     */
    private var scopeLevel: Int = 0

    private val nameAllocator = NameAllocator().apply {
        newName(proto, proto)
    }

    override fun visitBool(boolType: BuiltinType) {
        write.addStatement("\$N.writeBool(\$L)", proto, nameStack.peek())
    }

    override fun visitByte(byteType: BuiltinType) {
        write.addStatement("\$N.writeByte(\$L)", proto, nameStack.peek())
    }

    override fun visitI16(i16Type: BuiltinType) {
        write.addStatement("\$N.writeI16(\$L)", proto, nameStack.peek())
    }

    override fun visitI32(i32Type: BuiltinType) {
        write.addStatement("\$N.writeI32(\$L)", proto, nameStack.peek())
    }

    override fun visitI64(i64Type: BuiltinType) {
        write.addStatement("\$N.writeI64(\$L)", proto, nameStack.peek())
    }

    override fun visitDouble(doubleType: BuiltinType) {
        write.addStatement("\$N.writeDouble(\$L)", proto, nameStack.peek())
    }

    override fun visitString(stringType: BuiltinType) {
        write.addStatement("\$N.writeString(\$L)", proto, nameStack.peek())
    }

    override fun visitBinary(binaryType: BuiltinType) {
        write.addStatement("\$N.writeBinary(\$L)", proto, nameStack.peek())
    }

    override fun visitVoid(voidType: BuiltinType) {
        throw AssertionError("Fields cannot be void")
    }

    override fun visitEnum(enumType: EnumType) {
        write.addStatement("\$N.writeI32(\$L.value)", proto, nameStack.peek())
    }

    override fun visitList(listType: ListType) {
        visitSingleElementCollection(
                listType.elementType.trueType,
                "writeListBegin",
                "writeListEnd")
    }

    override fun visitSet(setType: SetType) {
        visitSingleElementCollection(
                setType.elementType.trueType,
                "writeSetBegin",
                "writeSetEnd")
    }

    private fun visitSingleElementCollection(elementType: ThriftType, beginMethod: String, endMethod: String) {
        val tag = "item$scopeLevel"
        val item = nameAllocator.newName(tag, tag)

        val javaClass = resolver.getJavaClass(elementType)
        val typeCode = resolver.getTypeCode(elementType)
        val typeCodeName = TypeNames.getTypeCodeName(typeCode)

        write.addStatement(
                "\$N.\$L(\$T.\$L, \$L.size())",
                proto,
                beginMethod,
                TypeNames.TTYPE,
                typeCodeName,
                nameStack.peek())

        write.beginControlFlow("for (\$T \$N : \$L)", javaClass, item, nameStack.peek())

        scope {
            nameStack.push(item)
            elementType.accept(this)
            nameStack.pop()
        }

        write.endControlFlow()

        write.addStatement("\$N.\$L()", proto, endMethod)
    }

    override fun visitMap(mapType: MapType) {
        val entryTag = "entry$scopeLevel"
        val keyTag = "key$scopeLevel"
        val valueTag = "value$scopeLevel"

        val entryName = nameAllocator.newName(entryTag, entryTag)
        val keyName = nameAllocator.newName(keyTag, keyTag)
        val valueName = nameAllocator.newName(valueTag, valueTag)
        val kt = mapType.keyType.trueType
        val vt = mapType.valueType.trueType

        val keyTypeCode = resolver.getTypeCode(kt)
        val valTypeCode = resolver.getTypeCode(vt)

        write.addStatement(
                "$1N.writeMapBegin($2T.$3L, $2T.$4L, $5L.size())",
                proto,
                TypeNames.TTYPE,
                TypeNames.getTypeCodeName(keyTypeCode),
                TypeNames.getTypeCodeName(valTypeCode),
                nameStack.peek())

        val keyTypeName = resolver.getJavaClass(kt)
        val valueTypeName = resolver.getJavaClass(vt)
        val entry = ParameterizedTypeName.get(TypeNames.MAP_ENTRY, keyTypeName, valueTypeName)
        write.beginControlFlow("for (\$T \$N : \$L.entrySet())", entry, entryTag, nameStack.peek())
        write.addStatement("\$T \$N = \$N.getKey()", keyTypeName, keyName, entryName)
        write.addStatement("\$T \$N = \$N.getValue()", valueTypeName, valueName, entryName)

        scope {
            nameStack.push(keyName)
            kt.accept(this)
            nameStack.pop()

            nameStack.push(valueName)
            vt.accept(this)
            nameStack.pop()
        }

        write.endControlFlow()
        write.addStatement("\$N.writeMapEnd()", proto)
    }

    override fun visitStruct(structType: StructType) {
        val javaName = structType.getNamespaceFor(NamespaceScope.JAVA) + "." + structType.name
        write.addStatement("\$L.ADAPTER.write(\$N, \$L)", javaName, proto, nameStack.peek())
    }

    override fun visitTypedef(typedefType: TypedefType) {
        typedefType.trueType.accept(this)
    }

    override fun visitService(serviceType: ServiceType) {
        throw AssertionError("Cannot write a service")
    }

    private inline fun scope(fn: () -> Unit) {
        scopeLevel++
        try {
            fn()
        } finally {
            scopeLevel--
        }
    }
}
