/*
 * Copyright (C) 2015-2016 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.thrifty.gen;

import com.bendb.thrifty.schema.EnumType;
import com.bendb.thrifty.schema.Schema;
import com.bendb.thrifty.schema.ThriftType;
import com.bendb.thrifty.schema.parser.ConstValueElement;
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

    public ConstantBuilder(TypeResolver typeResolver, Schema schema) {
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
            public Void visitEnum(ThriftType userType) {
                CodeBlock item = renderConstValue(initializer, allocator, scope, tt, value);

                initializer.addStatement("$L = $L", name, item);
                return null;
            }

            @Override
            public Void visitList(ThriftType.ListType listType) {
                List<ConstValueElement> list = (List<ConstValueElement>) value.value();
                ThriftType elementType = listType.elementType().getTrueType();
                TypeName elementTypeName = typeResolver.getJavaClass(elementType);
                TypeName genericName = ParameterizedTypeName.get(TypeNames.LIST, elementTypeName);
                TypeName listImplName = typeResolver.listOf(elementTypeName);
                generateSingleElementCollection(elementType, genericName, listImplName, list);
                return null;
            }

            @Override
            public Void visitSet(ThriftType.SetType setType) {
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
            public Void visitMap(ThriftType.MapType mapType) {
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
            public Void visitUserType(ThriftType userType) {
                // TODO: this
                throw new UnsupportedOperationException("struct-type default values are not yet implemented");
            }

            @Override
            public Void visitTypedef(ThriftType.TypedefType typedefType) {
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
        // TODO: Emit references to constants if kind == IDENTIFIER and it identifies an appropriately-typed const
        return type.accept(new ThriftType.Visitor<CodeBlock>() {
            @Override
            public CodeBlock visitBool() {
                String name;
                if (value.kind() == ConstValueElement.Kind.IDENTIFIER) {
                    name = "true".equals(value.value()) ? "true" : "false";
                } else if (value.kind() == ConstValueElement.Kind.INTEGER) {
                    name = ((Long) value.value()) == 0L ? "false" : "true";
                } else {
                    throw new AssertionError("Invalid boolean constant: " + value.value() + " at " + value.location());
                }

                return CodeBlock.builder().add(name).build();
            }

            @Override
            public CodeBlock visitByte() {
                return CodeBlock.builder().add("(byte) $L", value.getAsInt()).build();
            }

            @Override
            public CodeBlock visitI16() {
                return CodeBlock.builder().add("(short) $L", value.getAsInt()).build();
            }

            @Override
            public CodeBlock visitI32() {
                return CodeBlock.builder().add("$L", value.getAsInt()).build();
            }

            @Override
            public CodeBlock visitI64() {
                return CodeBlock.builder().add("$L", value.getAsLong()).build();
            }

            @Override
            public CodeBlock visitDouble() {
                return CodeBlock.builder().add("(double) $L", value.getAsDouble()).build();
            }

            @Override
            public CodeBlock visitString() {
                return CodeBlock.builder().add("$S", value.getAsString()).build();
            }

            @Override
            public CodeBlock visitBinary() {
                throw new UnsupportedOperationException("Binary literals are not supported");
            }

            @Override
            public CodeBlock visitVoid() {
                throw new AssertionError("Void literals are meaningless, what are you even doing");
            }

            @Override
            public CodeBlock visitEnum(final ThriftType tt) {
                EnumType enumType;
                try {
                    enumType = schema.findEnumByType(tt);
                } catch (NoSuchElementException e) {
                    throw new AssertionError("Missing enum type: " + tt.name());
                }

                EnumType.Member member;
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
                        .add("$T.$L", typeResolver.getJavaClass(tt), member.name())
                        .build();
            }

            @Override
            public CodeBlock visitList(ThriftType.ListType listType) {
                if (value.getAsList().isEmpty()) {
                    return CodeBlock.builder().add("$T.emptyList()", TypeNames.COLLECTIONS).build();
                }
                return visitCollection(listType, "list", "unmodifiableList");
            }

            @Override
            public CodeBlock visitSet(ThriftType.SetType setType) {
                if (value.getAsList().isEmpty()) {
                    return CodeBlock.builder().add("$T.emptySet()", TypeNames.COLLECTIONS).build();
                }
                return visitCollection(setType, "set", "unmodifiableSet");
            }

            @Override
            public CodeBlock visitMap(ThriftType.MapType mapType) {
                if (value.getAsMap().isEmpty()) {
                    return CodeBlock.builder().add("$T.emptyMap()", TypeNames.COLLECTIONS).build();
                }
                return visitCollection(mapType, "map", "unmodifiableMap");
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
            public CodeBlock visitUserType(ThriftType userType) {
                throw new IllegalStateException("nested structs not implemented");
            }

            @Override
            public CodeBlock visitTypedef(ThriftType.TypedefType typedefType) {
                return null;
            }
        });
    }
}
