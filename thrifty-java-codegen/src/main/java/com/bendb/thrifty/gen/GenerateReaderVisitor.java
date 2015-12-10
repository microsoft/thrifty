package com.bendb.thrifty.gen;

import com.bendb.thrifty.Adapter;
import com.bendb.thrifty.TType;
import com.bendb.thrifty.protocol.Protocol;
import com.bendb.thrifty.schema.Field;
import com.bendb.thrifty.schema.ThriftType;
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
final class GenerateReaderVisitor implements ThriftType.Visitor<Void> {
    private Deque<String> nameStack = new ArrayDeque<>();
    private TypeResolver resolver;
    private NameAllocator nameAllocator;
    private MethodSpec.Builder read;
    private Field field;
    private int scope;

    GenerateReaderVisitor(TypeResolver resolver, MethodSpec.Builder read, Field field) {
        this.resolver = resolver;
        this.read = read;
        this.field = field;
    }

    public void generate() {
        byte fieldTypeCode = resolver.getTypeCode(field.type());
        if (fieldTypeCode == TType.ENUM) {
            // Enums are I32 on the wire
            fieldTypeCode = TType.I32;
        }
        String codeName = TypeNames.getTypeCodeName(fieldTypeCode);
        read.beginControlFlow("if (field.typeId == $T.$L)", TypeNames.TTYPE, codeName);

        // something
        nameStack.push("value");
        field.type().getTrueType().accept(this);
        nameStack.pop();

        read.addStatement("builder.$N(value)", field.name());

        read.nextControlFlow("else");
        read.addStatement("$T.skip(protocol, field.typeId)", TypeNames.PROTO_UTIL);
        read.endControlFlow();

    }

    @Override
    public Void visitBool() {
        read.addStatement("$T $N = protocol.readBool()", TypeNames.BOOLEAN.unbox(), nameStack.peek());
        return null;
    }

    @Override
    public Void visitByte() {
        read.addStatement("$T $N = protocol.readByte()", TypeNames.BYTE.unbox(), nameStack.peek());
        return null;
    }

    @Override
    public Void visitI16() {
        read.addStatement("$T $N = protocol.readI16()", TypeNames.SHORT.unbox(), nameStack.peek());
        return null;
    }

    @Override
    public Void visitI32() {
        read.addStatement("$T $N = protocol.readI32()", TypeNames.INTEGER.unbox(), nameStack.peek());
        return null;
    }

    @Override
    public Void visitI64() {
        read.addStatement("$T $N = protocol.readI64()", TypeNames.LONG.unbox(), nameStack.peek());
        return null;
    }

    @Override
    public Void visitDouble() {
        read.addStatement("$T $N = protocol.readDouble()", TypeNames.DOUBLE.unbox(), nameStack.peek());
        return null;
    }

    @Override
    public Void visitString() {
        read.addStatement("$T $N = protocol.readString()", TypeNames.STRING, nameStack.peek());
        return null;
    }

    @Override
    public Void visitBinary() {
        read.addStatement("$T $N = protocol.readBinary()", TypeNames.BYTE_STRING, nameStack.peek());
        return null;
    }

    @Override
    public Void visitVoid() {
        throw new AssertionError("Cannot read void");
    }

    @Override
    public Void visitEnum(ThriftType userType) {
        String target = nameStack.peek();
        TypeName enumType = resolver.getJavaClass(userType);
        read.addStatement("$1T $2N = $1T.fromCode(protocol.readI32())", enumType, target);
        return null;
    }

    @Override
    public Void visitList(ThriftType.ListType listType) {
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
    public Void visitSet(ThriftType.SetType setType) {
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
    public Void visitMap(ThriftType.MapType mapType) {
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
    public Void visitUserType(ThriftType userType) {
        TypeName typeName = resolver.getJavaClass(userType);
        read.addStatement("$1T $2N = $1T.ADAPTER.read(protocol)", typeName, nameStack.peek());
        return null;
    }

    @Override
    public Void visitTypedef(ThriftType.TypedefType typedefType) {
        // throw AssertionError?
        typedefType.getTrueType().accept(this);
        return null;
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
