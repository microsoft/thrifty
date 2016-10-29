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
import com.microsoft.thrifty.protocol.Protocol;
import com.microsoft.thrifty.schema.BuiltinThriftType;
import com.microsoft.thrifty.schema.EnumType;
import com.microsoft.thrifty.schema.ListType;
import com.microsoft.thrifty.schema.MapType;
import com.microsoft.thrifty.schema.NamespaceScope;
import com.microsoft.thrifty.schema.ServiceType;
import com.microsoft.thrifty.schema.SetType;
import com.microsoft.thrifty.schema.StructType;
import com.microsoft.thrifty.schema.ThriftType;
import com.microsoft.thrifty.schema.TypedefType;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Generates Java code to write the value of a field in a {@link Adapter#write}
 * implementation.
 *
 * Handles nested values like lists, sets, maps, and user types.
 */
class GenerateWriterVisitor implements ThriftType.Visitor<Void> {
    private TypeResolver resolver;

    /**
     * The write method under construction.
     */
    private MethodSpec.Builder write;

    /**
     * The name of the {@link Protocol} parameter to {@linkplain #write}.
     */
    private String proto;

    /**
     * A stack of names, with the topmost name being the one currently
     * being written/assigned.
     */
    private Deque<String> nameStack = new LinkedList<>();

    /**
     * A count of nested scopes.  Used to prevent name clashes for iterator
     * and temporary names used when writing nested collections.
     */
    private int scopeLevel;

    private NameAllocator nameAllocator;

    /**
     * Creates a new GenerateWriterVisitor.
     *
     * @param write the {@link Adapter#write} method under construction
     * @param proto the name of the {@link Protocol} parameter to the write method
     * @param subject the name of the struct parameter to the write method
     * @param fieldName the Java name of the field being written
     */
    GenerateWriterVisitor(
            TypeResolver resolver,
            MethodSpec.Builder write,
            String proto,
            String subject,
            String fieldName) {
        this.resolver = resolver;
        this.write = write;
        this.proto = proto;
        nameStack.push(subject + "." + fieldName);
    }

    public Void visitBool(BuiltinThriftType boolType) {
        write.addStatement("$N.writeBool($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitByte(BuiltinThriftType byteType) {
        write.addStatement("$N.writeByte($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitI16(BuiltinThriftType i16Type) {
        write.addStatement("$N.writeI16($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitI32(BuiltinThriftType i32Type) {
        write.addStatement("$N.writeI32($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitI64(BuiltinThriftType i64Type) {
        write.addStatement("$N.writeI64($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitDouble(BuiltinThriftType doubleType) {
        write.addStatement("$N.writeDouble($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitString(BuiltinThriftType stringType) {
        write.addStatement("$N.writeString($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitBinary(BuiltinThriftType binaryType) {
        write.addStatement("$N.writeBinary($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitVoid(BuiltinThriftType voidType) {
        throw new AssertionError("Fields cannot be void");
    }

    @Override
    public Void visitEnum(EnumType enumType) {
        write.addStatement("$N.writeI32($L.value)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitList(ListType listType) {
        visitSingleElementCollection(
                listType.elementType().getTrueType(),
                "writeListBegin",
                "writeListEnd");
        return null;
    }

    @Override
    public Void visitSet(SetType setType) {
        visitSingleElementCollection(
                setType.elementType().getTrueType(),
                "writeSetBegin",
                "writeSetEnd");
        return null;
    }

    private void visitSingleElementCollection(ThriftType elementType, String beginMethod, String endMethod) {
        initCollectionHelpers();
        String tag = "item" + scopeLevel;
        String item = nameAllocator.newName(tag, tag);

        TypeName javaClass = resolver.getJavaClass(elementType);
        byte typeCode = resolver.getTypeCode(elementType);
        String typeCodeName = TypeNames.getTypeCodeName(typeCode);

        write.addStatement(
                "$N.$L($T.$L, $L.size())",
                proto,
                beginMethod,
                TypeNames.TTYPE,
                typeCodeName,
                nameStack.peek());

        write.beginControlFlow("for ($T $N : $L)", javaClass, item, nameStack.peek());

        scopeLevel++;
        nameStack.push(item);
        elementType.accept(this);
        nameStack.pop();
        scopeLevel--;
        write.endControlFlow();

        write.addStatement("$N.$L()", proto, endMethod);
    }

    @Override
    public Void visitMap(MapType mapType) {
        initCollectionHelpers();
        String entryTag = "entry" + scopeLevel;
        String keyTag = "key" + scopeLevel;
        String valueTag = "value" + scopeLevel;

        String entryName = nameAllocator.newName(entryTag, entryTag);
        String keyName = nameAllocator.newName(keyTag, keyTag);
        String valueName = nameAllocator.newName(valueTag, valueTag);

        ThriftType kt = mapType.keyType().getTrueType();
        ThriftType vt = mapType.valueType().getTrueType();

        write.addStatement(
                "$1N.writeMapBegin($2T.$3L, $2T.$4L, $5L.size())",
                proto,
                TypeNames.TTYPE,
                TypeNames.getTypeCodeName(resolver.getTypeCode(kt)),
                TypeNames.getTypeCodeName(resolver.getTypeCode(vt)),
                nameStack.peek());

        TypeName keyTypeName = resolver.getJavaClass(kt);
        TypeName valueTypeName = resolver.getJavaClass(vt);
        TypeName entry = ParameterizedTypeName.get(TypeNames.MAP_ENTRY, keyTypeName, valueTypeName);
        write.beginControlFlow("for ($T $N : $L.entrySet())", entry, entryTag, nameStack.peek());
        write.addStatement("$T $N = $N.getKey()", keyTypeName, keyName, entryName);
        write.addStatement("$T $N = $N.getValue()", valueTypeName, valueName, entryName);

        scopeLevel++;
        nameStack.push(keyName);
        kt.accept(this);
        nameStack.pop();

        nameStack.push(valueName);
        vt.accept(this);
        nameStack.pop();
        scopeLevel--;

        write.endControlFlow();
        write.addStatement("$N.writeMapEnd()", proto);

        return null;
    }

    @Override
    public Void visitStruct(StructType structType) {
        String javaName = structType.getNamespaceFor(NamespaceScope.JAVA) + "." + structType.name();
        write.addStatement("$L.ADAPTER.write($N, $L)", javaName, proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitTypedef(TypedefType typedefType) {
        typedefType.getTrueType().accept(this);
        return null;
    }

    @Override
    public Void visitService(ServiceType serviceType) {
        throw new AssertionError("Cannot write a service");
    }

    private void initCollectionHelpers() {
        if (nameAllocator == null) {
            nameAllocator = new NameAllocator();
            nameAllocator.newName(proto, proto);
        }
    }
}
