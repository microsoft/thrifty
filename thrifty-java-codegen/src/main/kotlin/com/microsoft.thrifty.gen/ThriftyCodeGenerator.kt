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
package com.microsoft.thrifty.gen

import com.microsoft.thrifty.Struct
import com.microsoft.thrifty.compiler.spi.TypeProcessor
import com.microsoft.thrifty.schema.BuiltinType
import com.microsoft.thrifty.schema.Constant
import com.microsoft.thrifty.schema.EnumType
import com.microsoft.thrifty.schema.Field
import com.microsoft.thrifty.schema.FieldNamingPolicy
import com.microsoft.thrifty.schema.ListType
import com.microsoft.thrifty.schema.Location
import com.microsoft.thrifty.schema.MapType
import com.microsoft.thrifty.schema.NamespaceScope
import com.microsoft.thrifty.schema.Schema
import com.microsoft.thrifty.schema.ServiceType
import com.microsoft.thrifty.schema.SetType
import com.microsoft.thrifty.schema.StructType
import com.microsoft.thrifty.schema.ThriftType
import com.microsoft.thrifty.schema.TypedefType
import com.microsoft.thrifty.schema.UserType
import com.microsoft.thrifty.schema.parser.ListValueElement
import com.microsoft.thrifty.schema.parser.MapValueElement
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.NameAllocator
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ThriftyCodeGenerator {

    private val schema: Schema
    private val typeResolver = TypeResolver()
    private val fieldNamer: FieldNamer
    private val constantBuilder: ConstantBuilder
    private val serviceBuilder: ServiceBuilder
    private var typeProcessor: TypeProcessor? = null
    private var nullabilityAnnotationType: NullabilityAnnotationType = NullabilityAnnotationType.NONE
    private var emitParcelable: Boolean = false
    private var emitFileComment = true
    private var failOnUnknownEnumValues = true
    private var generatedAnnotationType: ClassName? = null
    private val emitGeneratedAnnotations: Boolean
        get() = generatedAnnotationType != null

    constructor(schema: Schema, namingPolicy: FieldNamingPolicy = FieldNamingPolicy.DEFAULT) {

        this.schema = schema
        this.fieldNamer = FieldNamer(namingPolicy)

        typeResolver.setListClass(TypeNames.ARRAY_LIST)
        typeResolver.setSetClass(TypeNames.HASH_SET)
        typeResolver.setMapClass(TypeNames.HASH_MAP)

        constantBuilder = ConstantBuilder(typeResolver, schema)
        serviceBuilder = ServiceBuilder(typeResolver, constantBuilder, fieldNamer)
    }

    fun withListType(listClassName: String): ThriftyCodeGenerator {
        typeResolver.setListClass(ClassName.bestGuess(listClassName))
        return this
    }

    fun withSetType(setClassName: String): ThriftyCodeGenerator {
        typeResolver.setSetClass(ClassName.bestGuess(setClassName))
        return this
    }

    fun withMapType(mapClassName: String): ThriftyCodeGenerator {
        typeResolver.setMapClass(ClassName.bestGuess(mapClassName))
        return this
    }

    fun nullabilityAnnotationType(type: NullabilityAnnotationType): ThriftyCodeGenerator {
        nullabilityAnnotationType = type
        return this
    }

    fun emitParcelable(emitParcelable: Boolean): ThriftyCodeGenerator {
        this.emitParcelable = emitParcelable
        return this
    }

    fun emitFileComment(emitFileComment: Boolean): ThriftyCodeGenerator {
        this.emitFileComment = emitFileComment
        return this
    }

    fun emitGeneratedAnnotations(annotationTypeName: String?) = apply {
        this.generatedAnnotationType = annotationTypeName?.let { ClassName.bestGuess(it) }
    }

    fun usingTypeProcessor(typeProcessor: TypeProcessor): ThriftyCodeGenerator {
        this.typeProcessor = typeProcessor
        return this
    }

    fun failOnUnknownEnumValues(failOnUnknownEnumValues: Boolean): ThriftyCodeGenerator {
        this.failOnUnknownEnumValues = failOnUnknownEnumValues
        return this
    }

    fun generate(directory: Path) {
        generate { file ->
            file?.writeTo(directory)
        }
    }

    fun generate(directory: File) {
        generate { file ->
            file?.writeTo(directory)
        }
    }

    fun generate(appendable: Appendable) {
        generate { file ->
            file?.writeTo(appendable)
        }
    }

    fun generateTypes(): List<JavaFile> {
        val enums = schema.enums.mapNotNull { assembleJavaFile(it, buildEnum(it)) }
        val structs = schema.structs.mapNotNull { assembleJavaFile(it, buildStruct(it)) }
        val exceptions = schema.exceptions.mapNotNull { assembleJavaFile(it, buildStruct(it)) }
        val unions = schema.unions.mapNotNull { assembleJavaFile(it, buildStruct(it)) }

        val constantsByPackage = schema.constants.groupBy { it.getNamespaceFor(NamespaceScope.JAVA)!! }
        val constants = constantsByPackage.mapNotNull { (packageName, values) ->
            assembleJavaFile(packageName, buildConst(values))
        }

        val services = schema.services.flatMap { svc ->
            val iface = serviceBuilder.buildServiceInterface(svc)
            val impl = serviceBuilder.buildService(svc, iface)

            listOf(assembleJavaFile(svc, iface), assembleJavaFile(svc, impl))
        }.filterNotNull()

        return enums + structs + exceptions + unions + constants + services
    }

    private fun generate(writer: (JavaFile?) -> Unit) {
        for (file in generateTypes()) {
            writer(file)
        }
    }

    private fun assembleJavaFile(named: UserType, spec: TypeSpec): JavaFile? {
        val packageName = named.getNamespaceFor(NamespaceScope.JAVA)
        if (packageName == null || packageName == "") {
            throw IllegalArgumentException("A Java package name must be given for java code generation")
        }

        return assembleJavaFile(packageName, spec, named.location)
    }

    private fun assembleJavaFile(packageName: String, spec: TypeSpec, location: Location? = null): JavaFile? {
        val annotatedSpec = if (emitGeneratedAnnotations) {
            spec.toBuilder()
                    .addAnnotation(generatedAnnotation())
                    .build()
        } else {
            spec
        }

        val processedSpec = typeProcessor?.let { processor ->
            processor.process(annotatedSpec) ?: return null
        } ?: annotatedSpec

        val file = JavaFile.builder(packageName, processedSpec)
                .skipJavaLangImports(true)

        if (emitFileComment) {
            file.addFileComment(FILE_COMMENT + DATE_FORMATTER.format(Instant.now()))

            if (location != null) {
                file.addFileComment("\nSource: \$L", location)
            }
        }

        return file.build()
    }

    private fun generatedAnnotation(): AnnotationSpec {
        return AnnotationSpec.builder(generatedAnnotationType!!)
                .addMember("value", "\$S", ThriftyCodeGenerator::class.java.name)
                .addMember("comments", "\$S", "https://github.com/microsoft/thrifty")
                .build()
    }

    private fun buildStruct(type: StructType): TypeSpec {
        val packageName = type.getNamespaceFor(NamespaceScope.JAVA)
        val structTypeName = ClassName.get(packageName, type.name)
        val builderTypeName = structTypeName.nestedClass("Builder")

        val structBuilder = TypeSpec.classBuilder(type.name)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(Struct::class.java)

        if (type.hasJavadoc) {
            structBuilder.addJavadoc("\$L", type.documentation)
        }

        if (type.isException) {
            structBuilder.superclass(java.lang.Exception::class.java)
        }

        if (type.isDeprecated) {
            structBuilder.addAnnotation(AnnotationSpec.builder(TypeNames.DEPRECATED).build())
        }

        val builderSpec = builderFor(type, structTypeName, builderTypeName)
        val adapterSpec = adapterFor(type, structTypeName, builderTypeName)

        if (emitParcelable) {
            generateParcelable(type, structTypeName, structBuilder)
        }

        structBuilder.addType(builderSpec)
        structBuilder.addType(adapterSpec)
        structBuilder.addField(FieldSpec.builder(adapterSpec.superinterfaces[0], ADAPTER_FIELDNAME)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new \$N()", adapterSpec)
                .build())

        val ctor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(builderTypeName, "builder")

        val isUnion = type.isUnion
        for (field in type.fields) {

            val name = fieldNamer.getName(field)
            val fieldType = field.type
            val trueType = fieldType.trueType
            val fieldTypeName = typeResolver.getJavaClass(trueType)

            // Define field
            var fieldBuilder: FieldSpec.Builder = FieldSpec.builder(fieldTypeName, name)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addAnnotation(fieldAnnotation(field))

            if (nullabilityAnnotationType != NullabilityAnnotationType.NONE) {
                val nullability = when {
                    isUnion        -> nullabilityAnnotationType.nullableClassName
                    field.required -> nullabilityAnnotationType.notNullClassName
                    else           -> nullabilityAnnotationType.nullableClassName
                }
                fieldBuilder.addAnnotation(nullability)
            }

            if (field.hasJavadoc) {
                fieldBuilder = fieldBuilder.addJavadoc("\$L", field.documentation)
            }

            if (field.isRedacted) {
                fieldBuilder = fieldBuilder.addAnnotation(AnnotationSpec.builder(TypeNames.REDACTED).build())
            }

            if (field.isObfuscated) {
                fieldBuilder = fieldBuilder.addAnnotation(AnnotationSpec.builder(TypeNames.OBFUSCATED).build())
            }

            if (field.isDeprecated) {
                fieldBuilder = fieldBuilder.addAnnotation(AnnotationSpec.builder(TypeNames.DEPRECATED).build())
            }

            structBuilder.addField(fieldBuilder.build())

            // Update the struct ctor

            val assignment = CodeBlock.builder().add("$[this.\$N = ", name)

            when {
                trueType.isList -> {
                    if (!field.required) {
                        assignment.add("builder.\$N == null ? null : ", name)
                    }
                    assignment.add("\$T.unmodifiableList(builder.\$N)",
                            TypeNames.COLLECTIONS, name)
                }
                trueType.isSet -> {
                    if (!field.required) {
                        assignment.add("builder.\$N == null ? null : ", name)
                    }
                    assignment.add("\$T.unmodifiableSet(builder.\$N)",
                            TypeNames.COLLECTIONS, name)
                }
                trueType.isMap -> {
                    if (!field.required) {
                        assignment.add("builder.\$N == null ? null : ", name)
                    }
                    assignment.add("\$T.unmodifiableMap(builder.\$N)",
                            TypeNames.COLLECTIONS, name)
                }
                else -> assignment.add("builder.\$N", name)
            }

            ctor.addCode(assignment.add(";\n$]").build())
        }

        structBuilder.addMethod(ctor.build())
        structBuilder.addMethod(buildEqualsFor(type))
        structBuilder.addMethod(buildHashCodeFor(type))
        structBuilder.addMethod(buildToStringFor(type))
        structBuilder.addMethod(buildWrite())

        return structBuilder.build()
    }

    private fun generateParcelable(structType: StructType, structName: ClassName, structBuilder: TypeSpec.Builder) {
        structBuilder.addSuperinterface(TypeNames.PARCELABLE)

        structBuilder.addField(FieldSpec.builder(ClassLoader::class.java, "CLASS_LOADER")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("\$T.class.getClassLoader()", structName)
                .build())

        val creatorType = ParameterizedTypeName.get(
                TypeNames.PARCELABLE_CREATOR, structName)
        val creator = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(creatorType)
                .addMethod(MethodSpec.methodBuilder("createFromParcel")
                        .addAnnotation(TypeNames.OVERRIDE)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(structName)
                        .addParameter(TypeNames.PARCEL, "source")
                        .addStatement("return new \$T(source)", structName)
                        .build())
                .addMethod(MethodSpec.methodBuilder("newArray")
                        .addAnnotation(TypeNames.OVERRIDE)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(ArrayTypeName.of(structName))
                        .addParameter(Int::class.javaPrimitiveType, "size")
                        .addStatement("return new \$T[size]", structName)
                        .build())
                .build()

        val parcelCtor = MethodSpec.constructorBuilder()
                .addParameter(TypeNames.PARCEL, "in")
                .addModifiers(Modifier.PRIVATE)

        val parcelWriter = MethodSpec.methodBuilder("writeToParcel")
                .addAnnotation(TypeNames.OVERRIDE)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeNames.PARCEL, "dest")
                .addParameter(Int::class.javaPrimitiveType, "flags")

        for (field in structType.fields) {
            val name = fieldNamer.getName(field)
            val fieldType = typeResolver.getJavaClass(field.type.trueType)
            parcelCtor.addStatement("this.\$N = (\$T) in.readValue(CLASS_LOADER)", name, fieldType)

            parcelWriter.addStatement("dest.writeValue(this.\$N)", name)
        }

        val creatorField = FieldSpec.builder(creatorType, "CREATOR")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("\$L", creator)
                .build()

        structBuilder
                .addField(creatorField)
                .addMethod(MethodSpec.methodBuilder("describeContents")
                        .addAnnotation(TypeNames.OVERRIDE)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(Int::class.javaPrimitiveType!!)
                        .addStatement("return 0")
                        .build())
                .addMethod(parcelCtor.build())
                .addMethod(parcelWriter.build())

    }

    private fun builderFor(
            structType: StructType,
            structClassName: ClassName,
            builderClassName: ClassName): TypeSpec {
        val builderSuperclassName = ParameterizedTypeName.get(TypeNames.BUILDER, structClassName)
        val builder = TypeSpec.classBuilder("Builder")
                .addSuperinterface(builderSuperclassName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)

        val buildMethodBuilder = MethodSpec.methodBuilder("build")
                .addAnnotation(TypeNames.OVERRIDE)
                .returns(structClassName)
                .addModifiers(Modifier.PUBLIC)

        val resetBuilder = MethodSpec.methodBuilder("reset")
                .addAnnotation(TypeNames.OVERRIDE)
                .addModifiers(Modifier.PUBLIC)

        val structParameterBuilder = ParameterSpec.builder(structClassName, "struct")
        if (nullabilityAnnotationType != NullabilityAnnotationType.NONE) {
            structParameterBuilder
                    .addAnnotation(AnnotationSpec.builder(nullabilityAnnotationType.notNullClassName)
                    .build())
        }

        val copyCtor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(structParameterBuilder.build())

        val defaultCtor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)

        if (structType.isUnion) {
            buildMethodBuilder.addStatement("int setFields = 0")
        }

        // Add fields to the struct and set them in the ctor
        val allocator = NameAllocator()
        for (field in structType.fields) {
            val name = fieldNamer.getName(field)
            allocator.newName(name, name)
        }

        val tempNameId = AtomicInteger(0) // used for generating unique names of temporary values
        for (field in structType.fields) {
            val fieldType = field.type.trueType
            val javaTypeName = typeResolver.getJavaClass(fieldType)
            val fieldName = fieldNamer.getName(field)
            val f = FieldSpec.builder(javaTypeName, fieldName, Modifier.PRIVATE)

            if (field.hasJavadoc) {
                f.addJavadoc("\$L", field.documentation)
            }

            if (nullabilityAnnotationType != NullabilityAnnotationType.NONE) {
                f.addAnnotation(AnnotationSpec.builder(nullabilityAnnotationType.nullableClassName).build())
            }

            val fieldDefaultValue = field.defaultValue
            if (fieldDefaultValue != null) {
                val initializer = CodeBlock.builder()
                constantBuilder.generateFieldInitializer(
                        initializer,
                        allocator,
                        tempNameId,
                        "this.$fieldName",
                        fieldType.trueType,
                        fieldDefaultValue,
                        false)
                defaultCtor.addCode(initializer.build())

                resetBuilder.addCode(initializer.build())
            } else {
                resetBuilder.addStatement("this.\$N = null", fieldName)
            }

            builder.addField(f.build())

            val setterBuilder = MethodSpec.methodBuilder(fieldName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(builderClassName)

            val parameterBuilder = ParameterSpec.builder(javaTypeName, fieldName)

            if (nullabilityAnnotationType != NullabilityAnnotationType.NONE) {
                val nullabilityAnnotation = if (field.required) {
                    nullabilityAnnotationType.notNullClassName
                } else {
                    nullabilityAnnotationType.nullableClassName
                }

                parameterBuilder.addAnnotation(AnnotationSpec.builder(nullabilityAnnotation).build())
            }

            setterBuilder.addParameter(parameterBuilder.build())

            if (field.required) {
                setterBuilder.beginControlFlow("if (\$N == null)", fieldName)
                setterBuilder.addStatement(
                        "throw new \$T(\"Required field '\$L' cannot be null\")",
                        TypeNames.NULL_POINTER_EXCEPTION,
                        fieldName)
                setterBuilder.endControlFlow()
            }

            setterBuilder
                    .addStatement("this.\$N = \$N", fieldName, fieldName)
                    .addStatement("return this")

            builder.addMethod(setterBuilder.build())

            if (structType.isUnion) {
                buildMethodBuilder
                        .addStatement("if (this.\$N != null) ++setFields", fieldName)
            } else {
                if (field.required) {
                    buildMethodBuilder.beginControlFlow("if (this.\$N == null)", fieldName)
                    buildMethodBuilder.addStatement(
                            "throw new \$T(\$S)",
                            TypeNames.ILLEGAL_STATE_EXCEPTION,
                            "Required field '$fieldName' is missing")
                    buildMethodBuilder.endControlFlow()
                }
            }

            copyCtor.addStatement("this.\$N = \$N.\$N", fieldName, "struct", fieldName)
        }

        if (structType.isUnion) {
            buildMethodBuilder
                    .beginControlFlow("if (setFields != 1)")
                    .addStatement(
                            "throw new \$T(\$S + setFields + \$S)",
                            TypeNames.ILLEGAL_STATE_EXCEPTION,
                            "Invalid union; ",
                            " field(s) were set")
                    .endControlFlow()
        }

        buildMethodBuilder.addStatement("return new \$T(this)", structClassName)
        builder.addMethod(defaultCtor.build())
        builder.addMethod(copyCtor.build())
        builder.addMethod(buildMethodBuilder.build())
        builder.addMethod(resetBuilder.build())

        return builder.build()
    }

    private fun adapterFor(structType: StructType, structClassName: ClassName, builderClassName: ClassName): TypeSpec {
        val adapterSuperclass = ParameterizedTypeName.get(
                TypeNames.ADAPTER,
                structClassName,
                builderClassName)

        val write = MethodSpec.methodBuilder("write")
                .addAnnotation(TypeNames.OVERRIDE)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeNames.PROTOCOL, "protocol")
                .addParameter(structClassName, "struct")
                .addException(TypeNames.IO_EXCEPTION)

        val read = MethodSpec.methodBuilder("read")
                .addAnnotation(TypeNames.OVERRIDE)
                .addModifiers(Modifier.PUBLIC)
                .returns(typeResolver.getJavaClass(structType))
                .addParameter(TypeNames.PROTOCOL, "protocol")
                .addParameter(builderClassName, "builder")
                .addException(TypeNames.IO_EXCEPTION)

        val readHelper = MethodSpec.methodBuilder("read")
                .addAnnotation(TypeNames.OVERRIDE)
                .addModifiers(Modifier.PUBLIC)
                .returns(typeResolver.getJavaClass(structType))
                .addParameter(TypeNames.PROTOCOL, "protocol")
                .addException(TypeNames.IO_EXCEPTION)
                .addStatement("return read(protocol, new \$T())", builderClassName)
                .build()

        // First, the writer
        write.addStatement("protocol.writeStructBegin(\$S)", structType.name)

        // Then, the reader - set up the field-reading loop.
        read.addStatement("protocol.readStructBegin()")
        read.beginControlFlow("while (true)")
        read.addStatement("\$T field = protocol.readFieldBegin()", TypeNames.FIELD_METADATA)
        read.beginControlFlow("if (field.typeId == \$T.STOP)", TypeNames.TTYPE)
        read.addStatement("break")
        read.endControlFlow()

        if (structType.fields.isNotEmpty()) {
            read.beginControlFlow("switch (field.fieldId)")
        }

        for (field in structType.fields) {
            val fieldName = fieldNamer.getName(field)
            val optional = !field.required // could also be default, but same-same to us.
            val tt = field.type.trueType
            val typeCode = typeResolver.getTypeCode(tt)

            val typeCodeName = TypeNames.getTypeCodeName(typeCode)

            // Write
            if (optional) {
                write.beginControlFlow("if (struct.\$N != null)", fieldName)
            }

            write.addStatement(
                    "protocol.writeFieldBegin(\$S, \$L, \$T.\$L)",
                    field.name, // make sure that we write the Thrift IDL name, and not the name of the Java field
                    field.id,
                    TypeNames.TTYPE,
                    typeCodeName)

            tt.accept(GenerateWriterVisitor(typeResolver, write, "protocol", "struct", fieldName))

            write.addStatement("protocol.writeFieldEnd()")

            if (optional) {
                write.endControlFlow()
            }

            val effectiveFailOnUnknownValues = if (tt.isEnum) {
                failOnUnknownEnumValues || field.required
            } else {
                failOnUnknownEnumValues
            }

            read.beginControlFlow("case \$L:", field.id)
            GenerateReaderVisitor(typeResolver, read, fieldName, tt, effectiveFailOnUnknownValues).generate()
            read.endControlFlow() // end case block
            read.addStatement("break")
        }

        write.addStatement("protocol.writeFieldStop()")
        write.addStatement("protocol.writeStructEnd()")

        if (structType.fields.isNotEmpty()) {
            read.beginControlFlow("default:")
            read.addStatement("\$T.skip(protocol, field.typeId)", TypeNames.PROTO_UTIL)
            read.endControlFlow() // end default
            read.addStatement("break")
            read.endControlFlow() // end switch
        }

        read.addStatement("protocol.readFieldEnd()")
        read.endControlFlow() // end while
        read.addStatement("protocol.readStructEnd()")
        read.addStatement("return builder.build()")

        return TypeSpec.classBuilder(structType.name + "Adapter")
                .addSuperinterface(adapterSuperclass)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .addMethod(write.build())
                .addMethod(read.build())
                .addMethod(readHelper)
                .build()
    }

    private fun buildWrite(): MethodSpec {
        return MethodSpec.methodBuilder("write")
                .addAnnotation(TypeNames.OVERRIDE)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeNames.PROTOCOL, "protocol")
                .addStatement("ADAPTER.write(protocol, this)")
                .addException(TypeNames.IO_EXCEPTION)
                .build()
    }

    private fun buildEqualsFor(struct: StructType): MethodSpec {
        val equals = MethodSpec.methodBuilder("equals")
                .addAnnotation(TypeNames.OVERRIDE)
                .addModifiers(Modifier.PUBLIC)
                .returns(Boolean::class.javaPrimitiveType!!)
                .addParameter(Any::class.java, "other")
                .addStatement("if (this == other) return true")
                .addStatement("if (other == null) return false")


        if (struct.fields.isNotEmpty()) {
            equals.addStatement("if (!(other instanceof \$L)) return false", struct.name)
            equals.addStatement("$1L that = ($1L) other", struct.name)
        }

        val warningsToSuppress = mutableSetOf<String>()
        struct.fields.forEachIndexed { index, field ->
            val type = field.type.trueType
            val fieldName = fieldNamer.getName(field)

            if (index == 0) {
                equals.addCode("$[return ")
            } else {
                equals.addCode("\n&& ")
            }

            if (field.required) {
                equals.addCode("(this.$1N == that.$1N || this.$1N.equals(that.$1N))", fieldName)
            } else {
                equals.addCode("(this.$1N == that.$1N || (this.$1N != null && this.$1N.equals(that.$1N)))",
                        fieldName)
            }

            if (type.isBuiltin && (type as BuiltinType).isNumeric) {
                warningsToSuppress.add("NumberEquality")
            }

            if (type == BuiltinType.STRING) {
                warningsToSuppress.add("StringEquality")
            }
        }

        if (warningsToSuppress.isNotEmpty()) {
            equals.addAnnotation(suppressWarnings(warningsToSuppress))
        }

        if (struct.fields.isNotEmpty()) {
            equals.addCode(";\n$]")
        } else {
            equals.addStatement("return other instanceof $1L", struct.name)
        }

        return equals.build()
    }

    private fun suppressWarnings(warnings: Collection<String>): AnnotationSpec {
        val anno = AnnotationSpec.builder(TypeNames.SUPPRESS_WARNINGS)

        require(warnings.isNotEmpty()) { "No warnings present - compiler error?" }

        if (warnings.size == 1) {
            anno.addMember("value", "\$S", warnings.single())
        } else {
            val valuesText = warnings.joinToString(
                    separator = ", ",
                    prefix = "{",
                    postfix = "}") {
                "\"$it\""
            }

            anno.addMember("value", "\$L", valuesText)
        }

        return anno.build()
    }

    private fun buildHashCodeFor(struct: StructType): MethodSpec {
        val hashCode = MethodSpec.methodBuilder("hashCode")
                .addAnnotation(TypeNames.OVERRIDE)
                .addModifiers(Modifier.PUBLIC)
                .returns(Int::class.javaPrimitiveType!!)
                .addStatement("int code = 16777619")

        for (field in struct.fields) {
            val fieldName = fieldNamer.getName(field)

            if (field.required) {
                hashCode.addStatement("code ^= this.\$N.hashCode()", fieldName)
            } else {
                hashCode.addStatement("code ^= (this.$1N == null) ? 0 : this.$1N.hashCode()", fieldName)
            }
            hashCode.addStatement("code *= 0x811c9dc5")
        }

        hashCode.addStatement("return code")
        return hashCode.build()
    }

    /**
     * Builds a #toString() method for the given struct.
     *
     * The goal is to produce a method that performs as few string
     * concatenations as possible.  To do so, we identify what would be
     * consecutive constant strings (i.e. field name followed by '='),
     * collapsing them into "chunks", then using the chunks to generate
     * the actual code.
     *
     * This approach, while more complicated to implement than naive
     * StringBuilder usage, produces more-efficient and "more pleasing" code.
     * Simple structs (e.g. one with only one field, which is redacted) end up
     * with simple constants like `return "Foo{ssn=<REDACTED>}";`.
     */
    private fun buildToStringFor(struct: StructType): MethodSpec {
        /**
         * A chunk is a piece of a [CodeBlock]; it is a simple container
         * for a JavaPoet format string, along with any necessary arguments.
         */
        class Chunk(val format: String, vararg val args: Any)

        val toString = MethodSpec.methodBuilder("toString")
                .addAnnotation(TypeNames.OVERRIDE)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeNames.STRING)

        val chunks = ArrayList<Chunk>()

        val sb = StringBuilder(struct.name).append("{")
        struct.fields.forEachIndexed { index, field ->
            val fieldName = fieldNamer.getName(field)

            if (index > 0) {
                sb.append(", ")
            }

            sb.append(fieldName).append("=")

            when {
                field.isRedacted -> sb.append("<REDACTED>")
                field.isObfuscated -> {
                    chunks += Chunk("\$S", sb.toString())
                    sb.setLength(0)

                    val fieldType = field.type.trueType

                    chunks += if (fieldType.isList || fieldType.isSet) {
                        val type: String
                        val elementType: String
                        if (fieldType.isList) {
                            type = "list"
                            elementType = (fieldType as ListType).elementType.name
                        } else {
                            type = "set"
                            elementType = (fieldType as SetType).elementType.name
                        }

                        Chunk("\$T.summarizeCollection(this.\$L, \$S, \$S)",
                                TypeNames.OBFUSCATION_UTIL,
                                fieldName,
                                type,
                                elementType)
                    } else if (fieldType.isMap) {
                        val mapType = fieldType as MapType
                        val keyType = mapType.keyType.name
                        val valueType = mapType.valueType.name

                        Chunk("\$T.summarizeMap(this.\$L, \$S, \$S)",
                                TypeNames.OBFUSCATION_UTIL,
                                fieldName,
                                keyType,
                                valueType)
                    } else {
                        Chunk("\$T.hash(this.\$L)", TypeNames.OBFUSCATION_UTIL, fieldName)
                    }
                }
                else -> {
                    chunks += Chunk("\$S", sb.toString())
                    chunks += Chunk("this.\$L", fieldName)

                    sb.setLength(0)
                }
            }
        }

        sb.append("}")
        chunks += Chunk("\$S", sb.toString())

        val block = CodeBlock.builder()
        chunks.forEachIndexed { index, chunk ->
            when (index) {
                0 -> block.add("$[return ")
                else -> block.add(" + ")
            }
            block.add(chunk.format, *chunk.args)
        }
        block.add(";$]\n")

        toString.addCode(block.build())

        return toString.build()
    }

    private fun buildConst(constants: Collection<Constant>): TypeSpec {
        val builder = TypeSpec.classBuilder("Constants")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .addCode("// no instances\n")
                        .build())

        val allocator = NameAllocator()
        allocator.newName("Constants", "Constants")

        val scope = AtomicInteger(0) // used for temporaries in const collections
        val staticInit = CodeBlock.builder()
        val hasStaticInit = AtomicBoolean(false)

        for (constant in constants) {
            val type = constant.type.trueType

            var javaType = typeResolver.getJavaClass(type)

            // Primitive-typed const fields should be unboxed, but be careful -
            // while strings are builtin, they are *not* primitive!
            if (type.isBuiltin && type != BuiltinType.STRING) {
                javaType = javaType.unbox()
            }

            val field = FieldSpec.builder(javaType, constant.name)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)

            if (constant.hasJavadoc) {
                field.addJavadoc("\$L", constant.documentation + "\n\nGenerated from: " + constant.location.path + " at " + constant.location.line + ":" + constant.location.column + "\n")
            }

            if (constant.isDeprecated) {
                field.addAnnotation(AnnotationSpec.builder(TypeNames.DEPRECATED).build())
            }

            type.accept(object : SimpleVisitor<Unit>() {
                override fun visitBuiltin(builtinType: ThriftType) {
                    field.initializer(constantBuilder.renderConstValue(
                            CodeBlock.builder(), allocator, scope, type, constant.value))
                }

                override fun visitEnum(enumType: EnumType) {
                    field.initializer(constantBuilder.renderConstValue(
                            CodeBlock.builder(), allocator, scope, type, constant.value))
                }

                override fun visitList(listType: ListType) {
                    if ((constant.value as ListValueElement).value.isEmpty()) {
                        field.initializer("\$T.emptyList()", TypeNames.COLLECTIONS)
                    } else {
                        initCollection("list", "unmodifiableList")
                    }
                }

                override fun visitSet(setType: SetType) {
                    if ((constant.value as ListValueElement).value.isEmpty()) {
                        field.initializer("\$T.emptySet()", TypeNames.COLLECTIONS)
                    } else {
                        initCollection("set", "unmodifiableSet")
                    }
                }

                override fun visitMap(mapType: MapType) {
                    if ((constant.value as MapValueElement).value.isEmpty()) {
                        field.initializer("\$T.emptyMap()", TypeNames.COLLECTIONS)
                    } else {
                        initCollection("map", "unmodifiableMap")
                    }
                }

                private fun initCollection(localName: String, unmodifiableMethod: String) {
                    val tempName = localName + scope.incrementAndGet()

                    constantBuilder.generateFieldInitializer(
                            staticInit,
                            allocator,
                            scope,
                            tempName,
                            type,
                            constant.value,
                            true)
                    staticInit.addStatement("\$N = \$T.\$L(\$N)",
                            constant.name,
                            TypeNames.COLLECTIONS,
                            unmodifiableMethod,
                            tempName)

                    hasStaticInit.set(true)
                }

                override fun visitStruct(structType: StructType) {
                    throw UnsupportedOperationException("Struct-type constants are not supported")
                }

                override fun visitTypedef(typedefType: TypedefType) {
                    throw AssertionError("Typedefs should have been resolved before now")
                }

                override fun visitService(serviceType: ServiceType) {
                    throw AssertionError("Services cannot be constant values")
                }
            })

            builder.addField(field.build())
        }

        if (hasStaticInit.get()) {
            builder.addStaticBlock(staticInit.build())
        }

        return builder.build()
    }

    private fun buildEnum(type: EnumType): TypeSpec {
        val enumClassName = ClassName.get(
                type.getNamespaceFor(NamespaceScope.JAVA),
                type.name)

        val builder = TypeSpec.enumBuilder(type.name)
                .addModifiers(Modifier.PUBLIC)
                .addField(Int::class.javaPrimitiveType, "value", Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(Int::class.javaPrimitiveType, "value")
                        .addStatement("this.\$N = \$N", "value", "value")
                        .build())

        if (type.hasJavadoc) {
            builder.addJavadoc("\$L", type.documentation)
        }

        if (type.isDeprecated) {
            builder.addAnnotation(AnnotationSpec.builder(TypeNames.DEPRECATED).build())
        }

        val fromCodeMethod = MethodSpec.methodBuilder("findByValue")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(enumClassName)
                .addParameter(Int::class.javaPrimitiveType, "value")
                .beginControlFlow("switch (value)")

        if (nullabilityAnnotationType != NullabilityAnnotationType.NONE) {
            fromCodeMethod.addAnnotation(nullabilityAnnotationType.nullableClassName)
        }

        for (member in type.members) {
            val name = member.name

            val value = member.value

            val memberBuilder = TypeSpec.anonymousClassBuilder("\$L", value)
            if (member.hasJavadoc) {
                memberBuilder.addJavadoc("\$L", member.documentation)
            }

            if (member.isDeprecated) {
                memberBuilder.addAnnotation(AnnotationSpec.builder(TypeNames.DEPRECATED).build())
            }

            builder.addEnumConstant(name, memberBuilder.build())

            fromCodeMethod.addStatement("case \$L: return \$N", value, name)
        }

        fromCodeMethod
                .addStatement("default: return null")
                .endControlFlow()

        builder.addMethod(fromCodeMethod.build())

        return builder.build()
    }

    companion object {
        private const val FILE_COMMENT =
                "Automatically generated by the Thrifty compiler; do not edit!\nGenerated on: "

        private const val ADAPTER_FIELDNAME = "ADAPTER"

        private val DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT

        private fun fieldAnnotation(field: Field): AnnotationSpec {
            val spec: AnnotationSpec.Builder = AnnotationSpec.builder(TypeNames.THRIFT_FIELD)
                    .addMember("fieldId", "\$L", field.id)

            if (field.required) {
                spec.addMember("isRequired", "\$L", field.required)
            }

            if (field.optional) {
                spec.addMember("isOptional", "\$L", field.optional)
            }

            val typedef = field.typedefName
            if (typedef != null && typedef.isNotEmpty()) {
                spec.addMember("typedefName", "\$S", typedef)
            }

            return spec.build()
        }
    }
}
