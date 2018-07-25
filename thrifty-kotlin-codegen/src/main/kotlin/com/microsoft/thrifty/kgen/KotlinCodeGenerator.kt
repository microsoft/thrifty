package com.microsoft.thrifty.kgen

import com.microsoft.thrifty.schema.FieldNamingPolicy
import com.microsoft.thrifty.schema.Schema
import com.microsoft.thrifty.schema.StructType
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
        checkNotNull() { }
        TypeSpec.classBuilder("foo")
                .addModifiers(KModifier.DATA)

        return emptyList()
    }

    fun generateDataClass(struct: StructType): TypeSpec {
        val typeBuilder = TypeSpec.classBuilder(struct.name)
                .addModifiers(KModifier.DATA)

        val ctorBuilder = FunSpec.constructorBuilder()

        for (field in struct.fields) {
            val typeName = resolver.typeNameOf(field.type())
            val fieldName = fieldNamer.apply(field.name)

            // TODO: Default values

            val param = ParameterSpec.builder(fieldName, typeName).build()
            val prop = PropertySpec.builder(fieldName, typeName)
                    .initializer(fieldName)
                    .addAnnotation(JvmField::class)
                    .build()

            ctorBuilder.addParameter(param)
            typeBuilder.addProperty(prop)

//            val kType =
//            val paramBuilder = ParameterSpec.
        }

        return typeBuilder
                .primaryConstructor(ctorBuilder.build())
                .build()
    }
}
