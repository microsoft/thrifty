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

import com.microsoft.thrifty.schema.BuiltinType
import com.microsoft.thrifty.schema.EnumType
import com.microsoft.thrifty.schema.ListType
import com.microsoft.thrifty.schema.MapType
import com.microsoft.thrifty.schema.NamespaceScope
import com.microsoft.thrifty.schema.Schema
import com.microsoft.thrifty.schema.ServiceType
import com.microsoft.thrifty.schema.SetType
import com.microsoft.thrifty.schema.StructType
import com.microsoft.thrifty.schema.ThriftType
import com.microsoft.thrifty.schema.TypedefType
import com.microsoft.thrifty.schema.parser.ConstValueElement
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.NameAllocator
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import java.util.NoSuchElementException
import java.util.concurrent.atomic.AtomicInteger

internal class ConstantBuilder(
        private val typeResolver: TypeResolver,
        private val schema: Schema
) {

    fun generateFieldInitializer(
            initializer: CodeBlock.Builder,
            allocator: NameAllocator,
            scope: AtomicInteger,
            name: String,
            tt: ThriftType,
            value: ConstValueElement,
            needsDeclaration: Boolean) {

        tt.trueType.accept(object : SimpleVisitor<Unit>() {
            override fun visitBuiltin(builtinType: ThriftType) {
                val init = renderConstValue(initializer, allocator, scope, tt, value)
                initializer.addStatement("\$L = \$L", name, init)
            }

            override fun visitEnum(enumType: EnumType) {
                val item = renderConstValue(initializer, allocator, scope, tt, value)

                initializer.addStatement("\$L = \$L", name, item)
            }

            override fun visitList(listType: ListType) {
                val list = value.getAsList()
                val elementType = listType.elementType.trueType
                val elementTypeName = typeResolver.getJavaClass(elementType)
                val genericName = ParameterizedTypeName.get(TypeNames.LIST, elementTypeName)
                val listImplName = typeResolver.listOf(elementTypeName)
                generateSingleElementCollection(elementType, genericName, listImplName, list)
            }

            override fun visitSet(setType: SetType) {
                val set = value.getAsList()
                val elementType = setType.elementType.trueType
                val elementTypeName = typeResolver.getJavaClass(elementType)
                val genericName = ParameterizedTypeName.get(TypeNames.SET, elementTypeName)
                val setImplName = typeResolver.setOf(elementTypeName)
                generateSingleElementCollection(elementType, genericName, setImplName, set)
            }

            private fun generateSingleElementCollection(
                    elementType: ThriftType,
                    genericName: TypeName,
                    collectionImplName: TypeName,
                    values: List<ConstValueElement>) {
                if (needsDeclaration) {
                    initializer.addStatement("\$T \$N = new \$T()",
                            genericName, name, collectionImplName)
                } else {
                    initializer.addStatement("\$N = new \$T()", name, collectionImplName)
                }

                for (element in values) {
                    val elementName = renderConstValue(initializer, allocator, scope, elementType, element)
                    initializer.addStatement("\$N.add(\$L)", name, elementName)
                }
            }

            override fun visitMap(mapType: MapType) {
                val map = value.getAsMap()
                val keyType = mapType.keyType.trueType
                val valueType = mapType.valueType.trueType

                val keyTypeName = typeResolver.getJavaClass(keyType)
                val valueTypeName = typeResolver.getJavaClass(valueType)
                val mapImplName = typeResolver.mapOf(keyTypeName, valueTypeName)

                if (needsDeclaration) {
                    initializer.addStatement("\$T \$N = new \$T()",
                            ParameterizedTypeName.get(TypeNames.MAP, keyTypeName, valueTypeName),
                            name,
                            mapImplName)
                } else {
                    initializer.addStatement("\$N = new \$T()", name, mapImplName)
                }

                for ((key, value1) in map) {
                    val keyName = renderConstValue(initializer, allocator, scope, keyType, key)
                    val valueName = renderConstValue(initializer, allocator, scope, valueType, value1)
                    initializer.addStatement("\$N.put(\$L, \$L)", name, keyName, valueName)
                }
            }

            override fun visitStruct(structType: StructType) {
                // TODO: this
                throw UnsupportedOperationException("struct-type default values are not yet implemented")
            }

            override fun visitTypedef(typedefType: TypedefType) {
                throw AssertionError("Should not be possible!")
            }

            override fun visitService(serviceType: ServiceType) {
                throw AssertionError("Should not be possible!")
            }

            override fun visitVoid(voidType: BuiltinType) {
                throw AssertionError("Should not be possible!")
            }
        })
    }

    fun renderConstValue(
            block: CodeBlock.Builder,
            allocator: NameAllocator,
            scope: AtomicInteger,
            type: ThriftType,
            value: ConstValueElement): CodeBlock {
        return type.accept(ConstRenderingVisitor(block, allocator, scope, type, value))
    }

    private inner class ConstRenderingVisitor(
            internal val block: CodeBlock.Builder,
            internal val allocator: NameAllocator,
            internal val scope: AtomicInteger,
            internal val type: ThriftType,
            internal val value: ConstValueElement
    ) : ThriftType.Visitor<CodeBlock> {

        private fun getNumberLiteral(element: ConstValueElement): Any {
            if (!element.isInt) {
                throw AssertionError("Expected an int or double, got: " + element.kind)
            }

            return if (element.thriftText.startsWith("0x") || element.thriftText.startsWith("0X")) {
                element.thriftText
            } else {
                element.getAsInt()
            }
        }

        override fun visitBool(boolType: BuiltinType): CodeBlock {
            val name: String
            if (value.isIdentifier && ("true" == value.getAsString() || "false" == value.getAsString())) {
                name = if ("true" == value.getAsString()) "true" else "false"
            } else if (value.isInt) {
                name = if (value.getAsLong() == 0L) "false" else "true"
            } else {
                return constantOrError("Invalid boolean constant")
            }

            return CodeBlock.builder().add(name).build()
        }

        override fun visitByte(byteType: BuiltinType): CodeBlock {
            return if (value.isInt) {
                CodeBlock.builder().add("(byte) \$L", getNumberLiteral(value)).build()
            } else {
                constantOrError("Invalid byte constant")
            }
        }

        override fun visitI16(i16Type: BuiltinType): CodeBlock {
            return if (value.isInt) {
                CodeBlock.builder().add("(short) \$L", getNumberLiteral(value)).build()
            } else {
                constantOrError("Invalid i16 constant")
            }
        }

        override fun visitI32(i32Type: BuiltinType): CodeBlock {
            return if (value.isInt) {
                CodeBlock.builder().add("\$L", getNumberLiteral(value)).build()
            } else {
                constantOrError("Invalid i32 constant")
            }
        }

        override fun visitI64(i64Type: BuiltinType): CodeBlock {
            return if (value.isInt) {
                CodeBlock.builder().add("\$LL", getNumberLiteral(value)).build()
            } else {
                constantOrError("Invalid i64 constant")
            }
        }

        override fun visitDouble(doubleType: BuiltinType): CodeBlock {
            return if (value.isInt || value.isDouble) {
                CodeBlock.builder().add("(double) \$L", value.getAsDouble()).build()
            } else {
                constantOrError("Invalid double constant")
            }
        }

        override fun visitString(stringType: BuiltinType): CodeBlock {
            return if (value.isString) {
                CodeBlock.builder().add("\$S", value.getAsString()).build()
            } else {
                constantOrError("Invalid string constant")
            }
        }

        override fun visitBinary(binaryType: BuiltinType): CodeBlock {
            throw UnsupportedOperationException("Binary literals are not supported")
        }

        override fun visitVoid(voidType: BuiltinType): CodeBlock {
            throw AssertionError("Void literals are meaningless, what are you even doing")
        }

        override fun visitEnum(enumType: EnumType): CodeBlock {
            // TODO(ben): Figure out how to handle const references
            try {
                val member = when (value.kind) {
                    ConstValueElement.Kind.INTEGER ->
                        enumType.findMemberById(value.getAsInt())

                    ConstValueElement.Kind.IDENTIFIER -> {
                        // Remove the enum name prefix, assuming it is present
                        val id = value.getAsString().split(".").last()

                        enumType.findMemberByName(id)
                    }

                    else -> throw AssertionError(
                            "Constant value kind ${value.kind} is not possibly an enum; validation bug")
                }

                return CodeBlock.builder()
                        .add("\$T.\$L", typeResolver.getJavaClass(enumType), member.name)
                        .build()

            } catch (e: NoSuchElementException) {
                throw IllegalStateException(
                        "No enum member in ${enumType.name} with value ${value.value}")
            }
        }

        override fun visitList(listType: ListType): CodeBlock {
            if (value.isList) {
                if (value.getAsList().isEmpty()) {
                    val elementType = typeResolver.getJavaClass(listType.elementType)
                    return CodeBlock.builder()
                            .add("\$T.<\$T>emptyList()", TypeNames.COLLECTIONS, elementType)
                            .build()
                }
                return visitCollection(listType, "list", "unmodifiableList")
            } else {
                return constantOrError("Invalid list constant")
            }
        }

        override fun visitSet(setType: SetType): CodeBlock {
            if (value.isList) { // not a typo; ConstantValueElement.Kind.LIST covers lists and sets.
                if (value.getAsList().isEmpty()) {
                    val elementType = typeResolver.getJavaClass(setType.elementType)
                    return CodeBlock.builder()
                            .add("\$T.<\$T>emptySet()", TypeNames.COLLECTIONS, elementType)
                            .build()
                }
                return visitCollection(setType, "set", "unmodifiableSet")
            } else {
                return constantOrError("Invalid set constant")
            }
        }

        override fun visitMap(mapType: MapType): CodeBlock {
            if (value.isMap) {
                if (value.getAsMap().isEmpty()) {
                    val keyType = typeResolver.getJavaClass(mapType.keyType)
                    val valueType = typeResolver.getJavaClass(mapType.valueType)
                    return CodeBlock.builder()
                            .add("\$T.<\$T, \$T>emptyMap()", TypeNames.COLLECTIONS, keyType, valueType)
                            .build()
                }
                return visitCollection(mapType, "map", "unmodifiableMap")
            } else {
                return constantOrError("Invalid map constant")
            }
        }

        private fun visitCollection(
                type: ThriftType,
                tempName: String,
                method: String): CodeBlock {
            val name = allocator.newName(tempName, scope.getAndIncrement())
            generateFieldInitializer(block, allocator, scope, name, type, value, true)
            return CodeBlock.builder().add("\$T.\$L(\$N)", TypeNames.COLLECTIONS, method, name).build()
        }

        override fun visitStruct(structType: StructType): CodeBlock {
            throw IllegalStateException("nested structs not implemented")
        }

        override fun visitTypedef(typedefType: TypedefType): CodeBlock {
            return typedefType.oldType.accept(this)
        }

        override fun visitService(serviceType: ServiceType): CodeBlock {
            throw IllegalStateException("constants cannot be services")
        }

        private fun constantOrError(error: String): CodeBlock {
            val message = "$error: ${value.value} + at ${value.location}"

            if (!value.isIdentifier) {
                throw IllegalStateException(message)
            }

            val expectedType = type.trueType

            var name = value.getAsString()
            val ix = name.indexOf('.')
            var expectedProgram: String? = null
            if (ix != -1) {
                expectedProgram = name.substring(0, ix)
                name = name.substring(ix + 1)
            }

            // TODO(ben): Think of a more systematic way to know what [Program] owns a thrift element
            val c = schema.constants
                    .asSequence()
                    .filter { it.name == name }
                    .filter { it.type.trueType == expectedType }
                    .filter { expectedProgram == null || it.location.programName == expectedProgram }
                    .firstOrNull() ?: throw IllegalStateException(message)

            val packageName = c.getNamespaceFor(NamespaceScope.JAVA)
            return CodeBlock.builder().add("$packageName.Constants.$name").build()
        }
    }
}
