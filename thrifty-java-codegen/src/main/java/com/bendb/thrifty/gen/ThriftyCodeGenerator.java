package com.bendb.thrifty.gen;

import com.bendb.thrifty.Adapter;
import com.bendb.thrifty.StructBuilder;
import com.bendb.thrifty.TType;
import com.bendb.thrifty.ThriftField;
import com.bendb.thrifty.protocol.FieldMetadata;
import com.bendb.thrifty.protocol.ListMetadata;
import com.bendb.thrifty.protocol.MapMetadata;
import com.bendb.thrifty.protocol.SetMetadata;
import com.bendb.thrifty.protocol.StructMetadata;
import com.bendb.thrifty.protocol.Protocol;
import com.bendb.thrifty.schema.EnumType;
import com.bendb.thrifty.schema.Field;
import com.bendb.thrifty.schema.NamespaceScope;
import com.bendb.thrifty.schema.StructType;
import com.bendb.thrifty.schema.ThriftType;
import com.bendb.thrifty.util.ProtocolUtil;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import okio.ByteString;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.annotation.Generated;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ThriftyCodeGenerator {
    public static final String ADAPTER_FIELDNAME = "ADAPTER";

    private static final DateTimeFormatter DATE_FORMATTER =
            ISODateTimeFormat.dateTime().withZoneUTC();

    static final ClassName STRING = ClassName.get(String.class);
    static final ClassName LIST = ClassName.get(List.class);
    static final ClassName MAP = ClassName.get(Map.class);
    static final ClassName MAP_ENTRY = ClassName.get(Map.Entry.class);
    static final ClassName SET = ClassName.get(Set.class);
    static final ClassName BYTE_STRING = ClassName.get(ByteString.class);

    static final ClassName LIST_META = ClassName.get(ListMetadata.class);
    static final ClassName SET_META = ClassName.get(SetMetadata.class);
    static final ClassName MAP_META = ClassName.get(MapMetadata.class);
    static final ClassName STRUCT_META = ClassName.get(StructMetadata.class);
    static final ClassName FIELD_META = ClassName.get(FieldMetadata.class);

    static final ClassName PROTOCOL = ClassName.get(Protocol.class);
    static final ClassName PROTO_UTIL = ClassName.get(ProtocolUtil.class);
    static final ClassName PROTOCOL_EXCEPTION = ClassName.get(ProtocolException.class);
    static final ClassName TTYPE = ClassName.get(TType.class);

    static final ClassName BUILDER = ClassName.get(StructBuilder.class);
    static final ClassName ADAPTER = ClassName.get(Adapter.class);

    static final ClassName FIELD_METADATA = ClassName.get(FieldMetadata.class);

    /**
     * A mapping of {@link TType} constant values to their Java names.
     */
    static final ImmutableMap<Byte, String> TTYPE_NAMES;

    static {
        ImmutableMap.Builder<Byte, String> map = ImmutableMap.builder();
        map.put(TType.BOOL, "BOOL");
        map.put(TType.BYTE, "BYTE");
        map.put(TType.I16, "I16");
        map.put(TType.I32, "I32");
        map.put(TType.I64, "I64");
        map.put(TType.DOUBLE, "DOUBLE");
        map.put(TType.STRING, "STRING");
        map.put(TType.ENUM, "ENUM");
        map.put(TType.STRUCT, "STRUCT");
        map.put(TType.LIST, "LIST");
        map.put(TType.SET, "SET");
        map.put(TType.MAP, "MAP");
        map.put(TType.VOID, "VOID");
        map.put(TType.STOP, "STOP");
        TTYPE_NAMES = map.build();
    }

    private final Map<String, ClassName> nameCache = new LinkedHashMap<>();
    private final ClassName listClassName;
    private final ClassName setClassName;
    private final ClassName mapClassName;

    public ThriftyCodeGenerator() {
        this(ClassName.get(ArrayList.class), ClassName.get(HashSet.class), ClassName.get(HashMap.class));
    }

    private ThriftyCodeGenerator(ClassName listClassName, ClassName setClassName, ClassName mapClassName) {
        this.listClassName = listClassName;
        this.setClassName = setClassName;
        this.mapClassName = mapClassName;
    }

    public ThriftyCodeGenerator withListType(ClassName listClassName) {
        return new ThriftyCodeGenerator(listClassName, setClassName, mapClassName);
    }

    public ThriftyCodeGenerator withSetType(ClassName setClassName) {
        return new ThriftyCodeGenerator(listClassName, setClassName, mapClassName);
    }

    public ThriftyCodeGenerator withMapType(ClassName mapClassName) {
        return new ThriftyCodeGenerator(listClassName, setClassName, mapClassName);
    }


    TypeSpec buildStruct(StructType type) {
        String packageName = type.getNamespaceFor(NamespaceScope.JAVA);
        ClassName structTypeName = ClassName.get(packageName, type.name());
        ClassName builderTypeName = structTypeName.nestedClass("Builder");
        TypeName adapterSuperclass = ParameterizedTypeName.get(ADAPTER, structTypeName, builderTypeName);

        TypeSpec.Builder structBuilder = TypeSpec.classBuilder(type.name())
                .addAnnotation(generatedAnnotation())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        if (type.hasJavadoc()) {
            structBuilder.addJavadoc(type.documentation());
        }

        if (type.isException()) {
            structBuilder.superclass(Exception.class);
        }

        TypeSpec builderSpec = builderFor(type, structTypeName, builderTypeName);
        TypeSpec adapterSpec = adapterFor(type, structTypeName, builderTypeName);

        structBuilder.addType(builderSpec);
        structBuilder.addField(FieldSpec.builder(adapterSuperclass, ADAPTER_FIELDNAME)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", adapterSpec)
                .build());

        MethodSpec.Builder ctor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(builderTypeName, "builder");

        // Add fields to both struct and builder classes
        for (Field field : type.fields()) {
            ThriftType fieldType = field.type();
            TypeName fieldTypeName = getJavaClassName(fieldType.getTrueType());

            FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldTypeName, field.name())
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addAnnotation(fieldAnnotation(field));

            if (field.hasJavadoc()) {
                fieldBuilder = fieldBuilder.addJavadoc(field.documentation());
            }

            structBuilder.addField(fieldBuilder.build());

            // Update the struct ctor
            ctor.addStatement("this.$N = builder.$N", field.name(), field.name());
        }

        structBuilder.addMethod(ctor.build());


        return structBuilder.build();
    }

    private TypeSpec builderFor(
            StructType structType,
            ClassName structClassName,
            ClassName builderClassName) {
        TypeName builderSuperclassName = ParameterizedTypeName.get(BUILDER, structClassName);
        TypeSpec.Builder builder = TypeSpec.classBuilder("Builder")
                .addSuperinterface(builderSuperclassName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build());

        MethodSpec.Builder buildMethodBuilder = MethodSpec.methodBuilder("build")
                .addAnnotation(Override.class)
                .returns(structClassName)
                .addModifiers(Modifier.PUBLIC);

        MethodSpec.Builder resetBuilder = MethodSpec.methodBuilder("reset")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC);

        MethodSpec.Builder copyCtor = MethodSpec.constructorBuilder()
                .addParameter(structClassName, "struct");

        if (structType.isUnion()) {
            buildMethodBuilder.addStatement("int setFields = 0");
        }

        for (Field field : structType.fields()) {
            TypeName javaTypeName = getJavaClassName(field.type());
            String fieldName = field.name();
            FieldSpec.Builder f = FieldSpec.builder(javaTypeName, fieldName, Modifier.PRIVATE);

            if (field.hasJavadoc()) {
                f.addJavadoc(field.documentation());
            }

            builder.addField(f.build());

            MethodSpec setter = MethodSpec.methodBuilder(fieldName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(builderClassName)
                    .addParameter(javaTypeName, fieldName)
                    .addStatement("this.$N = $N", fieldName, fieldName)
                    .addStatement("return this")
                    .build();

            builder.addMethod(setter);

            if (structType.isUnion()) {
                buildMethodBuilder
                        .beginControlFlow("if (this.$N != null)")
                        .addStatement("++setFields")
                        .endControlFlow();
            } else {
                if (field.required()) {
                    buildMethodBuilder.beginControlFlow("if (this.$N == null)", fieldName);
                    buildMethodBuilder.addStatement(
                            "throw new $T($S)",
                            PROTOCOL_EXCEPTION,
                            "Required field '" + fieldName + "' is missing");
                    buildMethodBuilder.endControlFlow();
                }
            }

            resetBuilder.addStatement("this.$N = null", fieldName);
            copyCtor.addStatement("this.$N = $N.$N", fieldName, "struct", fieldName);
        }

        if (structType.isUnion()) {
            buildMethodBuilder
                    .beginControlFlow("if (setFields != 1)")
                    .addStatement(
                            "throw new $T($S + setFields + $S)",
                            PROTOCOL_EXCEPTION,
                            "Invalid union; ",
                            " field(s) were set")
                    .endControlFlow();
        }

        buildMethodBuilder.addStatement("return new $T(this)", structClassName);
        builder.addMethod(copyCtor.build());
        builder.addMethod(buildMethodBuilder.build());
        builder.addMethod(resetBuilder.build());

        return builder.build();
    }

    private TypeSpec adapterFor(StructType structType, ClassName structClassName, ClassName builderClassName) {
        TypeName adapterSuperclass = ParameterizedTypeName.get(ADAPTER, structClassName, builderClassName);

        final MethodSpec.Builder write = MethodSpec.methodBuilder("write")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(PROTOCOL, "protocol")
                .addParameter(structClassName, "struct")
                .addException(IOException.class);

        final MethodSpec.Builder read = MethodSpec.methodBuilder("read")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(getJavaClassName(structType.type()))
                .addParameter(PROTOCOL, "protocol")
                .addParameter(builderClassName, "builder")
                .addException(IOException.class);

        final MethodSpec readHelper = MethodSpec.methodBuilder("read")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(getJavaClassName(structType.type()))
                .addParameter(PROTOCOL, "protocol")
                .addException(IOException.class)
                .addStatement("return read(protocol, new $T())", builderClassName)
                .build();

        // First, the writer
        write.addStatement("protocol.writeStructBegin($S)", structType.name());

        // Then, the reader - set up the field-reading loop.
        read.addStatement("protocol.readStructBegin()");
        read.beginControlFlow("while (true)");
        read.addStatement("$T field = protocol.readFieldBegin()", FIELD_METADATA);
        read.beginControlFlow("if (field.fieldId == $T.STOP)", TTYPE);
        read.addStatement("break");
        read.endControlFlow();

        if (structType.fields().size() > 0) {
            read.beginControlFlow("switch (field.fieldId)");
        }

        for (Field field : structType.fields()) {
            boolean optional = !field.required();
            final String name = field.name();
            final ThriftType tt = field.type().getTrueType();
            byte typeCode = typeCode(tt);
            String typeCodeName = TTYPE_NAMES.get(typeCode);

            // Write
            if (optional) {
                write.beginControlFlow("if (struct.$N != null)", name);
            }

            write.addStatement("protocol.writeFieldBegin($S, $L, $T.$L)", name,  field.id(), TTYPE, typeCodeName);

            tt.accept(new GenerateWriterVisitor(write, "protocol", "struct", field));

            write.addStatement("protocol.writeFieldEnd()");

            if (optional) {
                write.endControlFlow();
            }

            // Read
            read.beginControlFlow("case $L:", field.id());
            new GenerateReaderVisitor(read, field).generate();
            read.endControlFlow(); // end case block
            read.addStatement("break");

        }

        write.addStatement("protocol.writeFieldStop()");
        write.addStatement("protocol.writeStructEnd()");

        if (structType.fields().size() > 0) {
            read.endControlFlow(); //
        }

        read.addStatement("protocol.readFieldEnd()");
        read.endControlFlow(); // end while
        read.addStatement("return builder.build()");

        return TypeSpec.anonymousClassBuilder("")
                .superclass(adapterSuperclass)
                .addMethod(write.build())
                .addMethod(read.build())
                .addMethod(readHelper)
                .build();
    }

    private static AnnotationSpec fieldAnnotation(Field field) {
        AnnotationSpec.Builder ann = AnnotationSpec.builder(ThriftField.class)
                .addMember("fieldId", "$L", field.id())
                .addMember("isRequired", "$L", field.required());

        String typedef = field.typedefName();
        if (!Strings.isNullOrEmpty(typedef)) {
            ann = ann.addMember("typedefName", "$S", typedef);
        }

        return ann.build();
    }

    private static AnnotationSpec generatedAnnotation() {
        return AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", ThriftyCodeGenerator.class.getCanonicalName())
                .addMember("date", "$S", DATE_FORMATTER.print(System.currentTimeMillis()))
                .build();
    }

    TypeSpec buildEnum(EnumType type) {
        ClassName enumClassName = ClassName.get(
                type.getNamespaceFor(NamespaceScope.JAVA),
                type.name());

        TypeSpec.Builder builder = TypeSpec.enumBuilder(type.name())
                .addAnnotation(generatedAnnotation())
                .addModifiers(Modifier.PUBLIC)
                .addField(int.class, "code", Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(int.class, "code")
                        .addStatement("this.$N = $N", "code", "code")
                        .build());

        if (type.hasJavadoc()) {
            builder.addJavadoc(type.documentation());
        }

        MethodSpec.Builder fromCodeMethod = MethodSpec.methodBuilder("fromCode")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(enumClassName)
                .addParameter(int.class, "code")
                .beginControlFlow("switch (code)");

        for (EnumType.Member member : type.members()) {
            String name = member.name();

            int value = member.value();

            TypeSpec.Builder memberBuilder = TypeSpec.anonymousClassBuilder("$L", value);
            if (member.hasJavadoc()) {
                memberBuilder.addJavadoc(member.documentation());
            }

            builder.addEnumConstant(name, memberBuilder.build());

            fromCodeMethod.addStatement("case $L: return $N", value, name);
        }

        fromCodeMethod
                .addStatement("default: return null")
                .endControlFlow();

        builder.addMethod(fromCodeMethod.build());

        return builder.build();
    }

    private TypeName getJavaClassName(ThriftType type) {
        return type.getTrueType().accept(typeNameVisitor);
    }

    private byte typeCode(ThriftType type) {
        return type.getTrueType().accept(TYPE_CODE_VISITOR);
    }

    /**
     * Generates Java code to write the value of a field in a {@link Adapter#write}
     * implementation.
     *
     * Handles nested values like lists, sets, maps, and user types.
     */
    class GenerateWriterVisitor implements ThriftType.Visitor<Void> {
        private MethodSpec.Builder write;
        private String proto;
        private Deque<String> nameStack = new LinkedList<>();
        private NameAllocator nameAllocator;
        private int scopeLevel;

        GenerateWriterVisitor(
                MethodSpec.Builder write,
                String proto,
                String subject,
                Field field) {
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
            initCollectionHelpers();
            String tag = "item" + scopeLevel;
            String item = nameAllocator.newName(tag, tag);

            ThriftType tt = listType.elementType().getTrueType();
            byte typeCode = typeCode(tt);
            String typeCodeName = TTYPE_NAMES.get(typeCode);

            write.addStatement("$N.writeListBegin($T.$L, $L.size())", proto, TTYPE, typeCodeName, nameStack.peek());
            write.beginControlFlow("for ($T $N : $L)", getJavaClassName(tt), item, nameStack.peek());

            scopeLevel++;
            nameStack.push(item);
            tt.accept(this);
            nameStack.pop();
            scopeLevel--;

            write.endControlFlow();
            return null;
        }

        @Override
        public Void visitSet(ThriftType.SetType setType) {
            initCollectionHelpers();
            String tag = "item" + scopeLevel;
            String item = nameAllocator.newName(tag, tag);

            ThriftType tt = setType.elementType().getTrueType();
            byte typeCode = typeCode(tt);
            String typeCodeName = TTYPE_NAMES.get(typeCode);

            write.addStatement("$N.writeSetBegin($T.$L, $L.size())", proto, TTYPE, typeCodeName, nameStack.peek());
            write.beginControlFlow("for ($T $N : $L)", getJavaClassName(tt), item, nameStack.peek());

            scopeLevel++;
            nameStack.push(item);
            tt.accept(this);
            nameStack.pop();
            scopeLevel--;

            write.endControlFlow();
            return null;
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
                    "$N.writeMapBegin($T.$L, $T.$L, $L.size()",
                    proto,
                    TTYPE,
                    TTYPE_NAMES.get(typeCode(kt)),
                    TTYPE,
                    TTYPE_NAMES.get(typeCode(vt)),
                    nameStack.peek());

            TypeName keyTypeName = getJavaClassName(kt);
            TypeName valueTypeName = getJavaClassName(vt);
            TypeName entry = ParameterizedTypeName.get(MAP_ENTRY, keyTypeName, valueTypeName);
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
        private NameAllocator nameAllocator;
        private Deque<String> nameStack = new ArrayDeque<>();
        private MethodSpec.Builder read;
        private Field field;
        private int scope;

        GenerateReaderVisitor(MethodSpec.Builder read, Field field) {
            this.read = read;
            this.field = field;
        }

        public void generate() {
            byte fieldTypeCode = typeCode(field.type());
            if (fieldTypeCode == TType.ENUM) {
                // Enums are I32 on the wire
                fieldTypeCode = TType.I32;
            }
            String codeName = TTYPE_NAMES.get(fieldTypeCode);
            read.beginControlFlow("if (field.typeId == $T.$L)", TTYPE, codeName);

            // something
            nameStack.push("value");
            field.type().getTrueType().accept(this);
            nameStack.pop();

            read.addStatement("builder.$N(value)", field.name());

            read.nextControlFlow("else");
            read.addStatement("$T.skip(protocol, field.typeId)", PROTO_UTIL);
            read.endControlFlow();

        }

        @Override
        public Void visitBool() {
            read.addStatement("$T $N = protocol.readBool()", ClassName.BOOLEAN.box(), nameStack.peek());
            return null;
        }

        @Override
        public Void visitByte() {
            read.addStatement("$T $N = protocol.readByte()", ClassName.BYTE.box(), nameStack.peek());
            return null;
        }

        @Override
        public Void visitI16() {
            read.addStatement("$T $N = protocol.readI16()", ClassName.SHORT.box(), nameStack.peek());
            return null;
        }

        @Override
        public Void visitI32() {
            read.addStatement("$T $N = protocol.readI32()", ClassName.INT.box(), nameStack.peek());
            return null;
        }

        @Override
        public Void visitI64() {
            read.addStatement("$T $N = protocol.readI64()", ClassName.LONG.box(), nameStack.peek());
            return null;
        }

        @Override
        public Void visitDouble() {
            read.addStatement("$T $N = protocol.readDouble()", ClassName.DOUBLE.box(), nameStack.peek());
            return null;
        }

        @Override
        public Void visitString() {
            read.addStatement("$T $N = protocol.readString()", STRING, nameStack.peek());
            return null;
        }

        @Override
        public Void visitBinary() {
            read.addStatement("$T $N = protocol.readBinary()", BYTE_STRING, nameStack.peek());
            return null;
        }

        @Override
        public Void visitVoid() {
            throw new AssertionError("Cannot read void");
        }

        @Override
        public Void visitEnum(ThriftType userType) {
            String target = nameStack.peek();
            TypeName enumType = getJavaClassName(userType);
            read.addStatement("$T $N = $T.fromCode(protocol.readI32())", enumType, target, enumType);
            return null;
        }

        @Override
        public Void visitList(ThriftType.ListType listType) {
            initNameAllocator();

            TypeName elementType = getJavaClassName(listType.elementType().getTrueType());
            TypeName genericListType = ParameterizedTypeName.get(LIST, elementType);
            TypeName listImplType = ParameterizedTypeName.get(listClassName, elementType);

            String listInfo = "listMetadata" + scope;
            String idx = "i" + scope;
            String item = "item" + scope;

            read.addStatement("$T $N = protocol.readListBegin()", LIST_META, listInfo);
            read.addStatement("$T $N = new $T($N.size)", genericListType, nameStack.peek(), listImplType, listInfo);
            read.beginControlFlow("for (int $N = 0; $N < $N.size; ++$N)", idx, idx, listInfo, idx);

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

            TypeName elementType = getJavaClassName(setType.elementType().getTrueType());
            TypeName genericSetType = ParameterizedTypeName.get(SET, elementType);
            TypeName setImplType = ParameterizedTypeName.get(setClassName, elementType);

            String setInfo = "setMetadata" + scope;
            String idx = "i" + scope;
            String item = "item" + scope;

            read.addStatement("$T $N = protocol.readSetBegin()", SET_META, setInfo);
            read.addStatement("$T $N = new $T($N.size)", genericSetType, nameStack.peek(), setImplType, setInfo);
            read.beginControlFlow("for (int $N = 0; $N < $N.size; ++$N)", idx, idx, setInfo, idx);

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

            TypeName keyType = getJavaClassName(mapType.keyType().getTrueType());
            TypeName valueType = getJavaClassName(mapType.valueType().getTrueType());
            TypeName genericMapType = ParameterizedTypeName.get(MAP, keyType, valueType);
            TypeName mapImplType = ParameterizedTypeName.get(mapClassName, keyType, valueType);

            String mapInfo = "mapMetadata" + scope;
            String idx = "i" + scope;
            String key = "key" + scope;
            String value = "value" + scope;
            ++scope;

            read.addStatement("$T $N = protocol.readMapBegin()", MAP_META, mapInfo);
            read.addStatement("$T $N = new $T($N.size)", genericMapType, nameStack.peek(), mapImplType, mapInfo);
            read.beginControlFlow("for (int $N = 0; $N < $N.size; ++$N)", idx, idx, mapInfo, idx);

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
            TypeName typeName = getJavaClassName(userType);
            read.addStatement("$T $N = $T.ADAPTER.read(protocol)", typeName, nameStack.peek(), typeName);
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

    /**
     * A Visitor that converts a {@link ThriftType} into a {@link TypeName}.
     */
    private final ThriftType.Visitor<TypeName> typeNameVisitor = new ThriftType.Visitor<TypeName>() {
        @Override
        public TypeName visitBool() {
            return ClassName.BOOLEAN.box();
        }

        @Override
        public TypeName visitByte() {
            return ClassName.BYTE.box();
        }

        @Override
        public TypeName visitI16() {
            return ClassName.SHORT.box();
        }

        @Override
        public TypeName visitI32() {
            return ClassName.INT.box();
        }

        @Override
        public TypeName visitI64() {
            return ClassName.LONG.box();
        }

        @Override
        public TypeName visitDouble() {
            return ClassName.DOUBLE.box();
        }

        @Override
        public TypeName visitString() {
            return STRING;
        }

        @Override
        public TypeName visitBinary() {
            return BYTE_STRING;
        }

        @Override
        public TypeName visitVoid() {
            return ClassName.VOID;
        }

        @Override
        public TypeName visitEnum(ThriftType userType) {
            return visitUserType(userType);
        }

        @Override
        public TypeName visitList(ThriftType.ListType listType) {
            ThriftType elementType = listType.elementType().getTrueType();
            TypeName elementTypeName = elementType.accept(this);
            return ParameterizedTypeName.get(LIST, elementTypeName);
        }

        @Override
        public TypeName visitSet(ThriftType.SetType setType) {
            ThriftType elementType = setType.elementType().getTrueType();
            TypeName elementTypeName = elementType.accept(this);
            return ParameterizedTypeName.get(SET, elementTypeName);
        }

        @Override
        public TypeName visitMap(ThriftType.MapType mapType) {
            ThriftType keyType = mapType.keyType().getTrueType();
            ThriftType valueType = mapType.valueType().getTrueType();

            TypeName keyTypeName = keyType.accept(this);
            TypeName valueTypeName = valueType.accept(this);
            return ParameterizedTypeName.get(MAP, keyTypeName, valueTypeName);
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
