package com.microsoft.thrifty.kgen

import com.google.common.collect.HashMultimap
import com.microsoft.thrifty.Adapter
import com.microsoft.thrifty.Obfuscated
import com.microsoft.thrifty.Redacted
import com.microsoft.thrifty.StructBuilder
import com.microsoft.thrifty.TType
import com.microsoft.thrifty.ThriftException
import com.microsoft.thrifty.ThriftField
import com.microsoft.thrifty.protocol.Protocol
import com.microsoft.thrifty.schema.BuiltinType
import com.microsoft.thrifty.schema.Constant
import com.microsoft.thrifty.schema.EnumType
import com.microsoft.thrifty.schema.FieldNamingPolicy
import com.microsoft.thrifty.schema.ListType
import com.microsoft.thrifty.schema.MapType
import com.microsoft.thrifty.schema.NamespaceScope
import com.microsoft.thrifty.schema.Schema
import com.microsoft.thrifty.schema.ServiceType
import com.microsoft.thrifty.schema.SetType
import com.microsoft.thrifty.schema.StructType
import com.microsoft.thrifty.schema.ThriftType
import com.microsoft.thrifty.schema.TypedefType
import com.microsoft.thrifty.schema.parser.ConstValueElement
import com.microsoft.thrifty.util.ObfuscationUtil
import com.microsoft.thrifty.util.ProtocolUtil
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import okio.ByteString

/**
 * An example of how a Thrift struct could look in Kotlin:
 *
 * ```
 * // thrift
 *
 * struct CrayCray {
 *   1: required list<list<list<i32>>> emptyList = [[]]
 *   2: required list<set<set<i32>>> emptySet = [[]]
 *   3: required list<list<map<i32, i32>>> emptyMap = [[]]
 * }
 * ```
 *
 * ```
 * // generated kotlin
 *
 * data class CrayCray(
 *   @JvmField @ThriftField(fieldId = 1, isRequired = true) val emptyList: List<List<List<Int>>> = listOf(emptyList()),
 *   @JvmField @ThriftField(fieldId = 2, isRequired = true) val emptySet: List<Set<Set<Int>>> = emptyList(),
 *   @JvmField @ThriftField(fieldId = 3, isRequired = true) val emptyMap: List<List<Map<Int, Int>>> = emptyList()
 * ) {
 *
 *   // Optionally, if redaction or obfuscation is called for
 *   override fun toString(): String {
 *     return "CrayCray(emptyList=$emptyList, emptySet=${ObfuscationUtil.obfuscate(emptySet)}, emptyMap=$emptyMap)"
 *   }
 *
 *   // Optionally, if builders are enabled
 *   class Builder {
 *     // ...
 *   }
 *
 *   companion object {
 *     // If builders:
 *     @JvmField val ADAPTER: Adapter<CrayCray, Builder> // exactly the same as Java adapters
 *
 *     // Otherwise:
 *     @JvmField val ADAPTER: KAdapter<CrayCray> = CrayCrayAdapter()
 *   }
 *
 *   class CrayCrayAdapter : KAdapter<CrayCray> {
 *
 *     fun read(protocol: Protocol): CrayCray {
 *       var emptyList: List<List<List<Int>>>? = null
 *       var emptySet: List<Set<Set<Int>>>? = null
 *       var emptyMap: List<List<Map<Int, Int>>>? = null
 *
 *       while (true) {
 *          val fieldMeta = protocol.readFieldBegin()
 *          if (fieldMeta.typeId == TType.STOP) {
 *            break
 *          }
 *
 *          when (fieldMeta.fieldId) {
 *            1 -> {
 *              if (fieldMeta.typeId == TType.LIST) {
 *                val listMeta0 = protocol.readListBegin()
 *                val value: MutableList<MutableList<MutableList<Int>>> = ArrayList(listMeta0.size)
 *                for (i0 in 0 until listMeta0.size) {
 *                  val listMeta1 = protocol.readListBegin()
 *                  val item0: MutableList<MutableList<Int>> = ArrayList(listMeta1.size)
 *                  for (i1 in 0 until listMeta1.size) {
 *                    val listMeta2 = protocol.readListBegin()
 *                    val item1: MutableList<Int> = ArrayList(listMeta2.size)
 *                    for (i2 in 0 until listMeta2.size) {
 *                      val item2 = protocol.readI32()
 *                      item1.add(item2)
 *                    }
 *                    protocol.readListEnd()
 *                    item0.add(item1)
 *                  }
 *                  protocol.readListEnd()
 *                  value.add(item0)
 *                }
 *                protocol.readListEnd()
 *                emptyList = value
 *              } else {
 *                ProtocolUtil.skip(protocol, fieldMeta.typeId)
 *              }
 *            }
 *
 *            2 -> {
 *              // etc
 *            }
 *
 *            3 -> {
 *              // etc
 *            }
 *
 *            else -> ProtocolUtil.skip(protocol, fieldMeta.typeId)
 *          }
 *       }
 *
 *       return CrayCray(
 *         emptyList = checkNotNull(emptyList) { "Field 'emptyList' is missing" },
 *         emptySet = checkNotNull(emptySet) { "Field 'emptySet' is missing" },
 *         emptyMap = checkNotNull(emptyMap) { "Field 'emptyMap' is missing" }
 *       )
 *     }
 *
 *     fun write(protocol: Protocol, struct: CrayCray) {
 *       protocol.writeStructBegin("CrayCray")
 *       protocol.writeFieldBegin("emptyList", 1, TType.LIST)
 *       // etc, as in Java adapters
 *     }
 *   }
 * }
 * ```
 */
class KotlinCodeGenerator {
    private val resolver = Resolver()
    private val fieldNamer = FieldNamingPolicy.JAVA

    // TODO: Customizable FieldNamingPolicy
    // TODO: Customizable collection impl types

    private val ThriftType.typeName
        get() = resolver.typeNameOf(this)

    private val ThriftType.typeCode
        get() = resolver.typeCodeOf(this)

    private val ThriftType.typeCodeName
        get() = when (this.typeCode) {
            TType.BOOL -> "BOOL"
            TType.BYTE -> "BYTE"
            TType.I16 -> "I16"
            TType.I32 -> "I32"
            TType.I64 -> "I64"
            TType.DOUBLE -> "DOUBLE"
            TType.STRING -> "STRING"
            TType.LIST -> "LIST"
            TType.SET -> "SET"
            TType.MAP -> "MAP"
            TType.STRUCT -> "STRUCT"
            TType.VOID -> "VOID"
            else -> error("Unexpected TType value: ${this.typeCode}")
        }

    private val ThriftType.isConstEligible
        get() = accept(object : ThriftType.Visitor<Boolean> {
            // JVM primitives and strings can be constants
            override fun visitBool(boolType: BuiltinType) = true
            override fun visitByte(byteType: BuiltinType) = true
            override fun visitI16(i16Type: BuiltinType) = true
            override fun visitI32(i32Type: BuiltinType) = true
            override fun visitI64(i64Type: BuiltinType) = true
            override fun visitDouble(doubleType: BuiltinType) = true
            override fun visitString(stringType: BuiltinType) = true

            // Everything else, cannot.
            override fun visitBinary(binaryType: BuiltinType) = false
            override fun visitEnum(enumType: EnumType) = false
            override fun visitList(listType: ListType) = false
            override fun visitSet(setType: SetType) = false
            override fun visitMap(mapType: MapType) = false
            override fun visitStruct(structType: StructType) = false
            override fun visitTypedef(typedefType: TypedefType) = typedefType.trueType.accept(this)

            // These make no sense
            override fun visitService(serviceType: ServiceType): Boolean {
                error("Cannot have a const value of a service type")
            }
            override fun visitVoid(voidType: BuiltinType): Boolean {
                error("Cannot have a const value of void")
            }
        })

    fun generate(schema: Schema): List<FileSpec> {
        TypeSpec.classBuilder("foo")
                .addModifiers(KModifier.DATA)

        val specsByNamespace = HashMultimap.create<String, TypeSpec>()
        val constantsByNamespace = HashMultimap.create<String, PropertySpec>()

        schema.enums.forEach { specsByNamespace.put(it.kotlinNamespace, generateEnumClass(it)) }
        schema.structs.forEach { specsByNamespace.put(it.kotlinNamespace, generateDataClass(schema, it)) }
        schema.unions.forEach { specsByNamespace.put(it.kotlinNamespace, generateDataClass(schema, it)) }
        schema.exceptions.forEach { specsByNamespace.put(it.kotlinNamespace, generateDataClass(schema, it)) }

        schema.constants.forEach {
            val ns = it.kotlinNamespace
            val property = generateConstantProperty(schema, it)
            constantsByNamespace.put(ns, property)
        }

        // TODO: Constants
        // TODO: Services

        val namespaces = mutableSetOf<String>().apply {
            addAll(specsByNamespace.keys())
            addAll(constantsByNamespace.keys())
        }
        val fileSpecsByNamespace = namespaces
                .map { it to FileSpec.builder(it,"ThriftTypes.kt") }
                .toMap()

        return fileSpecsByNamespace.map { (ns, fileSpec) ->
            constantsByNamespace[ns]?.forEach { fileSpec.addProperty(it) }
            specsByNamespace[ns]?.forEach { fileSpec.addType(it) }
            fileSpec.build()
        }
    }

    fun generateEnumClass(enumType: EnumType): TypeSpec {
        val typeBuilder = TypeSpec.enumBuilder(enumType.name)
                .addProperty(PropertySpec.builder("value", INT)
                        .addAnnotation(JvmField::class)
                        .initializer("value")
                        .build())
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("value", INT)
                        .build())

        if (enumType.isDeprecated) typeBuilder.addAnnotation(Deprecated::class)
        if (enumType.hasJavadoc) typeBuilder.addKdoc(enumType.documentation)

        val findByValue = FunSpec.builder("findByValue")
                .addParameter("value", INT)
                .returns(resolver.typeNameOf(enumType).asNullable())
                .addAnnotation(JvmStatic::class)
                .beginControlFlow("return when (value)")

        for (member in enumType.members) {
            val enumMemberSpec= TypeSpec.anonymousClassBuilder()
                    .addSuperclassConstructorParameter("%L", member.value)

            if (member.isDeprecated) enumMemberSpec.addAnnotation(Deprecated::class)
            if (member.hasJavadoc) enumMemberSpec.addKdoc(member.documentation)

            typeBuilder.addEnumConstant(member.name, enumMemberSpec.build())

            findByValue.addStatement("%L -> %L", member.value, member.name)
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

    fun generateDataClass(schema: Schema, struct: StructType): TypeSpec {
        val typeBuilder = TypeSpec.classBuilder(struct.name)
                .addModifiers(KModifier.DATA)

        if (struct.isDeprecated) typeBuilder.addAnnotation(Deprecated::class)
        if (struct.hasJavadoc) typeBuilder.addKdoc(struct.documentation)
        if (struct.isException) typeBuilder.superclass(Exception::class)

        val ctorBuilder = FunSpec.constructorBuilder()

        val companionBuilder = TypeSpec.companionObjectBuilder()

        for (field in struct.fields) {
            val fieldName = fieldNamer.apply(field.name)
            val typeName = field.type().typeName.let {
                if (!field.required()) it.asNullable() else it
            }

            // TODO: Default values

            val thriftField = AnnotationSpec.builder(ThriftField::class).let { anno ->
                anno.addMember("fieldId = ${field.id()}")
                if (field.required()) anno.addMember("isRequired = true")
                if (field.optional()) anno.addMember("isOptional = true")

                field.typedefName()?.let { anno.addMember("typedefName = %S", it) }

                anno.build()
            }

            val param = ParameterSpec.builder(fieldName, typeName)

            field.defaultValue()?.let {
                param.defaultValue(renderConstValue(schema, field.type().trueType, it))
            }

            val prop = PropertySpec.builder(fieldName, typeName)
                    .initializer(fieldName)
                    .addAnnotation(JvmField::class)
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

            typeBuilder.addType(generateBuilderFor(struct))
            typeBuilder.addType(generateAdapterFor(struct, adapterTypeName, builderTypeName))

            companionBuilder.addProperty(PropertySpec.builder("ADAPTER", adapterInterfaceTypeName)
                    .initializer("%T()", adapterTypeName)
                    .addAnnotation(JvmField::class)
                    .build())
        } else {
            // TODO: Builderless adapters
        }

        if (struct.fields.any { it.isObfuscated || it.isRedacted }) {
            typeBuilder.addFunction(generateToString(struct))
        }

        return typeBuilder
                .primaryConstructor(ctorBuilder.build())
                .addType(companionBuilder.build())
                .build()
    }

    fun generateToString(struct: StructType): FunSpec {

        // Two-phase formatting technique, ACTIVATE!!!!
        // Step 1: generate a "template" similar to:
        //    "TypeName(field1=%s, field2=%s, field3=%s)"
        //
        //    generate format arguments according to the field's type and redacted/obfuscated needs:
        //    "$fieldName", or "%1T.summarizeCollection($fieldName, "list", "elementTypeName")"
        //    also add Kotlinpoet param for ObfuscationUtil, if necessary
        //
        // Step 2: generate a kotlin fun, using the template string and any kotlinpoet format args.

        val placeholders = LinkedHashSet<Any>(0)
        val formatArgs = mutableListOf<String>()
        val templateBuilder = StringBuilder().apply {
            append(struct.name)
            append("(")

            for (field in struct.fields) {
                val fieldName = fieldNamer.apply(field.name)
                append("$fieldName=")
                append("%s")
                append(", ")

                formatArgs += when {
                    field.isRedacted -> "<REDACTED>"
                    field.isObfuscated -> {
                        placeholders += ObfuscationUtil::class.java
                        val type = field.type().trueType
                        val method = when {
                            type.isList -> "summarizeCollection($fieldName, \"list\", \"${(type as ListType).elementType().trueType.name}\")"
                            type.isSet -> "summarizeCollection($fieldName, \"set\", \"${(type as ListType).elementType().trueType.name}\")"
                            type.isMap -> {
                                val mapType = type as MapType
                                val keyTypeName = mapType.keyType().trueType.name
                                val valTypeName = mapType.valueType().trueType.name
                                "summarizeMap($fieldName, \"$keyTypeName\", \"$valTypeName\")"
                            }
                            else -> "hash($fieldName)"
                        }
                        "\${%1T.$method}"
                    }
                    else -> "\$$fieldName"
                }
            }

            setLength(length - 2)
            append(")")
        }

        val template = String.format(templateBuilder.toString(), *formatArgs.toTypedArray())

        return FunSpec.builder("toString")
                .addModifiers(KModifier.OVERRIDE)
                .addCode("return \"$template\"", *placeholders.toTypedArray())
                .build()
    }

    fun generateBuilderFor(struct: StructType): TypeSpec {
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

        val buildParamStringBuilder = StringBuilder()
        for (field in struct.fields) {
            val name = fieldNamer.apply(field.name)
            val type = resolver.typeNameOf(field.type())

            // Add a private var
            val propertySpec = PropertySpec.varBuilder(name, type.asNullable(), KModifier.PRIVATE)
                    .initializer("null") // TODO: Initialize with default value, if any

            // Add a builder fun
            val buildFunParamType = if (!field.required()) type.asNullable() else type
            val builderFunSpec = FunSpec.builder(name)
                    .addParameter(name, buildFunParamType)
                    .addStatement("return apply { this.$name = $name }")

            // Add initialization in default ctor
            if (field.defaultValue() != null) {
                // TODO: Add default-ctor initializer
                defaultCtor.addStatement("this.$name = null")
            } else {
                defaultCtor.addStatement("this.$name = null")
            }

            // Add initialization in copy ctor
            copyCtor.addStatement("this.$name = source.$name")

            // reset field
            resetFunSpec.addStatement("this.$name = null")
            // TODO: Reset to default value

            // Add field to build-method ctor-invocation arg builder
            buildParamStringBuilder.append("$name = ")
            if (field.required()) {
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

    fun generateAdapterFor(struct: StructType, adapterName: ClassName, builderType: ClassName): TypeSpec {
        val adapter = TypeSpec.classBuilder(adapterName)
                .addModifiers(KModifier.PRIVATE)
                .addSuperinterface(Adapter::class.asTypeName().parameterizedBy(struct.typeName, builderType))

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

        writer.addStatement("protocol.writeStructBegin(%S)", struct.name)
        for (field in struct.fields) {
            val name = fieldNamer.apply(field.name)
            val fieldType = field.type().trueType

            if (!field.required()) {
                writer.beginControlFlow("if (struct.$name != null)")
            }

            writer.addStatement("protocol.writeFieldBegin(%S, %L, %T.%L)",
                    field.name,
                    field.id(),
                    TType::class,
                    fieldType.typeCodeName)

            generateWriteCall(writer, name, fieldType)

            writer.addStatement("protocol.writeFieldEnd()")

            if (!field.required()) {
                writer.endControlFlow()
            }
        }
        writer.addStatement("protocol.writeFieldStop()")
        writer.addStatement("protocol.writeStructEnd()")

        // Reader next

        reader.addStatement("val structMeta = protocol.readStructBegin()")
        reader.beginControlFlow("while (true)")

        reader.addStatement("val fieldMeta = protocol.readFieldBegin()")

        reader.beginControlFlow("if (fieldMeta.typeId == %T.STOP)", TType::class)
        reader.addStatement("break")
        reader.endControlFlow()

        reader.beginControlFlow("when (fieldMeta.fieldId.toInt())")


        for (field in struct.fields) {
            val name = fieldNamer.apply(field.name)
            val fieldType = field.type().trueType

            reader.addCode {
                addStatement("${field.id()} -> {%>")
                beginControlFlow("if (fieldMeta.typeId == %T.%L)", TType::class, fieldType.typeCodeName)

                add(generateReadCall(name, fieldType))

                nextControlFlow("else")
                addStatement("%T.skip(protocol, fieldMeta.typeId)", ProtocolUtil::class)
                endControlFlow()
                addStatement("%<}")
            }
        }

        if (struct.fields.isNotEmpty()) {
            reader.addStatement("else -> %T.skip(protocol, fieldMeta.typeId)", ProtocolUtil::class)
        }

        // MAGIC

        reader.endControlFlow() // when (fieldMeta.fieldId.toInt())
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
        // - writer has a parameter "struct" that is a struct type

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
                    val elementType = listType.elementType().trueType
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
                    val elementType = setType.elementType().trueType
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
                    val keyType = mapType.keyType().trueType
                    val valType = mapType.valueType().trueType

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

        generateRecursiveWrite("struct.$name", type, 0)
    }

    fun generateReadCall(name: String, type: ThriftType): CodeBlock {
        fun generateRecursiveReadCall(block: CodeBlock.Builder, name: String, type: ThriftType, scope: Int) {
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
                    val elementType = listType.elementType().trueType
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
                    val elementType = setType.elementType().trueType
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
                    val keyType = mapType.keyType().trueType
                    val valType = mapType.valueType().trueType
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
        }

        return CodeBlock.builder()
                .apply { generateRecursiveReadCall(this, name, type, 0) }
                .addStatement("builder.$name($name)")
                .build()
    }

    fun generateConstantProperty(schema: Schema, constant: Constant): PropertySpec {
        val type = constant.type().trueType
        val typeName = type.typeName
        val propBuilder = PropertySpec.builder(constant.name, typeName)

        if (constant.isDeprecated) propBuilder.addAnnotation(Deprecated::class)
        if (constant.hasJavadoc) propBuilder.addKdoc(constant.documentation)

        if (type.isConstEligible) propBuilder.addModifiers(KModifier.CONST)

        propBuilder.initializer(renderConstValue(schema, constant.type(), constant.value()))

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
                        constOrError(block, value, type, "Invalid boolean constant")
                    }
                }

                override fun visitByte(byteType: BuiltinType) {
                    if (value.isInt) {
                        block.add("%L", value.getAsInt())
                    } else {
                        constOrError(block, value, type, "Invalid byte constant")
                    }
                }

                override fun visitI16(i16Type: BuiltinType) {
                    if (value.isInt) {
                        block.add("%L", value.getAsInt())
                    } else {
                        constOrError(block, value, type, "Invalid I16 constant")
                    }
                }

                override fun visitI32(i32Type: BuiltinType) {
                    if (value.isInt) {
                        block.add("%L", value.getAsInt())
                    } else {
                        constOrError(block, value, type, "Invalid I32 constant")
                    }
                }

                override fun visitI64(i64Type: BuiltinType) {
                    if (value.isInt) {
                        block.add("%L", value.getAsInt())
                    } else {
                        constOrError(block, value, type, "Invalid I64 constant")
                    }
                }

                override fun visitDouble(doubleType: BuiltinType) {
                    if (value.isDouble) {
                        block.add("%L", value.getAsDouble())
                    } else {
                        constOrError(block, value, type, "Invalid double constant")
                    }
                }

                override fun visitString(stringType: BuiltinType) {
                    if (value.isString) {
                        block.add("%S", value.getAsString())
                    } else {
                        constOrError(block, value, type, "Invalid string constant")
                    }
                }

                override fun visitBinary(binaryType: BuiltinType) {
                    // TODO: Implement support for binary constants in the ANTLR grammar
                    if (value.isString) {
                        block.add("%T.decodeHex(%S)", ByteString::class, value.getAsString())
                    } else {
                        constOrError(block, value, type, "Invalid binary constant")
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
                        block.add("%T.%L", enumType.typeName, member.name)
                    } else {
                        constOrError(block, value, type, "Invalid enum constant")
                    }
                }

                override fun visitList(listType: ListType) {
                    visitCollection(listType.elementType().trueType, "listOf", "Invalid list constant")

                }

                override fun visitSet(setType: SetType) {
                    visitCollection(
                            setType.elementType().trueType,
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
                        constOrError(block, value, type, errorMessage)
                    }
                }

                override fun visitMap(mapType: MapType) {
                    val keyType = mapType.keyType().trueType
                    val valueType = mapType.valueType().trueType
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
                        constOrError(block, value, type, "Invalid map constant")
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

                private fun constOrError(block:CodeBlock.Builder, value: ConstValueElement, type: ThriftType, error: String) {
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
                                        && it.type().trueType == type
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

    private inline fun FunSpec.Builder.addCode(fn: CodeBlock.Builder.() -> Unit) {
        addCode(buildCodeBlock(fn))
    }

    private inline fun buildCodeBlock(fn: CodeBlock.Builder.() -> Unit): CodeBlock {
        val block = CodeBlock.builder()
        block.fn()
        return block.build()
    }
}

