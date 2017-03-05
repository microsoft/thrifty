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

import com.google.common.base.Strings;
import com.microsoft.thrifty.TType;
import com.microsoft.thrifty.schema.BuiltinType;
import com.microsoft.thrifty.schema.EnumType;
import com.microsoft.thrifty.schema.ListType;
import com.microsoft.thrifty.schema.MapType;
import com.microsoft.thrifty.schema.NamespaceScope;
import com.microsoft.thrifty.schema.ServiceType;
import com.microsoft.thrifty.schema.SetType;
import com.microsoft.thrifty.schema.StructType;
import com.microsoft.thrifty.schema.ThriftType;
import com.microsoft.thrifty.schema.TypedefType;
import com.microsoft.thrifty.schema.UserType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility for getting JavaPoet {@link TypeName} and {@link TType} codes from
 * {@link ThriftType} instances.
 */
final class TypeResolver {
    private final Map<String, ClassName> nameCache = new LinkedHashMap<>();

    private ClassName listClass = ClassName.get(ArrayList.class);
    private ClassName setClass = ClassName.get(HashSet.class);
    private ClassName mapClass = ClassName.get(HashMap.class);

    void setListClass(ClassName listClass) {
        this.listClass = listClass;
    }

    void setSetClass(ClassName setClass) {
        this.setClass = setClass;
    }

    void setMapClass(ClassName mapClass) {
        this.mapClass = mapClass;
    }

    byte getTypeCode(ThriftType thriftType) {
        return thriftType.getTrueType().accept(TYPE_CODE_VISITOR);
    }

    TypeName getJavaClass(ThriftType thriftType) {
        return thriftType.accept(typeNameVisitor);
    }

    ParameterizedTypeName listOf(TypeName elementType) {
        return ParameterizedTypeName.get(listClass, elementType);
    }

    ParameterizedTypeName setOf(TypeName elementType) {
        return ParameterizedTypeName.get(setClass, elementType);
    }

    ParameterizedTypeName mapOf(TypeName keyType, TypeName valueType) {
        return ParameterizedTypeName.get(mapClass, keyType, valueType);
    }

    /**
     * A Visitor that converts a {@link ThriftType} into a {@link TypeName}.
     */
    private final ThriftType.Visitor<TypeName> typeNameVisitor = new ThriftType.Visitor<TypeName>() {
        @Override
        public TypeName visitVoid(BuiltinType voidType) {
            return TypeNames.VOID;
        }

        @Override
        public TypeName visitBool(BuiltinType boolType) {
            return TypeNames.BOOLEAN;
        }

        @Override
        public TypeName visitByte(BuiltinType byteType) {
            return TypeNames.BYTE;
        }

        @Override
        public TypeName visitI16(BuiltinType i16Type) {
            return TypeNames.SHORT;
        }

        @Override
        public TypeName visitI32(BuiltinType i32Type) {
            return TypeNames.INTEGER;
        }

        @Override
        public TypeName visitI64(BuiltinType i64Type) {
            return TypeNames.LONG;
        }

        @Override
        public TypeName visitDouble(BuiltinType doubleType) {
            return TypeNames.DOUBLE;
        }

        @Override
        public TypeName visitString(BuiltinType stringType) {
            return TypeNames.STRING;
        }

        @Override
        public TypeName visitBinary(BuiltinType binaryType) {
            return TypeNames.BYTE_STRING;
        }

        @Override
        public TypeName visitEnum(EnumType enumType) {
            return visitUserType(enumType);
        }

        @Override
        public TypeName visitList(ListType listType) {
            ThriftType elementType = listType.elementType().getTrueType();
            TypeName elementTypeName = elementType.accept(this);
            return ParameterizedTypeName.get(TypeNames.LIST, elementTypeName);
        }

        @Override
        public TypeName visitSet(SetType setType) {
            ThriftType elementType = setType.elementType().getTrueType();
            TypeName elementTypeName = elementType.accept(this);
            return ParameterizedTypeName.get(TypeNames.SET, elementTypeName);
        }

        @Override
        public TypeName visitMap(MapType mapType) {
            ThriftType keyType = mapType.keyType().getTrueType();
            ThriftType valueType = mapType.valueType().getTrueType();

            TypeName keyTypeName = keyType.accept(this);
            TypeName valueTypeName = valueType.accept(this);
            return ParameterizedTypeName.get(TypeNames.MAP, keyTypeName, valueTypeName);
        }

        @Override
        public TypeName visitStruct(StructType structType) {
            return visitUserType(structType);
        }

        @Override
        public TypeName visitTypedef(TypedefType typedefType) {
            return typedefType.oldType().accept(this);
        }

        @Override
        public TypeName visitService(ServiceType serviceType) {
            return visitUserType(serviceType);
        }

        private TypeName visitUserType(UserType userType) {
            String packageName = userType.getNamespaceFor(NamespaceScope.JAVA);
            if (Strings.isNullOrEmpty(packageName)) {
                throw new AssertionError("Missing namespace.  Did you forget to add 'namespace java'?");
            }

            String key = packageName + "##" + userType.name();
            return nameCache.computeIfAbsent(key, k -> ClassName.get(packageName, userType.name()));
        }
    };

    /**
     * A Visitor that converts a {@link ThriftType} into a {@link TType}
     * constant value.
     */
    private static final ThriftType.Visitor<Byte> TYPE_CODE_VISITOR = new ThriftType.Visitor<Byte>() {
        @Override
        public Byte visitBool(BuiltinType boolType) {
            return TType.BOOL;
        }

        @Override
        public Byte visitByte(BuiltinType byteType) {
            return TType.BYTE;
        }

        @Override
        public Byte visitI16(BuiltinType i16Type) {
            return TType.I16;
        }

        @Override
        public Byte visitI32(BuiltinType i32Type) {
            return TType.I32;
        }

        @Override
        public Byte visitI64(BuiltinType i64Type) {
            return TType.I64;
        }

        @Override
        public Byte visitDouble(BuiltinType doubleType) {
            return TType.DOUBLE;
        }

        @Override
        public Byte visitString(BuiltinType stringType) {
            return TType.STRING;
        }

        @Override
        public Byte visitBinary(BuiltinType binaryType) {
            return TType.STRING;
        }

        @Override
        public Byte visitVoid(BuiltinType voidType) {
            return TType.VOID;
        }

        @Override
        public Byte visitEnum(EnumType userType) {
            return TType.ENUM;
        }

        @Override
        public Byte visitList(ListType listType) {
            return TType.LIST;
        }

        @Override
        public Byte visitSet(SetType setType) {
            return TType.SET;
        }

        @Override
        public Byte visitMap(MapType mapType) {
            return TType.MAP;
        }

        @Override
        public Byte visitStruct(StructType userType) {
            return TType.STRUCT;
        }

        @Override
        public Byte visitTypedef(TypedefType typedefType) {
            return typedefType.oldType().accept(this);
        }

        @Override
        public Byte visitService(ServiceType serviceType) {
            throw new AssertionError("Services do not have typecodes");
        }
    };
}
