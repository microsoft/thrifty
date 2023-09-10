/*
 * Thrifty
 *
 * Copyright (c) Benjamin Bader
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
package com.bendb.thrifty.schema

/**
 * A type defined in Thrift.
 *
 * Nearly every top-level entity in the Thrift IDL is a type of some sort;
 * only consts are not.  A type can be a built-in "primitive" like the numeric
 * types, user-defined types like structs, unions, exceptions, services, and
 * typedefs, or containers like lists, sets, and maps.
 *
 * A ThriftType represents either the definition of a new type or a reference
 * to an existing type; the distinction after parsing is minimal, and surfaces
 * primarily in what annotations are present.  For example, a
 * `StructType` could define a struct, but it could also be the
 * type of a field in another struct.  In both cases, it would contain the full
 * definition, but the field's type might carry additional annotations which
 * would only be present on the instance held by that field.
 *
 * Confusing?  Yep, a little.  Take this sample thrift for example:
 * <pre>`struct Foo {
 * // fields
 * } (label = "foo")
 *
 * struct Bar {
 * 1: required Foo (immutable = "true") foo;
 * }`</pre>
 *
 * After parsing, we have (among others) three instances of
 * `StructType`.  One for `Bar`, and two for
 * `Foo`.  The first instance is the initial definition of
 * the struct; the second is that held by the sole field of `Bar`.
 * The first instance has a single annotation, `(label = "foo")`.
 * The second has *two* annotations:
 * `(label = "foo", immutable = "true")`.
 *
 * @property name The name of this type.
 */
abstract class ThriftType internal constructor(
        open val name: String
) {
    /**
     * @return true if this type is a built-in type, e.g. i32, bool, etc.
     */
    open val isBuiltin: Boolean
        get() = false

    /**
     * @return true if this is a list type.
     */
    open val isList: Boolean
        get() = false

    /**
     * @return true if this is a set type.
     */
    open val isSet: Boolean
        get() = false

    /**
     * @return true if this is a map type.
     */
    open val isMap: Boolean
        get() = false

    /**
     * @return true if this is an enumeration type.
     */
    open val isEnum: Boolean
        get() = false

    /**
     * True if this is a structured user-defined type such as a struct, union,
     * or exception.  This does *not* mean that this type is actually
     * a struct!
     *
     * @return true if this is a structured type.
     */
    open val isStruct: Boolean
        get() = false

    /**
     * @return true if this is a typedef of another type.
     */
    open val isTypedef: Boolean
        get() = false

    /**
     * @return true if this is a user-defined RPC service type.
     */
    open val isService: Boolean
        get() = false

    /**
     * Returns the aliased type if this is a typedef or, if not,
     * returns `this`.  A convenience function for
     * codegen.
     *
     * @return true if this is a typedef.
     */
    open val trueType: ThriftType
        get() = this

    /**
     * Accepts a [Visitor], performing an arbitrary operation and
     * returning its result.
     *
     * @param visitor the visitor to invoke.
     * @param T the type returned by the visitor.
     * @return the value returned by the visitor, if any.
     */
    abstract fun <T> accept(visitor: Visitor<T>): T

    /**
     * Returns a copy of this type with the given annotations applied.
     *
     * Annotations in the new map will overwrite duplicate names on this type;
     * this instance is left unmodified.
     *
     * @param annotations the annotations to add to the returned type.
     * @return a copy of `this` with the given
     * `annotations`.
     */
    abstract fun withAnnotations(annotations: Map<String, String>): ThriftType

    /**
     * @return all annotations present on this type.
     */
    abstract val annotations: Map<String, String>

    /** @inheritdoc */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as ThriftType

        return name == that.name
    }

    /** @inheritdoc */
    override fun hashCode(): Int {
        return name.hashCode()
    }

    /**
     * Represents an arbitrary computation on a [ThriftType].
     *
     *
     * Just your standard visitor from the Gang of Four book.  Very useful
     * for code generation, which is all about type-hierarchy-specific
     * operations; visitors let us effectively augment a complicated hierarchy
     * without bloating the ThriftType classes themselves.
     *
     * @param T the type of value returned by the visit methods.
     */
    interface Visitor<T> {
        /**
         * Visit a [BuiltinType.VOID].
         *
         * @param voidType the BuiltinType instance being visited.
         * @return the result of the operation.
         */
        fun visitVoid(voidType: BuiltinType): T

        /**
         * Visit a [BuiltinType.BOOL].
         *
         * @param boolType the BuiltinType instance being visited.
         * @return the result of the operation.
         */
        fun visitBool(boolType: BuiltinType): T

        /** Visit a [BuiltinType.BYTE] or [BuiltinType.I8].
         *
         * @param byteType BuiltinType instance being visited.
         * @return the result of the operation.
         */
        fun visitByte(byteType: BuiltinType): T

        /**
         * Visit a [BuiltinType.I16].
         *
         * @param i16Type the BuiltinType being visited.
         * @return the result of the operation.
         */
        fun visitI16(i16Type: BuiltinType): T

        /**
         * Visit a [BuiltinType.I32].
         *
         * @param i32Type the BuiltinType being visited.
         * @return the result of the operation.
         */
        fun visitI32(i32Type: BuiltinType): T

        /**
         * Visit a [BuiltinType.I64].
         *
         * @param i64Type the BuiltinType being visited.
         * @return the result of the operation.
         */
        fun visitI64(i64Type: BuiltinType): T

        /**
         * Visit a [BuiltinType.DOUBLE].
         *
         * @param doubleType the BuiltinType being visited.
         * @return the result of the operation.
         */
        fun visitDouble(doubleType: BuiltinType): T

        /**
         * Visit a [BuiltinType.STRING].
         *
         * @param stringType the BuiltinType being visited.
         * @return the result of the operation.
         */
        fun visitString(stringType: BuiltinType): T

        /**
         * Visit a [BuiltinType.BINARY].
         *
         * @param binaryType the BuiltinType being visited.
         * @return the result of the operation.
         */
        fun visitBinary(binaryType: BuiltinType): T

        /**
         * Visit a user-defined enum.
         *
         * @param enumType the EnumType instance being visited.
         * @return the result of the operation.
         */
        fun visitEnum(enumType: EnumType): T

        /**
         * Visit a list type.
         *
         * @param listType the ListType instance being visited.
         * @return the result of the operation.
         */
        fun visitList(listType: ListType): T

        /**
         * Visit a set type.
         *
         * @param setType the SetType instance being visited.
         * @return the result of the operation.
         */
        fun visitSet(setType: SetType): T

        /**
         * Visit a map type.
         *
         * @param mapType the MapType instance being visited.
         * @return the result of the operation.
         */
        fun visitMap(mapType: MapType): T

        /**
         * Visit a user-defined struct, union, or exception type.
         *
         * @param structType the StructType instance being visited.
         * @return the result of the operation.
         */
        fun visitStruct(structType: StructType): T

        /**
         * Visit a typedef type.
         *
         * @param typedefType the TypedefType instance being visited.
         * @return the result of the operation.
         */
        fun visitTypedef(typedefType: TypedefType): T

        /**
         * Visit a service type.
         *
         * @param serviceType the ServiceType instance being visited.
         * @return the result of the operation.
         */
        fun visitService(serviceType: ServiceType): T
    }
}

internal fun mergeAnnotations(
        baseAnnotations: Map<String, String>,
        newAnnotations: Map<String, String>): Map<String, String> {
    return linkedMapOf<String, String>().apply {
        putAll(baseAnnotations)
        putAll(newAnnotations)
    }
}
