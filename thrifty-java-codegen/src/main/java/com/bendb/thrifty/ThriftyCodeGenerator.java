package com.bendb.thrifty;

import com.bendb.thrifty.protocol.TProtocol;
import com.google.common.base.Strings;
import com.squareup.javapoet.*;
import okio.ByteString;

import javax.lang.model.element.Modifier;
import java.net.ProtocolException;
import java.util.*;

public final class ThriftyCodeGenerator {
    public static final String ADAPTER_FIELDNAME = "ADAPTER";

    static final ClassName STRING = ClassName.get(String.class);
    static final ClassName LIST = ClassName.get(List.class);
    static final ClassName MAP = ClassName.get(Map.class);
    static final ClassName SET = ClassName.get(Set.class);
    static final ClassName BYTE_STRING = ClassName.get(ByteString.class);

    static final ClassName TADAPTER = ClassName.get(ThriftAdapter.class);
    static final ClassName TSTRUCT = ClassName.get(TStruct.class);
    static final ClassName TBUILDER = ClassName.get(TStruct.Builder.class);
    static final ClassName TPROTOCOL = ClassName.get(TProtocol.class);
    static final ClassName PROTOCOL_EXCEPTION = ClassName.get(ProtocolException.class);
    static final ClassName TEXCEPTION = ClassName.get(TException.class);
    static final ClassName TFIELD = ClassName.get(ThriftField.class);
    static final ClassName TTYPE = ClassName.get(TType.class);

    private final Map<String, ClassName> nameCache = new LinkedHashMap<>();
    private final ClassName listClassName = ClassName.get(ArrayList.class);
    private final ClassName mapClassName = ClassName.get(HashMap.class);
    private final ClassName setClassName = ClassName.get(HashSet.class);
    private final boolean includeTupleAdapter;

    private ThriftyCodeGenerator(boolean includeTupleAdapter) {
        this.includeTupleAdapter = includeTupleAdapter;
    }

    public ThriftyCodeGenerator includeTupleAdapter() {
        return new ThriftyCodeGenerator(true);
    }


    TypeSpec buildStruct(StructType type) {
        String packageName = type.getNamespaceFor(NamespaceScope.JAVA);
        ClassName structTypeName = ClassName.get(packageName, type.name());
        ClassName builderTypeName = ClassName.get(packageName, type.name(), "Builder");
        TypeName structSuperclass = ParameterizedTypeName.get(TSTRUCT, structTypeName, builderTypeName);
        TypeName builderSuperclass = ParameterizedTypeName.get(TBUILDER, structTypeName, builderTypeName);
        TypeName adapterSuperclass = ParameterizedTypeName.get(TADAPTER, structTypeName);

        TypeSpec.Builder structBuilder = TypeSpec.classBuilder(type.name())
                .superclass(structSuperclass)
                .addJavadoc(type.documentation())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        TypeSpec.Builder builderBuilder = TypeSpec.classBuilder("Builder")
                .superclass(builderSuperclass)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

        TypeSpec.Builder adapterBuilder = TypeSpec.anonymousClassBuilder("")
                .superclass(adapterSuperclass);

        MethodSpec.Builder ctor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(builderTypeName, "builder");

        // Add fields to both struct and builder classes
        for (Field field : type.fields()) {
            ThriftType fieldType = field.type();
            TypeName fieldTypeName = getJavaClassName(fieldType);

            FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldTypeName, field.name())
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addAnnotation(fieldAnnotation(field));

            if (!Strings.isNullOrEmpty(field.documentation())) {
                fieldBuilder = fieldBuilder.addJavadoc(field.documentation());
            }

            structBuilder.addField(fieldBuilder
                    .build());

            // Add a builder field and setter method for the new field
            builderBuilder.addField(
                    FieldSpec.builder(fieldTypeName, field.name(), Modifier.PRIVATE).build());

            MethodSpec.Builder builderMethod = MethodSpec.methodBuilder(field.name())
                    .addParameter(fieldTypeName, field.name())
                    .returns(builderTypeName);

            if (field.required()) {
                builderMethod
                        .beginControlFlow("if ($N == null)", field.name())
                        .addStatement("throw new NullPointerException($S)", field.name())
                        .endControlFlow();
            }

            builderMethod
                    .addStatement("this.$N = $N", field.name(), field.name())
                    .addStatement("return this");

            builderBuilder.addMethod(builderMethod.build());

            // Update the struct ctor
            if (field.required()) {
                ctor.beginControlFlow("if (builder.$N == null)", field.name());
                ctor.addStatement("throw new $T($S)", PROTOCOL_EXCEPTION, "Missing required field: " + field.name());
                ctor.endControlFlow();
            }

            ctor.addStatement("this.$N = builder.$N", field.name(), field.name());
        }

        builderBuilder.addMethod(MethodSpec.methodBuilder("build")
                .returns(structTypeName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addStatement("return new $T(this)", structTypeName)
                .build());

        MethodSpec.Builder writeMethod = MethodSpec.methodBuilder("write")
                .addAnnotation(Override.class)
                .addParameter(TPROTOCOL, "protocol")
                .addParameter(structTypeName, "struct")
                .addStatement("$N.writeStructBegin($S)", "protocol", type.name());

        for (Field field : type.fields()) {
            switch (field.t)
        }

        writeMethod.addStatement("$N.writeFieldStop()", "protocol");
        writeMethod.addStatement("$N.writeStructEnd()", "protocol");

        adapterBuilder.addMethod(writeMethod.build());

        structBuilder.addMethod(ctor.build());
        structBuilder.addType(builderBuilder.build());
        structBuilder.addField(FieldSpec.builder(adapterSuperclass, ADAPTER_FIELDNAME)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", adapterBuilder.build())
                .build());

        return structBuilder.build();
    }

    private static AnnotationSpec fieldAnnotation(Field field) {
        AnnotationSpec.Builder ann = AnnotationSpec.builder(ThriftField.class)
                .addMember("tag", "$L", field.id())
                .addMember("required", "$L", field.required());

        String typedef = field.typedefName();
        if (!Strings.isNullOrEmpty(typedef)) {
            ann = ann.addMember("typedefName", "$S", typedef);
        }

        return ann.build();
    }

    TypeSpec buildEnum(EnumType type) {
        TypeSpec.Builder builder = TypeSpec.enumBuilder(type.name())
                .addJavadoc(type.documentation())
                .addModifiers(Modifier.PUBLIC)
                .addField(int.class, "code", Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(int.class, "code")
                        .addStatement("this.$N = $N", "code", "code")
                        .build());


        for (EnumType.Member member : type.members()) {
            String name = member.name();

            int value = member.value();

            builder.addEnumConstant(
                    name, TypeSpec.anonymousClassBuilder("$L", value)
                            .addJavadoc(member.documentation())
                            .build());
        }

        ClassName enumClassName = ClassName.get(
                type.getNamespaceFor(NamespaceScope.JAVA), type.name());

        MethodSpec fromCodeMethod = MethodSpec.methodBuilder("fromCode")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(enumClassName)
                .addParameter(int.class, "code")
                .addCode(CodeBlock.builder()
                        .beginControlFlow("for ($T value : values())", enumClassName)
                        .beginControlFlow("if (value.code == code)")
                        .addStatement("return value")
                        .endControlFlow()
                        .endControlFlow()
                        .addStatement("return null")
                        .build())
                .build();

        ParameterizedTypeName adapterTypeName = ParameterizedTypeName.get(TADAPTER, enumClassName);

        TypeSpec adapter = TypeSpec.anonymousClassBuilder("")
                .superclass(adapterTypeName)
                .addMethod(MethodSpec.methodBuilder("read")
                        .addAnnotation(Override.class)
                        .returns(enumClassName)
                        .addException(PROTOCOL_EXCEPTION)
                        .addParameter(TPROTOCOL, "protocol")
                        .addStatement("int code = $N.readI32()", "protocol")
                        .addStatement("$T value = $T.fromCode(code)", enumClassName, enumClassName)
                        .beginControlFlow("if (value == null)")
                        .addStatement("throw new $T($S)", PROTOCOL_EXCEPTION, "Invalid enum value")
                        .endControlFlow()
                        .addStatement("return value")
                        .build())
                .addMethod(MethodSpec.methodBuilder("write")
                        .addException(PROTOCOL_EXCEPTION)
                        .addParameter(TPROTOCOL, "protocol")
                        .addParameter(enumClassName, "value")
                        .addStatement("$N.writeI32($N.code)", "protocol", "value")
                        .build())
                .build();

        FieldSpec.Builder adapterField = FieldSpec.builder(
                        adapterTypeName,
                        ADAPTER_FIELDNAME,
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", adapter);

        builder.addMethod(fromCodeMethod);
        builder.addField(adapterField.build());

        return builder.build();
    }

    private TypeName getJavaClassName(ThriftType type) {
        if (type.isBuiltin()) {
            if (ThriftType.BOOL == type) {
                return ClassName.BOOLEAN.box();
            }

            if (ThriftType.BYTE == type) {
                return ClassName.BYTE.box();
            }

            if (ThriftType.I16 == type) {
                return ClassName.SHORT.box();
            }

            if (ThriftType.I32 == type) {
                return ClassName.INT.box();
            }

            if (ThriftType.I64 == type) {
                return ClassName.LONG.box();
            }

            if (ThriftType.DOUBLE == type) {
                return ClassName.DOUBLE.box();
            }

            if (ThriftType.STRING == type) {
                return STRING;
            }

            if (ThriftType.BINARY == type) {
                return BYTE_STRING;
            }

            throw new AssertionError("Unexpected builtin type: " + type.name());
        }

        String packageName = type.getNamespace(NamespaceScope.JAVA);
        if (packageName == null) {
            throw new AssertionError("Missing namespace.  Did you forget to add 'namespace java'?");
        }

        String key = packageName + "##" + type.name();
        ClassName cn = nameCache.get(key);
        if (cn == null) {
            cn = ClassName.get(packageName, type.name());
            nameCache.put(key, cn);
        }
        return cn;
    }

    private byte typeCode(ThriftType type) {
        if (type.isBuiltin()) {
            if (ThriftType.BOOL == type) {
                return TType.BOOL;
            }

            if (ThriftType.BYTE == type) {
                return TType.BYTE;
            }

            if (ThriftType.I16 == type) {
                return TType.I16;
            }

            if (ThriftType.I32 == type) {
                return TType.I32;
            }

            if (ThriftType.I64 == type) {
                return TType.I64;
            }

            if (ThriftType.DOUBLE == type) {
                return TType.DOUBLE;
            }

            if (ThriftType.STRING == type) {
                return TType.STRING;
            }

            if (ThriftType.BINARY == type) {
                return TType.STRING;
            }

            throw new AssertionError("Unexpected builtin type: " + type.name());
        }
    }
}
