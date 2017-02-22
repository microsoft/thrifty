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
package com.microsoft.thrifty.gen;

import com.microsoft.thrifty.schema.BuiltinType;
import com.microsoft.thrifty.schema.Constant;
import com.microsoft.thrifty.schema.EnumMember;
import com.microsoft.thrifty.schema.EnumType;
import com.microsoft.thrifty.schema.ListType;
import com.microsoft.thrifty.schema.MapType;
import com.microsoft.thrifty.schema.NamespaceScope;
import com.microsoft.thrifty.schema.Schema;
import com.microsoft.thrifty.schema.ServiceType;
import com.microsoft.thrifty.schema.SetType;
import com.microsoft.thrifty.schema.StructType;
import com.microsoft.thrifty.schema.ThriftType;
import com.microsoft.thrifty.schema.TypedefType;
import com.microsoft.thrifty.schema.parser.ConstValueElement;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

final class ConstantBuilder {
    private final TypeResolver typeResolver;
    private final Schema schema;

    ConstantBuilder(TypeResolver typeResolver, Schema schema) {
        this.typeResolver = typeResolver;
        this.schema = schema;
    }

    @SuppressWarnings("unchecked")
    void generateFieldInitializer(
            final CodeBlock.Builder initializer,
            final NameAllocator allocator,
            final AtomicInteger scope,
            final String name,
            final ThriftType tt,
            final ConstValueElement value,
            final boolean needsDeclaration) {

        tt.getTrueType().accept(new SimpleVisitor<Void>() {
            @Override
            public Void visitBuiltin(ThriftType builtinType) {
                CodeBlock init = renderConstValue(initializer, allocator, scope, tt, value);
                initializer.addStatement("$L = $L", name, init);
                return null;
            }

            @Override
            public Void visitEnum(EnumType userType) {
                CodeBlock item = renderConstValue(initializer, allocator, scope, tt, value);

                initializer.addStatement("$L = $L", name, item);
                return null;
            }

            @Override
            public Void visitList(ListType listType) {
                List<ConstValueElement> list = (List<ConstValueElement>) value.value();
                ThriftType elementType = listType.elementType().getTrueType();
                TypeName elementTypeName = typeResolver.getJavaClass(elementType);
                TypeName genericName = ParameterizedTypeName.get(TypeNames.LIST, elementTypeName);
                TypeName listImplName = typeResolver.listOf(elementTypeName);
                generateSingleElementCollection(elementType, genericName, listImplName, list);
                return null;
            }

            @Override
            public Void visitSet(SetType setType) {
                List<ConstValueElement> set = (List<ConstValueElement>) value.value();
                ThriftType elementType = setType.elementType().getTrueType();
                TypeName elementTypeName = typeResolver.getJavaClass(elementType);
                TypeName genericName = ParameterizedTypeName.get(TypeNames.SET, elementTypeName);
                TypeName setImplName = typeResolver.setOf(elementTypeName);
                generateSingleElementCollection(elementType, genericName, setImplName, set);
                return null;
            }

            private void generateSingleElementCollection(
                    ThriftType elementType,
                    TypeName genericName,
                    TypeName collectionImplName,
                    List<ConstValueElement> values) {
                if (needsDeclaration) {
                    initializer.addStatement("$T $N = new $T()",
                            genericName, name, collectionImplName);
                } else {
                    initializer.addStatement("$N = new $T()", name, collectionImplName);
                }

                for (ConstValueElement element : values) {
                    CodeBlock elementName = renderConstValue(initializer, allocator, scope, elementType, element);
                    initializer.addStatement("$N.add($L)", name, elementName);
                }
            }

            @Override
            public Void visitMap(MapType mapType) {
                Map<ConstValueElement, ConstValueElement> map =
                        (Map<ConstValueElement, ConstValueElement>) value.value();
                ThriftType keyType = mapType.keyType().getTrueType();
                ThriftType valueType = mapType.valueType().getTrueType();

                TypeName keyTypeName = typeResolver.getJavaClass(keyType);
                TypeName valueTypeName = typeResolver.getJavaClass(valueType);
                TypeName mapImplName = typeResolver.mapOf(keyTypeName, valueTypeName);

                if (needsDeclaration) {
                    initializer.addStatement("$T $N = new $T()",
                            ParameterizedTypeName.get(TypeNames.MAP, keyTypeName, valueTypeName),
                            name,
                            mapImplName);
                } else {
                    initializer.addStatement("$N = new $T()", name, mapImplName);
                }

                for (Map.Entry<ConstValueElement, ConstValueElement> entry : map.entrySet()) {
                    CodeBlock keyName = renderConstValue(initializer, allocator, scope, keyType, entry.getKey());
                    CodeBlock valueName = renderConstValue(initializer, allocator, scope, valueType, entry.getValue());
                    initializer.addStatement("$N.put($L, $L)", name, keyName, valueName);
                }
                return null;
            }

            @Override
            public Void visitStruct(StructType userType) {
                // TODO: this
                throw new UnsupportedOperationException("struct-type default values are not yet implemented");
            }

            @Override
            public Void visitTypedef(TypedefType typedefType) {
                throw new AssertionError("Should not be possible!");
            }

            @Override
            public Void visitService(ServiceType serviceType) {
                throw new AssertionError("Should not be possible!");
            }

            @Override
            public Void visitVoid(BuiltinType voidType) {
                throw new AssertionError("Should not be possible!");
            }
        });
    }

    CodeBlock renderConstValue(
            final CodeBlock.Builder block,
            final NameAllocator allocator,
            final AtomicInteger scope,
            final ThriftType type,
            final ConstValueElement value) {
        return type.accept(new ConstRenderingVisitor(block, allocator, scope, type, value));
    }

    private class ConstRenderingVisitor implements ThriftType.Visitor<CodeBlock> {
        final CodeBlock.Builder block;
        final NameAllocator allocator;
        final AtomicInteger scope;
        final ThriftType type;
        final ConstValueElement value;

        ConstRenderingVisitor(
                CodeBlock.Builder block,
                NameAllocator allocator,
                AtomicInteger scope,
                ThriftType type,
                ConstValueElement value) {
            this.block = block;
            this.allocator = allocator;
            this.scope = scope;
            this.type = type;
            this.value = value;
        }

        private Object getNumberLiteral(ConstValueElement element) {
            if (!element.isInt()) {
                throw new AssertionError("Expected an int or double, got: " + element.kind());
            }

            if (element.thriftText().startsWith("0x") || element.thriftText().startsWith("0X")) {
                return element.thriftText();
            } else {
                return element.getAsInt();
            }
        }

        @Override
        public CodeBlock visitBool(BuiltinType boolType) {
            String name;
            if (value.isIdentifier()
                    && ("true".equals(value.getAsString()) || "false".equals(value.getAsString()))) {
                name = "true".equals(value.value()) ? "true" : "false";
            } else if (value.isInt()) {
                name = ((Long) value.value()) == 0L ? "false" : "true";
            } else {
                return constantOrError("Invalid boolean constant");
            }

            return CodeBlock.builder().add(name).build();
        }

        @Override
        public CodeBlock visitByte(BuiltinType byteType) {
            if (value.isInt()) {
                return CodeBlock.builder().add("(byte) $L", getNumberLiteral(value)).build();
            } else {
                return constantOrError("Invalid byte constant");
            }
        }

        @Override
        public CodeBlock visitI16(BuiltinType i16Type) {
            if (value.isInt()) {
                return CodeBlock.builder().add("(short) $L", getNumberLiteral(value)).build();
            } else {
                return constantOrError("Invalid i16 constant");
            }
        }

        @Override
        public CodeBlock visitI32(BuiltinType i32Type) {
            if (value.isInt()) {
                return CodeBlock.builder().add("$L", getNumberLiteral(value)).build();
            } else {
                return constantOrError("Invalid i32 constant");
            }
        }

        @Override
        public CodeBlock visitI64(BuiltinType i64Type) {
            if (value.isInt()) {
                return CodeBlock.builder().add("$LL", getNumberLiteral(value)).build();
            } else {
                return constantOrError("Invalid i64 constant");
            }
        }

        @Override
        public CodeBlock visitDouble(BuiltinType doubleType) {
            if (value.isInt() || value.isDouble()) {
                return CodeBlock.builder().add("(double) $L", value.getAsDouble()).build();
            } else {
                return constantOrError("Invalid double constant");
            }
        }

        @Override
        public CodeBlock visitString(BuiltinType stringType) {
            if (value.isString()) {
                return CodeBlock.builder().add("$S", value.getAsString()).build();
            } else {
                return constantOrError("Invalid string constant");
            }
        }

        @Override
        public CodeBlock visitBinary(BuiltinType binaryType) {
            throw new UnsupportedOperationException("Binary literals are not supported");
        }

        @Override
        public CodeBlock visitVoid(BuiltinType voidType) {
            throw new AssertionError("Void literals are meaningless, what are you even doing");
        }

        @Override
        public CodeBlock visitEnum(EnumType enumType) {
            // TODO(ben): Figure out how to handle const references
            EnumMember member;
            try {
                if (value.kind() == ConstValueElement.Kind.INTEGER) {
                    member = enumType.findMemberById(value.getAsInt());
                } else if (value.kind() == ConstValueElement.Kind.IDENTIFIER) {
                    String id = value.getAsString();

                    // Remove the enum name prefix, assuming it is present
                    int ix = id.lastIndexOf('.');
                    if (ix != -1) {
                        id = id.substring(ix + 1);
                    }

                    member = enumType.findMemberByName(id);
                } else {
                    throw new AssertionError(
                            "Constant value kind " + value.kind() + " is not possibly an enum; validation bug");
                }
            } catch (NoSuchElementException e) {
                throw new IllegalStateException(
                        "No enum member in " + enumType.name() + " with value " + value.value());
            }

            return CodeBlock.builder()
                    .add("$T.$L", typeResolver.getJavaClass(enumType), member.name())
                    .build();
        }

        @Override
        public CodeBlock visitList(ListType listType) {
            if (value.isList()) {
                if (value.getAsList().isEmpty()) {
                    TypeName elementType = typeResolver.getJavaClass(listType.elementType());
                    return CodeBlock.builder()
                            .add("$T.<$T>emptyList()", TypeNames.COLLECTIONS, elementType)
                            .build();
                }
                return visitCollection(listType, "list", "unmodifiableList");
            } else {
                return constantOrError("Invalid list constant");
            }
        }

        @Override
        public CodeBlock visitSet(SetType setType) {
            if (value.isList()) { // not a typo; ConstantValueElement.Kind.LIST covers lists and sets.
                if (value.getAsList().isEmpty()) {
                    TypeName elementType = typeResolver.getJavaClass(setType.elementType());
                    return CodeBlock.builder()
                            .add("$T.<$T>emptySet()", TypeNames.COLLECTIONS, elementType)
                            .build();
                }
                return visitCollection(setType, "set", "unmodifiableSet");
            } else {
                return constantOrError("Invalid set constant");
            }
        }

        @Override
        public CodeBlock visitMap(MapType mapType) {
            if (value.isMap()) {
                if (value.getAsMap().isEmpty()) {
                    TypeName keyType = typeResolver.getJavaClass(mapType.keyType());
                    TypeName valueType = typeResolver.getJavaClass(mapType.valueType());
                    return CodeBlock.builder()
                            .add("$T.<$T, $T>emptyMap()", TypeNames.COLLECTIONS, keyType, valueType)
                            .build();
                }
                return visitCollection(mapType, "map", "unmodifiableMap");
            } else {
                return constantOrError("Invalid map constant");
            }
        }

        private CodeBlock visitCollection(
                ThriftType type,
                String tempName,
                String method) {
            String name = allocator.newName(tempName, scope.getAndIncrement());
            generateFieldInitializer(block, allocator, scope, name, type, value, true);
            return CodeBlock.builder().add("$T.$L($N)", TypeNames.COLLECTIONS, method, name).build();
        }

        @Override
        public CodeBlock visitStruct(StructType userType) {
            throw new IllegalStateException("nested structs not implemented");
        }

        @Override
        public CodeBlock visitTypedef(TypedefType typedefType) {
            return typedefType.oldType().accept(this);
        }

        @Override
        public CodeBlock visitService(ServiceType serviceType) {
            throw new IllegalStateException("constants cannot be services");
        }

        private CodeBlock constantOrError(String error) {
            error += ": " + value.value() + " at " + value.location();

            if (!value.isIdentifier()) {
                throw new IllegalStateException(error);
            }

            ThriftType expectedType = type.getTrueType();

            String name = value.getAsString();
            String expectedProgram = null;
            int ix = name.indexOf('.');
            if (ix != -1) {
                expectedProgram = name.substring(0, ix);
                name = name.substring(ix + 1);
            }

            for (Constant constant : schema.constants()) {
                if (!constant.name().equals(name)) {
                    continue;
                }

                ThriftType constantType = constant.type().getTrueType();
                if (!constantType.equals(expectedType)) {
                    continue;
                }

                // TODO(ben): Think of a more systematic way to know what Program owns
                //            a thrift element - pointer-to-parent, probably.
                String programName = constant.location().getProgramName();
                if (expectedProgram != null && !programName.equals(expectedProgram)) {
                    continue;
                }

                String packageName = constant.getNamespaceFor(NamespaceScope.JAVA);
                return CodeBlock.builder().add(packageName + ".Constants." + name).build();
            }

            throw new IllegalStateException(error);
        }
    }
}
