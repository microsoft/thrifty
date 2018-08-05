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
package com.microsoft.thrifty.kgen

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.HashMultimap
import com.microsoft.thrifty.Adapter
import com.microsoft.thrifty.Obfuscated
import com.microsoft.thrifty.Redacted
import com.microsoft.thrifty.Struct
import com.microsoft.thrifty.StructBuilder
import com.microsoft.thrifty.TType
import com.microsoft.thrifty.ThriftException
import com.microsoft.thrifty.ThriftField
import com.microsoft.thrifty.compiler.spi.KotlinTypeProcessor
import com.microsoft.thrifty.protocol.MessageMetadata
import com.microsoft.thrifty.protocol.Protocol
import com.microsoft.thrifty.schema.BuiltinType
import com.microsoft.thrifty.schema.Constant
import com.microsoft.thrifty.schema.EnumType
import com.microsoft.thrifty.schema.FieldNamingPolicy
import com.microsoft.thrifty.schema.ListType
import com.microsoft.thrifty.schema.MapType
import com.microsoft.thrifty.schema.NamespaceScope
import com.microsoft.thrifty.schema.Schema
import com.microsoft.thrifty.schema.ServiceMethod
import com.microsoft.thrifty.schema.ServiceType
import com.microsoft.thrifty.schema.SetType
import com.microsoft.thrifty.schema.StructType
import com.microsoft.thrifty.schema.ThriftType
import com.microsoft.thrifty.schema.TypedefType
import com.microsoft.thrifty.schema.UserElement
import com.microsoft.thrifty.schema.parser.ConstValueElement
import com.microsoft.thrifty.service.AsyncClientBase
import com.microsoft.thrifty.service.MethodCall
import com.microsoft.thrifty.service.ServiceMethodCallback
import com.microsoft.thrifty.service.TMessageType
import com.microsoft.thrifty.util.ObfuscationUtil
import com.microsoft.thrifty.util.ProtocolUtil
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.jvm.jvmField
import com.squareup.kotlinpoet.jvm.jvmStatic
import kotlin.coroutines.experimental.buildSequence
import okio.ByteString
import java.io.IOException

private object Tags {
    val ADAPTER = "RESERVED:ADAPTER"
    val MESSAGE = "RESERVED:message"
    val CAUSE = "RESERVED:cause"
    val FIND_BY_VALUE = "RESERVED:findByValue"
    val VALUE = "RESERVED:value"
    val CALLBACK = "RESERVED:callback"
    val SEND = "RESERVED:send"
    val RECEIVE = "RESERVED:receive"
    val RESULT = "RESERVED:resultValue"
    val FIELD = "RESERVED:fieldMeta"
}

/**
 * Generates Kotlin code from a [Schema].
 *
 * While substantially complete, there is a bit more yet to be implemented:
 * - Services (coroutine-based?)
 * - Builderless adapters (builders are dumb, given data classes)
 * - Customizable collection types?  Some droids prefer ArrayMap, ArraySet, etc
 * - Option to emit one file per type
 *
 * @param fieldNamingPolicy A user-specified naming policy for fields.
 */
class KotlinCodeGenerator(
        fieldNamingPolicy: FieldNamingPolicy = FieldNamingPolicy.DEFAULT
) {
    enum class OutputStyle {
        FILE_PER_NAMESPACE,
        FILE_PER_TYPE
    }

    // TODO: Add a compiler flag to omit struct generation
    private var shouldImplementStruct: Boolean = true

    private val nameAllocators = CacheBuilder
            .newBuilder()
            .build(object : CacheLoader<UserElement, NameAllocator>() {
        override fun load(key: UserElement?): NameAllocator {
            val elem = requireNotNull(key) { "Can't get a name allocator for null" }
            return NameAllocator().apply {

                when (elem) {
                    is StructType -> {
                        newName("ADAPTER", Tags.ADAPTER)
                        if (elem.isException) {
                            newName("message", Tags.MESSAGE)
                            newName("cause", Tags.CAUSE)
                        }

                        for (field in elem.fields) {
                            val conformingName = fieldNamingPolicy.apply(field.name)
                            newName(conformingName, field)
                        }
                    }

                    is EnumType -> {
                        newName("findByValue", Tags.FIND_BY_VALUE)
                        newName("value", Tags.VALUE)
                        for (member in elem.members) {
                            newName(member.name, member)
                        }
                    }

                    is ServiceType -> {
                        for (method in elem.methods) {
                            newName(method.name, method)
                        }
                    }

                    is ServiceMethod -> {
                        newName("callback", Tags.CALLBACK)
                        newName("send", Tags.SEND)
                        newName("receive", Tags.RECEIVE)
                        newName("resultValue", Tags.RESULT)
                        newName("fieldMeta", Tags.FIELD)

                        for (param in elem.parameters) {
                            newName(param.name, param)
                        }

                        for (ex in elem.exceptions) {
                            newName(ex.name, ex)
                        }
                    }

                    else -> error("Unexpected UserElement: $key")
                }
            }
        }
    })

    var processor: KotlinTypeProcessor = NoTypeProcessor
    var outputStyle: OutputStyle = OutputStyle.FILE_PER_NAMESPACE

    fun filePerNamespace(): KotlinCodeGenerator = apply { outputStyle = OutputStyle.FILE_PER_NAMESPACE }
    fun filePerType(): KotlinCodeGenerator = apply { outputStyle = OutputStyle.FILE_PER_TYPE }

    private object NoTypeProcessor : KotlinTypeProcessor {
        override fun process(typeSpec: TypeSpec) = typeSpec
    }

    fun generate(schema: Schema): List<FileSpec> {
        TypeSpec.classBuilder("foo")
                .addModifiers(KModifier.DATA)

        val specsByNamespace = HashMultimap.create<String, TypeSpec>()
        val constantsByNamespace = HashMultimap.create<String, PropertySpec>()

        schema.enums.forEach { specsByNamespace.put(it.kotlinNamespace, generateEnumClass(it)) }
        schema.structs.forEach { specsByNamespace.put(it.kotlinNamespace, generateDataClass(schema, it)) }
        schema.unions.forEach { specsByNamespace.put(it.kotlinNamespace, generateDataClass(schema, it)) }
        schema.exceptions.forEach { specsByNamespace.put(it.kotlinNamespace, generateDataClass(schema, it)) }

        val constantNameAllocators = mutableMapOf<String, NameAllocator>()
        schema.constants.forEach {
            val ns = it.kotlinNamespace
            val allocator = constantNameAllocators.getOrPut(ns) { NameAllocator() }
            val property = generateConstantProperty(schema, allocator, it)
            constantsByNamespace.put(ns, property)
        }

        schema.services.forEach {
            val iface = generateServiceInterface(it)
            val impl = generateServiceImplementation(schema, it, iface)
            specsByNamespace.put(it.kotlinNamespace, iface)
            specsByNamespace.put(it.kotlinNamespace, impl)
        }

        return when (outputStyle) {
            OutputStyle.FILE_PER_NAMESPACE -> {
                val namespaces = mutableSetOf<String>().apply {
                    addAll(specsByNamespace.keys())
                    addAll(constantsByNamespace.keys())
                }

                val fileSpecsByNamespace = namespaces
                        .map { it to FileSpec.builder(it,"ThriftTypes") }
                        .toMap()

                fileSpecsByNamespace.map { (ns, fileSpec) ->
                    constantsByNamespace[ns]?.forEach { fileSpec.addProperty(it) }
                    specsByNamespace[ns]
                            ?.mapNotNull { processor.process(it) }
                            ?.forEach { fileSpec.addType(it) }
                    fileSpec.build()
                }
            }

            OutputStyle.FILE_PER_TYPE -> {
                buildSequence {

                    val types = specsByNamespace.entries().asSequence()
                    for ((ns, type) in types) {
                        val processedType = processor.process(type) ?: continue
                        val name = processedType.name ?: throw AssertionError("Top-level TypeSpecs must have names")
                        val spec = FileSpec.builder(ns, name)
                                .addType(processedType)
                                .build()
                        yield(spec)
                    }

                    for ((ns, props) in constantsByNamespace.asMap().entries) {
                        val spec = FileSpec.builder(ns, "Constants")
                        props.forEach { spec.addProperty(it) }
                        yield(spec.build())
                    }

                }.toList()
            }
        }
    }

    // region Enums

    fun generateEnumClass(enumType: EnumType): TypeSpec {
        val typeBuilder = TypeSpec.enumBuilder(enumType.name)
                .addProperty(PropertySpec.builder("value", INT)
                        .jvmField()
                        .initializer("value")
                        .build())
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("value", INT)
                        .build())

        if (enumType.isDeprecated) typeBuilder.addAnnotation(makeDeprecated())
        if (enumType.hasJavadoc) typeBuilder.addKdoc("%L", enumType.documentation)

        val findByValue = FunSpec.builder("findByValue")
                .addParameter("value", INT)
                .returns(enumType.typeName.asNullable())
                .jvmStatic()
                .beginControlFlow("return when (value)")

        val nameAllocator = nameAllocators[enumType]
        for (member in enumType.members) {
            val enumMemberSpec= TypeSpec.anonymousClassBuilder()
                    .addSuperclassConstructorParameter("%L", member.value)

            if (member.isDeprecated) enumMemberSpec.addAnnotation(makeDeprecated())
            if (member.hasJavadoc) enumMemberSpec.addKdoc("%L", member.documentation)

            val name = nameAllocator.get(member)
            typeBuilder.addEnumConstant(name, enumMemberSpec.build())
            findByValue.addStatement("%L -> %L", member.value, name)
        }


        val companion = TypeSpec.companionObjectBuilder()
                .addFunction(findByValue
                        .addStatement("else -> null")
                        .endControlFlow()
                        .build())
                .build()

        return typeBuilder
                .addType(companion)
                .build()
    }

    // endregion Enums

    // region Structs

    fun generateDataClass(schema: Schema, struct: StructType): TypeSpec {
        val structClassName = ClassName(struct.kotlinNamespace, struct.name)
        val typeBuilder = TypeSpec.classBuilder(structClassName).apply {
            if (struct.fields.isNotEmpty()) {
                addModifiers(KModifier.DATA)
            }
        }

        if (struct.isDeprecated) typeBuilder.addAnnotation(makeDeprecated())
        if (struct.hasJavadoc) typeBuilder.addKdoc("%L", struct.documentation)
        if (struct.isException) typeBuilder.superclass(Exception::class)

        val ctorBuilder = FunSpec.constructorBuilder()

        val companionBuilder = TypeSpec.companionObjectBuilder()

        val nameAllocator = nameAllocators[struct]
        for (field in struct.fields) {
            val fieldName = nameAllocator.get(field)
            val typeName = field.type.typeName.let {
                if (!field.required) it.asNullable() else it
            }

            val thriftField = AnnotationSpec.builder(ThriftField::class).let { anno ->
                anno.addMember("fieldId = ${field.id}")
                if (field.required) anno.addMember("isRequired = true")
                if (field.optional) anno.addMember("isOptional = true")

                field.typedefName?.let { anno.addMember("typedefName = %S", it) }

                anno.build()
            }

            val param = ParameterSpec.builder(fieldName, typeName)

            field.defaultValue?.let {
                param.defaultValue(renderConstValue(schema, field.type.trueType, it))
            }

            val prop = PropertySpec.builder(fieldName, typeName)
                    .initializer(fieldName)
                    .jvmField()
                    .addAnnotation(thriftField)

            if (field.isObfuscated) prop.addAnnotation(Obfuscated::class)
            if (field.isRedacted) prop.addAnnotation(Redacted::class)

            ctorBuilder.addParameter(param.build())
            typeBuilder.addProperty(prop.build())
        }

        if (true) { // TODO: Add an option to generate Java-style builders

            val builderTypeName = ClassName(struct.kotlinNamespace, struct.name, "Builder")
            val adapterTypeName = ClassName(struct.kotlinNamespace, struct.name, "${struct.name}Adapter")
            val adapterInterfaceTypeName = Adapter::class.asTypeName().parameterizedBy(
                    struct.typeName, builderTypeName)

            typeBuilder.addType(generateBuilderFor(schema, struct))
            typeBuilder.addType(generateAdapterFor(struct, adapterTypeName, adapterInterfaceTypeName, builderTypeName))

            companionBuilder.addProperty(PropertySpec.builder("ADAPTER", adapterInterfaceTypeName)
                    .initializer("%T()", adapterTypeName)
                    .jvmField()
                    .build())
        } else {
            TODO("Builderless adapters")
        }

        if (struct.fields.any { it.isObfuscated || it.isRedacted } || struct.fields.isEmpty()) {
            typeBuilder.addFunction(generateToString(struct))
        }

        if (struct.fields.isEmpty()) {
            typeBuilder.addFunction(FunSpec.builder("hashCode")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(INT)
                    .addStatement("return javaClass.hashCode()")
                    .build())

            typeBuilder.addFunction(FunSpec.builder("equals")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("other", Any::class.asTypeName().asNullable())
                    .returns(BOOLEAN)
                    .addStatement("return other is %T", structClassName)
                    .build())
        }

        if (shouldImplementStruct) {
            typeBuilder
                    .addSuperinterface(Struct::class)
                    .addFunction(FunSpec.builder("write")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter("protocol", Protocol::class)
                            .addStatement("%L.write(protocol, this)", nameAllocator.get(Tags.ADAPTER))
                            .build())
        }

        return typeBuilder
                .primaryConstructor(ctorBuilder.build())
                .addType(companionBuilder.build())
                .build()
    }

    // endregion Structs

    // region Redaction/obfuscation

    fun generateToString(struct: StructType): FunSpec {

        val block = buildCodeBlock {
            add("return \"${struct.name}(")

            val nameAllocator = nameAllocators[struct]
            for ((ix, field) in struct.fields.withIndex()) {
                if (ix != 0) {
                    add(", ")
                }
                val fieldName = nameAllocator.get(field)
                add("$fieldName=")

                when {
                    field.isRedacted -> add("<REDACTED>")
                    field.isObfuscated -> {
                        val type = field.type.trueType
                        when (type) {
                            is ListType -> {
                                val elementName = type.elementType.trueType.name
                                add("\${%T.summarizeCollection($fieldName, %S, %S)}",
                                        ObfuscationUtil::class,
                                        "list",
                                        elementName)
                            }
                            is SetType -> {
                                val elementName = type.elementType.trueType.name
                                add("\${%T.summarizeCollection($fieldName, %S, %S)}",
                                        ObfuscationUtil::class,
                                        "set",
                                        elementName)
                            }
                            is MapType -> {
                                val keyName = type.keyType.trueType.name
                                val valName = type.valueType.trueType.name
                                add("\${%T.summarizeMap($fieldName, %S, %S)}",
                                        ObfuscationUtil::class,
                                        keyName,
                                        valName)
                            }
                            else -> {
                                add("\${%T.hash($fieldName)}", ObfuscationUtil::class)
                            }
                        }
                    }
                    else -> add("\$$fieldName")
                }
            }

            add(")\"")
        }

        return FunSpec.builder("toString")
                .addModifiers(KModifier.OVERRIDE)
                .addCode(block)
                .build()
    }

    // endregion Redaction/obfuscation

    // region Builders

    fun generateBuilderFor(schema: Schema, struct: StructType): TypeSpec {
        val structTypeName = ClassName(struct.kotlinNamespace, struct.name)
        val spec = TypeSpec.classBuilder("Builder")
                .addSuperinterface(StructBuilder::class.asTypeName().parameterizedBy(structTypeName))
        val buildFunSpec = FunSpec.builder("build")
                .addModifiers(KModifier.OVERRIDE)
                .returns(structTypeName)
                .addCode("%[return ${struct.name}(")

        val resetFunSpec = FunSpec.builder("reset")
                .addModifiers(KModifier.OVERRIDE)

        val copyCtor = FunSpec.constructorBuilder()
                .addParameter("source", structTypeName)

        val defaultCtor = FunSpec.constructorBuilder()

        val nameAllocator = nameAllocators[struct]
        val buildParamStringBuilder = StringBuilder()
        for (field in struct.fields) {
            val name = nameAllocator.get(field)
            val type = field.type.typeName

            // Add a private var

            val defaultValueBlock = field.defaultValue?.let {
                renderConstValue(schema, field.type.trueType, it)
            } ?: CodeBlock.of("null")

            val propertySpec = PropertySpec.varBuilder(name, type.asNullable(), KModifier.PRIVATE)
                    .initializer(defaultValueBlock)

            // Add a builder fun
            val buildFunParamType = if (!field.required) type.asNullable() else type
            val builderFunSpec = FunSpec.builder(name)
                    .addParameter(name, buildFunParamType)
                    .addStatement("return apply { this.$name = $name }")

            // Add initialization in default ctor
            defaultCtor.addStatement("this.$name = %L", defaultValueBlock)

            // Add initialization in copy ctor
            copyCtor.addStatement("this.$name = source.$name")

            // reset field

            resetFunSpec.addStatement("this.$name = %L", defaultValueBlock)

            // Add field to build-method ctor-invocation arg builder
            // TODO: Add newlines and indents if numFields > 1
            buildParamStringBuilder.append("$name = ")
            if (field.required) {
                buildParamStringBuilder.append("checkNotNull($name) { \"Required field '$name' is missing\" }")
            } else {
                buildParamStringBuilder.append("this.$name")
            }

            buildParamStringBuilder.append(",%W")

            // Finish off the property and builder fun
            spec.addProperty(propertySpec.build())
            spec.addFunction(builderFunSpec.build())
        }

        // If there are any fields, the string builder will have a trailing comma-separator.
        // We need to trim that off.
        if (struct.fields.isNotEmpty()) {
            buildParamStringBuilder.setLength(buildParamStringBuilder.length - 3)
        }

        buildFunSpec
                .addCode(buildParamStringBuilder.toString())
                .addCode(")%]")

        return spec
                .addFunction(defaultCtor.build())
                .addFunction(copyCtor.build())
                .addFunction(buildFunSpec.build())
                .addFunction(resetFunSpec.build())
                .build()
    }

    // endregion Builders

    // region Adapters

    fun generateAdapterFor(
            struct: StructType,
            adapterName: ClassName,
            adapterInterfaceName: TypeName,
            builderType: ClassName): TypeSpec {
        val adapter = TypeSpec.classBuilder(adapterName)
                .addModifiers(KModifier.PRIVATE)
                .addSuperinterface(adapterInterfaceName)

        val reader = FunSpec.builder("read")
                .addModifiers(KModifier.OVERRIDE)
                .returns(struct.typeName)
                .addParameter("protocol", Protocol::class)
                .addParameter("builder", builderType)

        val writer = FunSpec.builder("write")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("protocol", Protocol::class)
                .addParameter("struct", struct.typeName)

        // Writer first, b/c it is easier

        val nameAllocator = nameAllocators[struct]

        writer.addStatement("protocol.writeStructBegin(%S)", struct.name)
        for (field in struct.fields) {
            val name = nameAllocator.get(field)
            val fieldType = field.type.trueType

            if (!field.required) {
                writer.beginControlFlow("if (struct.$name != null)")
            }

            writer.addStatement("protocol.writeFieldBegin(%S, %L, %T.%L)",
                    field.name,
                    field.id,
                    TType::class,
                    fieldType.typeCodeName)

            generateWriteCall(writer, "struct.$name", fieldType)

            writer.addStatement("protocol.writeFieldEnd()")

            if (!field.required) {
                writer.endControlFlow()
            }
        }
        writer.addStatement("protocol.writeFieldStop()")
        writer.addStatement("protocol.writeStructEnd()")

        // Reader next

        reader.addStatement("protocol.readStructBegin()")
        reader.beginControlFlow("while (true)")

        reader.addStatement("val fieldMeta = protocol.readFieldBegin()")

        reader.beginControlFlow("if (fieldMeta.typeId == %T.STOP)", TType::class)
        reader.addStatement("break")
        reader.endControlFlow()


        if (struct.fields.isNotEmpty()) {
            reader.beginControlFlow("when (fieldMeta.fieldId.toInt())")

            for (field in struct.fields) {
                val name = nameAllocator.get(field)
                val fieldType = field.type.trueType

                reader.addCode {
                    addStatement("${field.id} -> {%>")
                    beginControlFlow("if (fieldMeta.typeId == %T.%L)", TType::class, fieldType.typeCodeName)

                    generateRecursiveReadCall(this, name, fieldType)
                    addStatement("builder.$name($name)")

                    nextControlFlow("else")
                    addStatement("%T.skip(protocol, fieldMeta.typeId)", ProtocolUtil::class)
                    endControlFlow()
                    addStatement("%<}")
                }
            }

            reader.addStatement("else -> %T.skip(protocol, fieldMeta.typeId)", ProtocolUtil::class)
            reader.endControlFlow() // when (fieldMeta.fieldId.toInt())
        } else {
            reader.addStatement("%T.skip(protocol, fieldMeta.typeId)", ProtocolUtil::class)
        }

        reader.addStatement("protocol.readFieldEnd()")
        reader.endControlFlow() // while (true)
        reader.addStatement("protocol.readStructEnd()")
        reader.addStatement("return builder.build()")

        return adapter
                .addFunction(writer.build())
                .addFunction(reader.build())
                .addFunction(FunSpec.builder("read")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("protocol", Protocol::class)
                        .addStatement("return read(protocol, %T())", builderType)
                        .build())
                .build()
    }

    private fun generateWriteCall(writer: FunSpec.Builder, name: String, type: ThriftType) {

        // Assumptions:
        // - writer has a parameter "protocol" that is a Protocol

        fun generateRecursiveWrite(source: String, type: ThriftType, scope: Int) {
            type.accept(object : ThriftType.Visitor<Unit> {
                override fun visitVoid(voidType: BuiltinType) {
                    error("Cannot write void, wat r u doing")
                }

                override fun visitBool(boolType: BuiltinType) {
                    writer.addStatement("%N.writeBool(%N)", "protocol", source)
                }

                override fun visitByte(byteType: BuiltinType) {
                    writer.addStatement("%N.writeByte(%N)", "protocol", source)
                }

                override fun visitI16(i16Type: BuiltinType) {
                    writer.addStatement("%N.writeI16(%N)", "protocol", source)
                }

                override fun visitI32(i32Type: BuiltinType) {
                    writer.addStatement("%N.writeI32(%N)", "protocol", source)
                }

                override fun visitI64(i64Type: BuiltinType) {
                    writer.addStatement("%N.writeI64(%N)", "protocol", source)
                }

                override fun visitDouble(doubleType: BuiltinType) {
                    writer.addStatement("%N.writeDouble(%N)", "protocol", source)
                }

                override fun visitString(stringType: BuiltinType) {
                    writer.addStatement("%N.writeString(%N)", "protocol", source)
                }

                override fun visitBinary(binaryType: BuiltinType) {
                    writer.addStatement("%N.writeBinary(%N)", "protocol", source)
                }

                override fun visitEnum(enumType: EnumType) {
                    writer.addStatement("%N.writeI32(%N.value)", "protocol", source)
                }

                override fun visitList(listType: ListType) {
                    val elementType = listType.elementType.trueType
                    writer.addStatement(
                            "%N.writeListBegin(%T.%L, %L.size)",
                            "protocol",
                            TType::class,
                            elementType.typeCodeName,
                            source)

                    val iterator = "item$scope"
                    writer.beginControlFlow("for ($iterator in %N)", source)

                    generateRecursiveWrite(iterator, elementType, scope + 1)

                    writer.endControlFlow()

                    writer.addStatement("%N.writeListEnd()", "protocol")
                }

                override fun visitSet(setType: SetType) {
                    val elementType = setType.elementType.trueType
                    writer.addStatement(
                            "%N.writeSetBegin(%T.%L, %L.size)",
                            "protocol",
                            TType::class,
                            elementType.typeCodeName,
                            source)

                    val iterator = "item$scope"
                    writer.beginControlFlow("for ($iterator in %N)", source)

                    generateRecursiveWrite(iterator, elementType, scope + 1)

                    writer.endControlFlow()

                    writer.addStatement("%N.writeSetEnd()", "protocol")
                }

                override fun visitMap(mapType: MapType) {
                    val keyType = mapType.keyType.trueType
                    val valType = mapType.valueType.trueType

                    writer.addStatement(
                            "%1N.writeMapBegin(%2T.%3L, %2T.%4L, %5L.size)",
                            "protocol",
                            TType::class,
                            keyType.typeCodeName,
                            valType.typeCodeName,
                            source)

                    val keyIter = "key$scope"
                    val valIter = "val$scope"
                    writer.beginControlFlow("for (($keyIter, $valIter) in %N)", source)

                    generateRecursiveWrite(keyIter, keyType, scope + 1)
                    generateRecursiveWrite(valIter, valType, scope + 1)

                    writer.endControlFlow()

                    writer.addStatement("%N.writeMapEnd()", "protocol")
                }

                override fun visitStruct(structType: StructType) {
                    writer.addStatement("%T.ADAPTER.write(%N, %N)", structType.typeName, "protocol", source)
                }

                override fun visitTypedef(typedefType: TypedefType) {
                    typedefType.trueType.accept(this)
                }

                override fun visitService(serviceType: ServiceType) {
                    error("Cannot write a service, wat r u doing")
                }
            })
        }

        generateRecursiveWrite(name, type, 0)
    }

    private fun generateRecursiveReadCall(block: CodeBlock.Builder, name: String, type: ThriftType, scope: Int = 0): CodeBlock.Builder {
        type.accept(object : ThriftType.Visitor<Unit> {
            override fun visitVoid(voidType: BuiltinType) {
                error("Cannot read a void, wat r u doing")
            }

            override fun visitBool(boolType: BuiltinType) {
                block.addStatement("val $name = protocol.readBool()")
            }

            override fun visitByte(byteType: BuiltinType) {
                block.addStatement("val $name = protocol.readByte()")
            }

            override fun visitI16(i16Type: BuiltinType) {
                block.addStatement("val $name = protocol.readI16()")
            }

            override fun visitI32(i32Type: BuiltinType) {
                block.addStatement("val $name = protocol.readI32()")
            }

            override fun visitI64(i64Type: BuiltinType) {
                block.addStatement("val $name = protocol.readI64()")
            }

            override fun visitDouble(doubleType: BuiltinType) {
                block.addStatement("val $name = protocol.readDouble()")
            }

            override fun visitString(stringType: BuiltinType) {
                block.addStatement("val $name = protocol.readString()")
            }

            override fun visitBinary(binaryType: BuiltinType) {
                block.addStatement("val $name = protocol.readBinary()")
            }

            override fun visitEnum(enumType: EnumType) {
                val codeName = "code$scope"
                block.addStatement("val $codeName = protocol.readI32()")
                block.addStatement("val $name = %T.findByValue($codeName)", enumType.typeName)
                block.beginControlFlow("if ($name == null)")
                block.addStatement("throw %T(%T.PROTOCOL_ERROR, \"Unexpected value for enum type %T: \$$codeName\")",
                        ThriftException::class,
                        ThriftException.Kind::class,
                        enumType.typeName)
                block.endControlFlow()
            }

            override fun visitList(listType: ListType) {
                val elementType = listType.elementType.trueType
                val listImplType = ArrayList::class.asTypeName().parameterizedBy(elementType.typeName)
                val listMeta = "list$scope"
                block.addStatement("val $listMeta = protocol.readListBegin()")
                block.addStatement("val $name = %T($listMeta.size)", listImplType)

                block.beginControlFlow("for (i$scope in 0 until $listMeta.size)")
                generateRecursiveReadCall(block, "item$scope", elementType, scope + 1)
                block.addStatement("$name += item$scope")
                block.endControlFlow()

                block.addStatement("protocol.readListEnd()")
            }

            override fun visitSet(setType: SetType) {
                val elementType = setType.elementType.trueType
                val setImplType = HashSet::class.asTypeName().parameterizedBy(elementType.typeName)
                val setMeta = "set$scope"

                block.addStatement("val $setMeta = protocol.readSetBegin()")
                block.addStatement("val $name = %T($setMeta.size)", setImplType)

                block.beginControlFlow("for (i$scope in 0 until $setMeta.size)")
                generateRecursiveReadCall(block, "item$scope", elementType, scope + 1)
                block.addStatement("$name += item$scope")
                block.endControlFlow()

                block.addStatement("protocol.readSetEnd()")
            }

            override fun visitMap(mapType: MapType) {
                val keyType = mapType.keyType.trueType
                val valType = mapType.valueType.trueType
                val mapImplType = HashMap::class.asTypeName().parameterizedBy(keyType.typeName, valType.typeName)
                val mapMeta = "map$scope"

                block.addStatement("val $mapMeta = protocol.readMapBegin()")
                block.addStatement("val $name = %T($mapMeta.size)", mapImplType)

                block.beginControlFlow("for (i$scope in 0 until $mapMeta.size)")

                val keyName = "key$scope"
                val valName = "val$scope"

                generateRecursiveReadCall(block, keyName, keyType, scope + 1)
                generateRecursiveReadCall(block, valName, valType, scope + 1)

                block.addStatement("$name[$keyName] = $valName")
                block.endControlFlow()

                block.addStatement("protocol.readMapEnd()")
            }

            override fun visitStruct(structType: StructType) {
                block.addStatement("val $name = %T.ADAPTER.read(protocol)", structType.typeName)
            }

            override fun visitTypedef(typedefType: TypedefType) {
                typedefType.trueType.accept(this)
            }

            override fun visitService(serviceType: ServiceType) {
                error("cannot read a service, wat r u doing")
            }
        })
        return block
    }

    //endregion Adapters

    //region Constants

    fun generateConstantProperty(schema: Schema, allocator: NameAllocator, constant: Constant): PropertySpec {
        val type = constant.type.trueType
        val typeName = type.typeName
        val propName = allocator.newName(constant.name, constant)
        val propBuilder = PropertySpec.builder(propName, typeName)

        if (constant.isDeprecated) propBuilder.addAnnotation(makeDeprecated())
        if (constant.hasJavadoc) propBuilder.addKdoc("%L", constant.documentation)

        val canBeConst = type.accept(object : ThriftType.Visitor<Boolean> {
            // JVM primitives and strings can be constants
            override fun visitBool(boolType: BuiltinType) = true

            override fun visitByte(byteType: BuiltinType) = true
            override fun visitI16(i16Type: BuiltinType) = true
            override fun visitI32(i32Type: BuiltinType) = true
            override fun visitI64(i64Type: BuiltinType) = true
            override fun visitDouble(doubleType: BuiltinType) = true
            override fun visitString(stringType: BuiltinType) = true

            // Everything else, cannot...
            override fun visitBinary(binaryType: BuiltinType) = false

            override fun visitEnum(enumType: EnumType) = false
            override fun visitList(listType: ListType) = false
            override fun visitSet(setType: SetType) = false
            override fun visitMap(mapType: MapType) = false
            override fun visitStruct(structType: StructType) = false

            // ...except, possibly, a typedef
            override fun visitTypedef(typedefType: TypedefType) = typedefType.trueType.accept(this)

            // These make no sense
            override fun visitService(serviceType: ServiceType): Boolean {
                error("Cannot have a const value of a service type")
            }

            override fun visitVoid(voidType: BuiltinType): Boolean {
                error("Cannot have a const value of void")
            }
        })

        if (canBeConst) {
            propBuilder.addModifiers(KModifier.CONST)
        }

        propBuilder.initializer(renderConstValue(schema, constant.type, constant.value))

        return propBuilder.build()
    }

    fun renderConstValue(schema: Schema, thriftType: ThriftType, valueElement: ConstValueElement): CodeBlock {
        fun recursivelyRenderConstValue(block: CodeBlock.Builder, type: ThriftType, value: ConstValueElement) {
            type.accept(object : ThriftType.Visitor<Unit> {
                override fun visitVoid(voidType: BuiltinType) {
                    error("Can't have void as a constant")
                }

                override fun visitBool(boolType: BuiltinType) {
                    if (value.isIdentifier && value.getAsString() in listOf("true", "false")) {
                        block.add("%L", value.getAsString() == "true")
                    } else if (value.isInt) {
                        block.add("%L", value.getAsInt() != 0)
                    } else {
                        constOrError("Invalid boolean constant")
                    }
                }

                override fun visitByte(byteType: BuiltinType) {
                    if (value.isInt) {
                        block.add("%L", value.getAsInt())
                    } else {
                        constOrError("Invalid byte constant")
                    }
                }

                override fun visitI16(i16Type: BuiltinType) {
                    if (value.isInt) {
                        block.add("%L", value.getAsInt())
                    } else {
                        constOrError("Invalid I16 constant")
                    }
                }

                override fun visitI32(i32Type: BuiltinType) {
                    if (value.isInt) {
                        block.add("%L", value.getAsInt())
                    } else {
                        constOrError("Invalid I32 constant")
                    }
                }

                override fun visitI64(i64Type: BuiltinType) {
                    if (value.isInt) {
                        block.add("%L", value.getAsInt())
                    } else {
                        constOrError("Invalid I64 constant")
                    }
                }

                override fun visitDouble(doubleType: BuiltinType) {
                    if (value.isDouble || value.isInt) {
                        block.add("%L", value.getAsDouble())
                    } else {
                        constOrError("Invalid double constant")
                    }
                }

                override fun visitString(stringType: BuiltinType) {
                    if (value.isString) {
                        block.add("%S", value.getAsString())
                    } else {
                        constOrError("Invalid string constant")
                    }
                }

                override fun visitBinary(binaryType: BuiltinType) {
                    // TODO: Implement support for binary constants in the ANTLR grammar
                    if (value.isString) {
                        block.add("%T.decodeHex(%S)", ByteString::class, value.getAsString())
                    } else {
                        constOrError("Invalid binary constant")
                    }
                }

                override fun visitEnum(enumType: EnumType) {
                    val member = try {
                        when {
                            // Enum references may or may not be scoped with their typename; either way, we must remove
                            // the type reference to get the member name on its own.
                            value.isIdentifier -> enumType.findMemberByName(value.getAsString().split(".").last())
                            value.isInt -> enumType.findMemberById(value.getAsInt())
                            else -> throw AssertionError("Value kind ${value.kind} is not possibly an enum")
                        }
                    } catch (e: NoSuchElementException) {
                        null
                    }

                    if (member != null) {
                        block.add("${enumType.typeName}.%L", member.name)
                    } else {
                        constOrError("Invalid enum constant")
                    }
                }

                override fun visitList(listType: ListType) {
                    visitCollection(listType.elementType.trueType, "listOf", "Invalid list constant")

                }

                override fun visitSet(setType: SetType) {
                    visitCollection(
                            setType.elementType.trueType,
                            "setOf",
                            "Invalid set constant")
                }

                private fun visitCollection(elementType: ThriftType, factoryMethod: String, errorMessage: String) {
                    if (value.isList) {
                        block.add("$factoryMethod(%>")

                        var first = true
                        for (elementValue in value.getAsList()) {
                            if (first) {
                                first = false
                            } else {
                                block.add(",%W")
                            }
                            recursivelyRenderConstValue(block, elementType, elementValue)
                        }

                        block.add("%<)")
                    } else {
                        constOrError(errorMessage)
                    }
                }

                override fun visitMap(mapType: MapType) {
                    val keyType = mapType.keyType.trueType
                    val valueType = mapType.valueType.trueType
                    if (value.isMap) {
                        block.add("mapOf(%>")

                        var first = true
                        for ((k, v) in value.getAsMap()) {
                            if (first) {
                                first = false
                            } else {
                                block.add(",%W")
                            }
                            recursivelyRenderConstValue(block, keyType, k)
                            block.add(" to ")
                            recursivelyRenderConstValue(block, valueType, v)
                        }

                        block.add("%<)")
                    } else {
                        constOrError("Invalid map constant")
                    }
                }

                override fun visitStruct(structType: StructType) {
                    TODO("not implemented")
                }

                override fun visitTypedef(typedefType: TypedefType) {
                    typedefType.trueType.accept(this)
                }

                override fun visitService(serviceType: ServiceType) {
                    throw AssertionError("Cannot have a const value of a service type, wat r u doing")
                }

                private fun constOrError(error: String) {
                    val message = "$error: ${value.value} at ${value.location}"
                    require(value.isIdentifier) { message }

                    val name: String
                    val expectedProgram: String?

                    val text = value.getAsString()
                    val ix = text.indexOf(".")
                    if (ix != -1) {
                        expectedProgram = text.substring(0, ix)
                        name = text.substring(ix + 1)
                    } else {
                        expectedProgram = null
                        name = text
                    }

                    val c = schema.constants.asSequence()
                            .firstOrNull {
                                it.name == name
                                        && it.type.trueType == type
                                        && (expectedProgram == null || expectedProgram == it.location.programName)
                            } ?: throw IllegalStateException(message)

                    val packageName = c.getNamespaceFor(NamespaceScope.KOTLIN, NamespaceScope.JAVA, NamespaceScope.ALL)
                            ?: throw IllegalStateException("No JVM namespace found for ${c.name} at ${c.location}")

                    block.add("$packageName.$name")
                }
            })
        }

        return buildCodeBlock {
            recursivelyRenderConstValue(this, thriftType.trueType, valueElement)
        }
    }

    //endregion Constants

    // region Services

    fun generateServiceInterface(serviceType: ServiceType): TypeSpec {
        val type = TypeSpec.interfaceBuilder(serviceType.name).apply {
            if (serviceType.hasJavadoc) addKdoc("%L", serviceType.documentation)
            if (serviceType.isDeprecated) addAnnotation(makeDeprecated())

            serviceType.extendsService?.let { baseType ->
                addSuperinterface(baseType.typeName)
            }
        }

        val allocator = nameAllocators[serviceType]
        for (method in serviceType.methods) {
            val name = allocator.get(method)
            val funSpec = FunSpec.builder(name).apply {
                addModifiers(KModifier.ABSTRACT)

                if (method.hasJavadoc) addKdoc("%L", method.documentation)
                if (method.isDeprecated) addAnnotation(makeDeprecated())
            }

            val methodNameAllocator = nameAllocators[method]
            for (param in method.parameters) {
                val paramName = methodNameAllocator.get(param)
                val paramSpec = ParameterSpec.builder(paramName, param.type.typeName).apply {
                    if (param.isDeprecated) addAnnotation(makeDeprecated())
                }
                funSpec.addParameter(paramSpec.build())
            }

            val callbackName = methodNameAllocator.get(Tags.CALLBACK)
            val callbackResultType = if (method.returnType.trueType == BuiltinType.VOID) {
                UNIT
            } else {
                method.returnType.typeName
            }
            val callbackType = ServiceMethodCallback::class
                    .asTypeName()
                    .parameterizedBy(callbackResultType)

            funSpec.addParameter(callbackName, callbackType)

            type.addFunction(funSpec.build())
        }

        return type.build()
    }

    fun generateServiceImplementation(schema: Schema, serviceType: ServiceType, serviceInterface: TypeSpec): TypeSpec {
        val type = TypeSpec.classBuilder(serviceType.name + "Client").apply {
            val baseType = serviceType.extendsService as? ServiceType
            val baseClassName = if (baseType != null) {
                ClassName(baseType.kotlinNamespace, baseType.name + "Client")
            } else {
                AsyncClientBase::class.asClassName()
            }

            superclass(baseClassName)
            addSuperinterface(ClassName(serviceType.kotlinNamespace, serviceType.name))

            // If any servces extend this, then this needs to be open.
            if (schema.services.any { it.extendsService == serviceType }) {
                addModifiers(KModifier.OPEN)
            }

            primaryConstructor(FunSpec.constructorBuilder()
                    .addParameter("protocol", Protocol::class)
                    .addParameter("listener", AsyncClientBase.Listener::class)
                    .build())

            addSuperclassConstructorParameter("protocol", Protocol::class)
            addSuperclassConstructorParameter("listener", AsyncClientBase.Listener::class)
        }

        for ((index, interfaceFun) in serviceInterface.funSpecs.withIndex()) {
            val method = serviceType.methods[index]
            val call = buildCallType(schema, method)
            val spec = FunSpec.builder(interfaceFun.name).apply {
                addModifiers(KModifier.OVERRIDE)
                for (param in interfaceFun.parameters) {
                    addParameter(param)
                }

                addCode {
                    add("this.enqueue(%N(", call)
                    for ((ix, param) in interfaceFun.parameters.withIndex()) {
                        if (ix > 0) {
                            add(", ")
                        }
                        add("%N", param.name)
                    }
                    add("))")
                }
            }
            type.addType(call)
            type.addFunction(spec.build())
        }

        return type.build()
    }

    private fun buildCallType(schema: Schema, method: ServiceMethod): TypeSpec {
        val callName = method.name.capitalize() + "Call"
        val returnType = method.returnType.trueType
        val resultType = if (returnType == BuiltinType.VOID) {
            UNIT
        } else {
            returnType.typeName
        }
        val hasResult = resultType != UNIT
        val messageType = if (method.oneWay) "ONEWAY" else "CALL"
        val nameAllocator = nameAllocators[method]
        val callbackTypeName = ServiceMethodCallback::class
                .asTypeName()
                .parameterizedBy(resultType)
        val superclassTypeName = MethodCall::class
                .asTypeName()
                .parameterizedBy(resultType)


        return TypeSpec.classBuilder(callName).run {
            addModifiers(KModifier.PRIVATE)
            superclass(superclassTypeName)

            addSuperclassConstructorParameter("%S", method.name)
            addSuperclassConstructorParameter("%T.%L", TMessageType::class, messageType)
            addSuperclassConstructorParameter("%N", nameAllocator.get(Tags.CALLBACK))

            // Add ctor
            val ctor = FunSpec.constructorBuilder()

            for (param in method.parameters) {
                val name = nameAllocator.get(param)
                val type = param.type.trueType
                val typeName = type.typeName

                val defaultValue = param.defaultValue
                val hasDefaultValue = defaultValue != null
                val propertyType = when {
                    param.required -> typeName
                    hasDefaultValue -> typeName
                    else -> typeName.asNullable()
                }

                val ctorParam = ParameterSpec.builder(name, propertyType)
                if (defaultValue != null) {
                    ctorParam.defaultValue(renderConstValue(schema, type, defaultValue))
                } else if (typeName.nullable) {
                    ctorParam.defaultValue("null")
                }

                ctor.addParameter(ctorParam.build())

                addProperty(PropertySpec.builder(name, typeName)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer(name)
                        .build())
            }

            ctor.addParameter(nameAllocator.get(Tags.CALLBACK), callbackTypeName)
            primaryConstructor(ctor.build())

            // Add send method
            val send = FunSpec.builder(nameAllocator.get(Tags.SEND))
                    .addModifiers(KModifier.OVERRIDE)
                    .addAnnotation(AnnotationSpec.builder(Throws::class)
                            .addMember("%T::class", IOException::class)
                            .build())
                    .addParameter("protocol", Protocol::class)
                    .addStatement("protocol.writeStructBegin(%S)", "args")

            for (param in method.parameters) {
                val name = nameAllocator.get(param)
                val type = param.type.trueType
                val typeCodeName = type.typeCodeName
                val optional = !param.required

                if (optional) {
                    send.beginControlFlow("if (this.%N != null)", name)
                }

                send.addStatement("protocol.writeFieldBegin(%S, %L, %T.%L)",
                        param.name, // Make sure to send the Thrift name, not the allocated field name
                        param.id,
                        TType::class,
                        typeCodeName)

                generateWriteCall(send, "this.$name", type)

                send.addStatement("protocol.writeFieldEnd()")

                if (optional) {
                    send.endControlFlow()
                }
            }

            send.addStatement("protocol.writeFieldStop()")
            send.addStatement("protocol.writeStructEnd()")
            addFunction(send.build())

            // Add receive method
            val recv = FunSpec.builder(nameAllocator.get(Tags.RECEIVE))
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("protocol", Protocol::class)
                    .addParameter("metadata", MessageMetadata::class)
                    .addAnnotation(AnnotationSpec.builder(Throws::class)
                            .addMember("%T::class", Exception::class)
                            .build())


            val maybeResultType = resultType.asNullable()
            val resultName = nameAllocator.get(Tags.RESULT)
            if (hasResult) {
                recv.returns(resultType.asNonNullable())
                recv.addStatement("var %N: %T = null", resultName, maybeResultType)
            }

            for (ex in method.exceptions) {
                recv.addStatement("var %N: %T = null", nameAllocator.get(ex), ex.type.typeName.asNullable())
            }

            val fieldMeta = nameAllocator.get(Tags.FIELD)
            recv.addStatement("protocol.readStructBegin()")
                    .beginControlFlow("while (true)")
                    .addStatement("val %N = protocol.readFieldBegin()", fieldMeta)
                    .beginControlFlow("if (%N.typeId == %T.STOP)", fieldMeta, TType::class)
                    .addStatement("break")
                    .endControlFlow() // if (fieldMeta.typeId == TType.STOP)

            val readsSomething = hasResult || method.exceptions.isNotEmpty()
            if (readsSomething) {
                recv.beginControlFlow("when (%N.fieldId.toInt())", fieldMeta)
            }

            if (hasResult) {
                recv.addCode {
                    addStatement("0 -> {%>")
                    beginControlFlow("if (%N.typeId == %T.%L)", fieldMeta, TType::class, returnType.typeCodeName)

                    generateRecursiveReadCall(this, "value", returnType)
                    addStatement("%N = value", resultName)

                    nextControlFlow("else")
                    addStatement("%T.skip(protocol, %N.typeId)", ProtocolUtil::class, fieldMeta)
                    endControlFlow()
                    addStatement("%<}")
                }
            }

            for (exn in method.exceptions) {
                val name = nameAllocator.get(exn)
                val type = exn.type.trueType
                recv.addCode {
                    addStatement("${exn.id} -> {%>")
                    beginControlFlow("if (%N.typeId == %T.%L)", fieldMeta, TType::class, type.typeCodeName)

                    generateRecursiveReadCall(this, "value", type)
                    addStatement("$name = value")

                    nextControlFlow("else")
                    addStatement("%T.skip(protocol, %N.typeId)", ProtocolUtil::class, fieldMeta)
                    endControlFlow()
                    addStatement("%<}")
                }
            }

            if (readsSomething) {
                recv.addStatement("else -> %T.skip(protocol, %N.typeId)", ProtocolUtil::class, fieldMeta)
                recv.endControlFlow()
            } else {
                recv.addStatement("%T.skip(protocol, %N.typeId)", ProtocolUtil::class, fieldMeta)
            }

            recv.addStatement("protocol.readFieldEnd()")
            recv.endControlFlow() // while (true)
            recv.addStatement("protocol.readStructEnd()")

            for (exn in method.exceptions) {
                val name = nameAllocator.get(exn)
                recv.addStatement("if (%1N != null) throw %1N", name)
            }

            if (hasResult) {
                recv.addStatement("return %N ?: throw %T(%T.%L, %S)",
                        resultName,
                        ThriftException::class,
                        ThriftException.Kind::class,
                        ThriftException.Kind.MISSING_RESULT.name,
                        "Missing result")
            }

            // At this point, any exceptions have been thrown, and any results have been
            // returned.  If control reaches here, then the return type is Unit and there
            // is nothing to do.
            addFunction(recv.build())

            build()
        }
    }

    // endregion Services

    private inline fun FunSpec.Builder.addCode(fn: CodeBlock.Builder.() -> Unit) {
        addCode(buildCodeBlock(fn))
    }

    private inline fun buildCodeBlock(fn: CodeBlock.Builder.() -> Unit): CodeBlock {
        val block = CodeBlock.builder()
        block.fn()
        return block.build()
    }

    private fun makeDeprecated(): AnnotationSpec {
        return AnnotationSpec.builder(Deprecated::class)
                .addMember("message = %S", "Deprecated in source .thrift")
                .build()
    }
}

