package com.bendb.thrifty.gen;

import com.bendb.thrifty.Adapter;
import com.bendb.thrifty.protocol.Protocol;
import com.bendb.thrifty.schema.Field;
import com.bendb.thrifty.schema.ThriftType;
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
     * The write method under construction
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
     * @param field the field being written
     */
    GenerateWriterVisitor(
            TypeResolver resolver,
            MethodSpec.Builder write,
            String proto,
            String subject,
            Field field) {
        this.resolver = resolver;
        this.write = write;
        this.proto = proto;
        nameStack.push(subject + "." + field.name());
    }

    public Void visitBool() {
        write.addStatement("$N.writeBool($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitByte() {
        write.addStatement("$N.writeByte($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitI16() {
        write.addStatement("$N.writeI16($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitI32() {
        write.addStatement("$N.writeI32($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitI64() {
        write.addStatement("$N.writeI64($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitDouble() {
        write.addStatement("$N.writeDouble($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitString() {
        write.addStatement("$N.writeString($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitBinary() {
        write.addStatement("$N.writeBinary($L)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitVoid() {
        throw new AssertionError("Fields cannot be void");
    }

    @Override
    public Void visitEnum(ThriftType userType) {
        write.addStatement("$N.writeI32($L.code)", proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitList(ThriftType.ListType listType) {
        visitSingleElementCollection(
                listType.elementType().getTrueType(),
                "writeListBegin",
                "writeListEnd");
        return null;
    }

    @Override
    public Void visitSet(ThriftType.SetType setType) {
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

        write.addStatement("$N.$L($T.$L, $L.size()", proto, beginMethod, TypeNames.TTYPE, typeCodeName, nameStack.peek());
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
    public Void visitMap(ThriftType.MapType mapType) {
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
    public Void visitUserType(ThriftType userType) {
        write.addStatement("$N.ADAPTER.write($N, $L)", userType.name(), proto, nameStack.peek());
        return null;
    }

    @Override
    public Void visitTypedef(ThriftType.TypedefType typedefType) {
        typedefType.getTrueType().accept(this);
        return null;
    }

    private void initCollectionHelpers() {
        if (nameAllocator == null) {
            nameAllocator = new NameAllocator();
            nameAllocator.newName(proto, proto);
        }
    }
}
