package com.microsoft.thrifty.kgen

import com.microsoft.thrifty.schema.FieldNamingPolicy
import com.microsoft.thrifty.schema.ListType
import com.microsoft.thrifty.schema.MapType
import com.microsoft.thrifty.schema.Schema
import com.microsoft.thrifty.schema.StructType
import com.microsoft.thrifty.util.ObfuscationUtil
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

data class CrayCray(
        @JvmField val emptyList: List<List<List<Int>>> = emptyList(),
        @JvmField val emptySet: List<Set<Set<Int>>> = emptyList()
)

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
 *   @JvmField @ThriftField(fieldId = 1, isRequired = true) val emptyList: List<List<List<Int>>> = emptyList(),
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

    fun generate(schema: Schema): List<FileSpec> {
        TypeSpec.classBuilder("foo")
                .addModifiers(KModifier.DATA)

        val structsByNamespace = (schema.structs + schema.unions + schema.exceptions)
                .groupBy({ it.kotlinNamespace }, { generateDataClass(it) })

        val structFiles = structsByNamespace.map { (ns, types) ->
            FileSpec.builder(ns, "Structs.kt").let { builder ->
                types.forEach { builder.addType(it) }
                builder.build()
            }
          }

        return structFiles
    }

    fun generateDataClass(struct: StructType): TypeSpec {
        val typeBuilder = TypeSpec.classBuilder(struct.name)
                .addModifiers(KModifier.DATA)

        if (struct.isDeprecated) {
            typeBuilder.addAnnotation(Deprecated::class)
        }

        if (struct.hasJavadoc) {
            typeBuilder.addKdoc(struct.documentation)
        }

        if (struct.isException) {
            typeBuilder.superclass(Exception::class)
        }

        val ctorBuilder = FunSpec.constructorBuilder()

        for (field in struct.fields) {
            val typeName = resolver.typeNameOf(field.type()).let {
                if (field.optional()) {
                    it.asNullable()
                } else {
                    it.asNonNullable()
                }
            }

            val fieldName = fieldNamer.apply(field.name)

            // TODO: Default values

            val param = ParameterSpec.builder(fieldName, typeName)
            val prop = PropertySpec.builder(fieldName, typeName)
                    .initializer(fieldName)
                    .addAnnotation(JvmField::class)

            ctorBuilder.addParameter(param.build())
            typeBuilder.addProperty(prop.build())
        }

        if (struct.fields.any { it.isObfuscated || it.isRedacted }) {
            typeBuilder.addFunction(generateToString(struct))
        }

        return typeBuilder
                .primaryConstructor(ctorBuilder.build())
                .build()
    }

    fun generateToString(struct: StructType): FunSpec {

        // Two-phase formatting technique, ACTIVATE!!!!

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

            this.setLength(length - 2)

            append(")")
        }

        val template = String.format(templateBuilder.toString(), *formatArgs.toTypedArray())

        return FunSpec.builder("toString")
                .addModifiers(KModifier.OVERRIDE)
                .addCode("return \"$template\"", *placeholders.toTypedArray())
                .build()
    }
}
