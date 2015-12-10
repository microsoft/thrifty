package com.bendb.thrifty.gen;

import com.bendb.thrifty.TType;
import com.bendb.thrifty.schema.NamespaceScope;
import com.bendb.thrifty.schema.ThriftType;
import com.google.common.base.Strings;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility for
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
        public TypeName visitBool() {
            return TypeNames.BOOLEAN;
        }

        @Override
        public TypeName visitByte() {
            return TypeNames.BYTE;
        }

        @Override
        public TypeName visitI16() {
            return TypeNames.SHORT;
        }

        @Override
        public TypeName visitI32() {
            return TypeNames.INTEGER;
        }

        @Override
        public TypeName visitI64() {
            return TypeNames.LONG;
        }

        @Override
        public TypeName visitDouble() {
            return TypeNames.DOUBLE;
        }

        @Override
        public TypeName visitString() {
            return TypeNames.STRING;
        }

        @Override
        public TypeName visitBinary() {
            return TypeNames.BYTE_STRING;
        }

        @Override
        public TypeName visitVoid() {
            return TypeNames.VOID;
        }

        @Override
        public TypeName visitEnum(ThriftType userType) {
            return visitUserType(userType);
        }

        @Override
        public TypeName visitList(ThriftType.ListType listType) {
            ThriftType elementType = listType.elementType().getTrueType();
            TypeName elementTypeName = elementType.accept(this);
            return ParameterizedTypeName.get(TypeNames.LIST, elementTypeName);
        }

        @Override
        public TypeName visitSet(ThriftType.SetType setType) {
            ThriftType elementType = setType.elementType().getTrueType();
            TypeName elementTypeName = elementType.accept(this);
            return ParameterizedTypeName.get(TypeNames.SET, elementTypeName);
        }

        @Override
        public TypeName visitMap(ThriftType.MapType mapType) {
            ThriftType keyType = mapType.keyType().getTrueType();
            ThriftType valueType = mapType.valueType().getTrueType();

            TypeName keyTypeName = keyType.accept(this);
            TypeName valueTypeName = valueType.accept(this);
            return ParameterizedTypeName.get(TypeNames.MAP, keyTypeName, valueTypeName);
        }

        @Override
        public TypeName visitUserType(ThriftType userType) {
            String packageName = userType.getNamespace(NamespaceScope.JAVA);
            if (Strings.isNullOrEmpty(packageName)) {
                throw new AssertionError("Missing namespace.  Did you forget to add 'namespace java'?");
            }

            String key = packageName + "##" + userType.name();
            ClassName cn = nameCache.get(key);
            if (cn == null) {
                cn = ClassName.get(packageName, userType.name());
                nameCache.put(key, cn);
            }
            return cn;
        }

        @Override
        public TypeName visitTypedef(ThriftType.TypedefType typedefType) {
            throw new AssertionError("Typedefs should have been resolved");
        }
    };

    /**
     * A Visitor that converts a {@link ThriftType} into a {@link TType}
     * constant value.
     */
    private static final ThriftType.Visitor<Byte> TYPE_CODE_VISITOR = new ThriftType.Visitor<Byte>() {
        @Override
        public Byte visitBool() {
            return TType.BOOL;
        }

        @Override
        public Byte visitByte() {
            return TType.BYTE;
        }

        @Override
        public Byte visitI16() {
            return TType.I16;
        }

        @Override
        public Byte visitI32() {
            return TType.I32;
        }

        @Override
        public Byte visitI64() {
            return TType.I64;
        }

        @Override
        public Byte visitDouble() {
            return TType.DOUBLE;
        }

        @Override
        public Byte visitString() {
            return TType.STRING;
        }

        @Override
        public Byte visitBinary() {
            return TType.STRING;
        }

        @Override
        public Byte visitVoid() {
            return TType.VOID;
        }

        @Override
        public Byte visitEnum(ThriftType userType) {
            return TType.ENUM;
        }

        @Override
        public Byte visitList(ThriftType.ListType listType) {
            return TType.LIST;
        }

        @Override
        public Byte visitSet(ThriftType.SetType setType) {
            return TType.SET;
        }

        @Override
        public Byte visitMap(ThriftType.MapType mapType) {
            return TType.MAP;
        }

        @Override
        public Byte visitUserType(ThriftType userType) {
            return TType.STRUCT;
        }

        @Override
        public Byte visitTypedef(ThriftType.TypedefType typedefType) {
            throw new AssertionError("Typedefs should have been resolved");
        }
    };
}
