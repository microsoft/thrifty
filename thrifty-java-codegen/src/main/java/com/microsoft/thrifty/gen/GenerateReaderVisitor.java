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

import com.microsoft.thrifty.Adapter;
import com.microsoft.thrifty.TType;
import com.microsoft.thrifty.protocol.Protocol;
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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Generates Java code to read a field's value from an open Protocol object.
 *
 * Assumptions:
 * We are inside of {@link Adapter#read(Protocol)}.  Further, we are
 * inside of a single case block for a single field.  There are variables
 * in scope named "protocol" and "builder", representing the connection and
 * the struct builder.
 */
class GenerateReaderVisitor implements ThriftType.Visitor<Void> {
    private Deque<String> nameStack = new ArrayDeque<>();
    private TypeResolver resolver;
    private NameAllocator nameAllocator;
    private MethodSpec.Builder read;
    private String fieldName;
    private ThriftType fieldType;
    private int scope;

    GenerateReaderVisitor(TypeResolver resolver, MethodSpec.Builder read, String fieldName, ThriftType fieldType) {
        this.resolver = resolver;
        this.read = read;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    public void generate() {
        byte fieldTypeCode = resolver.getTypeCode(fieldType);
        if (fieldTypeCode == TType.ENUM) {
            // Enums are I32 on the wire
            fieldTypeCode = TType.I32;
        }
        String codeName = TypeNames.getTypeCodeName(fieldTypeCode);
        read.beginControlFlow("if (field.typeId == $T.$L)", TypeNames.TTYPE, codeName);

        nameStack.push("value");
        fieldType.accept(this);
        nameStack.pop();

        useReadValue("value");

        read.nextControlFlow("else");
        read.addStatement("$T.skip(protocol, field.typeId)", TypeNames.PROTO_UTIL);
        read.endControlFlow();

    }

    protected void useReadValue(String localName) {
        read.addStatement("builder.$N($N)", fieldName, localName);
    }

    @Override
    public Void visitBool(BuiltinType boolType) {
        read.addStatement("$T $N = protocol.readBool()", TypeNames.BOOLEAN.unbox(), nameStack.peek());
        return null;
    }

    @Override
    public Void visitByte(BuiltinType bytetype) {
        read.addStatement("$T $N = protocol.readByte()", TypeNames.BYTE.unbox(), nameStack.peek());
        return null;
    }

    @Override
    public Void visitI16(BuiltinType i16Type) {
        read.addStatement("$T $N = protocol.readI16()", TypeNames.SHORT.unbox(), nameStack.peek());
        return null;
    }

    @Override
    public Void visitI32(BuiltinType i32Type) {
        read.addStatement("$T $N = protocol.readI32()", TypeNames.INTEGER.unbox(), nameStack.peek());
        return null;
    }

    @Override
    public Void visitI64(BuiltinType i64Type) {
        read.addStatement("$T $N = protocol.readI64()", TypeNames.LONG.unbox(), nameStack.peek());
        return null;
    }

    @Override
    public Void visitDouble(BuiltinType doubleType) {
        read.addStatement("$T $N = protocol.readDouble()", TypeNames.DOUBLE.unbox(), nameStack.peek());
        return null;
    }

    @Override
    public Void visitString(BuiltinType stringType) {
        read.addStatement("$T $N = protocol.readString()", TypeNames.STRING, nameStack.peek());
        return null;
    }

    @Override
    public Void visitBinary(BuiltinType binaryType) {
        read.addStatement("$T $N = protocol.readBinary()", TypeNames.BYTE_STRING, nameStack.peek());
        return null;
    }

    @Override
    public Void visitVoid(BuiltinType voidType) {
        throw new AssertionError("Cannot read void");
    }

    @Override
    public Void visitEnum(EnumType enumType) {
        String target = nameStack.peek();
        String qualifiedJavaName = getFullyQualifiedJavaName(enumType);
        read.addStatement("$1L $2N = $1L.findByValue(protocol.readI32())", qualifiedJavaName, target);
        return null;
    }

    @Override
    public Void visitList(ListType listType) {
        initNameAllocator();

        TypeName elementType = resolver.getJavaClass(listType.elementType().getTrueType());
        TypeName genericListType = ParameterizedTypeName.get(TypeNames.LIST, elementType);
        TypeName listImplType = resolver.listOf(elementType);

        String listInfo = "listMetadata" + scope;
        String idx = "i" + scope;
        String item = "item" + scope;

        read.addStatement("$T $N = protocol.readListBegin()", TypeNames.LIST_META, listInfo);
        read.addStatement("$T $N = new $T($N.size)", genericListType, nameStack.peek(), listImplType, listInfo);
        read.beginControlFlow("for (int $1N = 0; $1N < $2N.size; ++$1N)", idx, listInfo);

        ++scope;
        nameStack.push(item);

        listType.elementType().getTrueType().accept(this);

        nameStack.pop();
        --scope;

        read.addStatement("$N.add($N)", nameStack.peek(), item);
        read.endControlFlow();
        read.addStatement("protocol.readListEnd()");

        return null;
    }

    @Override
    public Void visitSet(SetType setType) {
        initNameAllocator();

        TypeName elementType = resolver.getJavaClass(setType.elementType().getTrueType());
        TypeName genericSetType = ParameterizedTypeName.get(TypeNames.SET, elementType);
        TypeName setImplType = resolver.setOf(elementType);

        String setInfo = "setMetadata" + scope;
        String idx = "i" + scope;
        String item = "item" + scope;

        read.addStatement("$T $N = protocol.readSetBegin()", TypeNames.SET_META, setInfo);
        read.addStatement("$T $N = new $T($N.size)", genericSetType, nameStack.peek(), setImplType, setInfo);
        read.beginControlFlow("for (int $1N = 0; $1N < $2N.size; ++$1N)", idx, setInfo);

        ++scope;
        nameStack.push(item);

        setType.elementType().accept(this);

        nameStack.pop();
        --scope;

        read.addStatement("$N.add($N)", nameStack.peek(), item);
        read.endControlFlow();
        read.addStatement("protocol.readSetEnd()");

        return null;
    }

    @Override
    public Void visitMap(MapType mapType) {
        initNameAllocator();

        TypeName keyType = resolver.getJavaClass(mapType.keyType().getTrueType());
        TypeName valueType = resolver.getJavaClass(mapType.valueType().getTrueType());
        TypeName genericMapType = ParameterizedTypeName.get(TypeNames.MAP, keyType, valueType);
        TypeName mapImplType = resolver.mapOf(keyType, valueType);

        String mapInfo = "mapMetadata" + scope;
        String idx = "i" + scope;
        String key = "key" + scope;
        String value = "value" + scope;
        ++scope;

        read.addStatement("$T $N = protocol.readMapBegin()", TypeNames.MAP_META, mapInfo);
        read.addStatement("$T $N = new $T($N.size)", genericMapType, nameStack.peek(), mapImplType, mapInfo);
        read.beginControlFlow("for (int $1N = 0; $1N < $2N.size; ++$1N)", idx, mapInfo);

        nameStack.push(key);
        mapType.keyType().accept(this);
        nameStack.pop();

        nameStack.push(value);
        mapType.valueType().accept(this);
        nameStack.pop();

        read.addStatement("$N.put($N, $N)", nameStack.peek(), key, value);

        read.endControlFlow();
        read.addStatement("protocol.readMapEnd()");

        --scope;

        return null;
    }

    @Override
    public Void visitStruct(StructType userType) {
        String qualifiedJavaName = getFullyQualifiedJavaName(userType);
        read.addStatement("$1L $2N = $1L.ADAPTER.read(protocol)", qualifiedJavaName, nameStack.peek());
        return null;
    }

    @Override
    public Void visitTypedef(TypedefType typedefType) {
        // throw AssertionError?
        typedefType.getTrueType().accept(this);
        return null;
    }

    @Override
    public Void visitService(ServiceType serviceType) {
        throw new AssertionError("Cannot read a service");
    }

    private String getFullyQualifiedJavaName(UserType type) {
        if (type.isBuiltin() || type.isList() || type.isMap() || type.isSet() || type.isTypedef()) {
            throw new AssertionError("Only user and enum types are supported");
        }

        String packageName = type.getNamespaceFor(NamespaceScope.JAVA);
        return packageName + "." + type.name();
    }

    private void initNameAllocator() {
        if (nameAllocator == null) {
            nameAllocator = new NameAllocator();
            nameAllocator.newName("protocol", "protocol");
            nameAllocator.newName("builder", "builder");
            nameAllocator.newName("value", "value");
        }
    }
}
