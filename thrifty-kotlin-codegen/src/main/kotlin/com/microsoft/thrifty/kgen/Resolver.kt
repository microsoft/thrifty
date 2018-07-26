package com.microsoft.thrifty.kgen

import com.microsoft.thrifty.schema.BuiltinType
import com.microsoft.thrifty.schema.EnumType
import com.microsoft.thrifty.schema.ListType
import com.microsoft.thrifty.schema.MapType
import com.microsoft.thrifty.schema.NamespaceScope
import com.microsoft.thrifty.schema.ServiceType
import com.microsoft.thrifty.schema.SetType
import com.microsoft.thrifty.schema.StructType
import com.microsoft.thrifty.schema.ThriftType
import com.microsoft.thrifty.schema.TypedefType
import com.microsoft.thrifty.schema.UserType
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asTypeName
import okio.ByteString

class Resolver {
    fun typeNameOf(type: ThriftType): TypeName {
        return type.accept(TypeNameVisitor)
    }
}

object TypeNameVisitor : ThriftType.Visitor<TypeName> {
    override fun visitVoid(voidType: BuiltinType) = UNIT

    override fun visitBool(boolType: BuiltinType) = BOOLEAN

    override fun visitByte(byteType: BuiltinType) = BYTE

    override fun visitI16(i16Type: BuiltinType) = SHORT

    override fun visitI32(i32Type: BuiltinType) = INT

    override fun visitI64(i64Type: BuiltinType) = LONG

    override fun visitDouble(doubleType: BuiltinType) = DOUBLE

    override fun visitString(stringType: BuiltinType) = String::class.asTypeName()

    override fun visitBinary(binaryType: BuiltinType) = ByteString::class.asTypeName()

    override fun visitEnum(enumType: EnumType) = userTypeName(enumType)

    override fun visitList(listType: ListType): TypeName {
        val elementType = listType.elementType().accept(this)
        return ParameterizedTypeName.get(List::class.asTypeName(), elementType)
    }

    override fun visitSet(setType: SetType): TypeName {
        val elementType = setType.elementType().accept(this)
        return ParameterizedTypeName.get(Set::class.asTypeName(), elementType)
    }

    override fun visitMap(mapType: MapType): TypeName {
        val keyType = mapType.keyType().accept(this)
        val valueType = mapType.valueType().accept(this)
        return ParameterizedTypeName.get(Map::class.asTypeName(), keyType, valueType)
    }

    override fun visitStruct(structType: StructType): TypeName = userTypeName(structType)

    override fun visitTypedef(typedefType: TypedefType) = typedefType.trueType.accept(this)

    override fun visitService(serviceType: ServiceType) = userTypeName(serviceType)

    private fun userTypeName(userType: UserType): TypeName {
        return ClassName(userType.kotlinNamespace, userType.name)
    }

}

val UserType.kotlinNamespace: String
    get() {
        val kotlinNs = namespaces[NamespaceScope.KOTLIN]
        val javaNs = namespaces[NamespaceScope.JAVA]
        val fallbackNs = namespaces[NamespaceScope.ALL]

        return kotlinNs ?: javaNs ?: fallbackNs ?: throw AssertionError("No JVM namespace defined for $name")
    }