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
package com.microsoft.thrifty.schema;

import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A type defined in Thrift.
 *
 * <p>Nearly every top-level entity in the Thrift IDL is a type of some sort;
 * only consts are not.  A type can be a built-in "primitive" like the numeric
 * types, user-defined types like structs, unions, exceptions, services, and
 * typedefs, or containers like lists, sets, and maps.
 *
 * A ThriftType represents either the definition of a new type or a reference
 * to an existing type; the distinction after parsing is minimal, and surfaces
 * primarily in what annotations are present.  For example, a
 * <code>StructType</code> could define a struct, but it could also be the
 * type of a field in another struct.  In both cases, it would contain the full
 * definition, but the field's type might carry additional annotations which
 * would only be present on the instance held by that field.
 *
 * Confusing?  Yep, a little.  Take this sample thrift for example:
 * <pre><code>struct Foo {
 *   // fields
 * } (label = "foo")
 *
 * struct Bar {
 *   1: required Foo (immutable = "true") foo;
 * }
 * </code></pre>
 *
 * After parsing, we have (among others) three instances of
 * <code>StructType</code>.  One for <code>Bar</code>, and two for
 * <code>Foo</code>.  The first instance is the initial definition of
 * the struct; the second is that held by the sole field of <code>Bar</code>.
 * The first instance has a single annotation, <code>(label = "foo")</code>.
 * The second has <i>two</i> annotations:
 * <code>(label = "foo", immutable = "true")</code>.
 */
public abstract class ThriftType {
    private final String name;

    ThriftType(String name) {
        this.name = name;
    }

    /**
     * Gets the name of this type.
     */
    public String name() {
        return name;
    }

    /**
     * Accepts a {@link Visitor<T>}, performing an arbitrary operation and
     * returning its result.
     *
     * @param visitor the visitor to invoke.
     * @param <T> the type returned by <code>visitor</code>.
     * @return the value returned by <code>visitor</code>, if any.
     */
    public abstract <T> T accept(Visitor<T> visitor);

    /**
     * True if this type is a built-in type, e.g. i32, bool, string, etc.
     */
    public boolean isBuiltin() {
        return false;
    }

    /**
     * True if this is a list type.
     */
    public boolean isList() {
        return false;
    }

    /**
     * True if this is a set type.
     */
    public boolean isSet() {
        return false;
    }

    /**
     * True if this is a map type.
     */
    public boolean isMap() {
        return false;
    }

    /**
     * True if this is an enumeration type.
     */
    public boolean isEnum() {
        return false;
    }

    /**
     * True if this is a structured user-defined type such as a struct, union,
     * or exception.  This does <em>not</em> mean that this type is actually
     * a struct!
     */
    public boolean isStruct() {
        return false;
    }

    /**
     * True if this is a typedef of another type.
     */
    public boolean isTypedef() {
        return false;
    }

    /**
     * True if this is a user-defined RPC service type.
     */
    public boolean isService() {
        return false;
    }

    /**
     * Returns the aliased type if this is a typedef or, if not,
     * returns <code>this</code>.  A convenience function for
     * codegen.
     */
    public ThriftType getTrueType() {
        return this;
    }

    /**
     * Returns a copy of this type with the given annotations applied.
     *
     * Annotations in the new map will overwrite duplicate names on this type;
     * this instance is left unmodified.
     *
     * @param annotations the annotations to add to the returned type.
     * @return a copy of <code>this</code> with the given
     *         <code>annotations</code>.
     */
    public abstract ThriftType withAnnotations(Map<String, String> annotations);

    /**
     * Returns all annotations present on this type.
     */
    public abstract ImmutableMap<String, String> annotations();

    protected static ImmutableMap<String, String> merge(
            ImmutableMap<String, String> baseAnnotations,
            Map<String, String> newAnnotations) {
        LinkedHashMap<String, String> merged = new LinkedHashMap<>();
        merged.putAll(baseAnnotations);
        merged.putAll(newAnnotations);
        return ImmutableMap.copyOf(merged);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ThriftType that = (ThriftType) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Represents an arbitrary computation on a {@link ThriftType}.
     *
     * <p>Just your standard visitor from the Gang of Four book; very useful
     * for code generation which is all about type-hierarchy-specific
     * operations; visitors let us effectively augment a complicated hierarchy
     * without bloating the ThriftType classes themselves.
     *
     * @param <T> the type of value returned by the visit methods.
     */
    public interface Visitor<T> {
        /** Visit a BuiltinType.VOID. */
        T visitVoid(BuiltinType voidType);

        /** Visit a BuiltinType.BOOL. */
        T visitBool(BuiltinType boolType);

        /** Visit a BuiltinType.BYTE or BuiltinType.I8. */
        T visitByte(BuiltinType byteType);

        /** Visit a BuiltinType.I16. */
        T visitI16(BuiltinType i16Type);

        /** Visit a BuiltinType.I32. */
        T visitI32(BuiltinType i32Type);

        /** Visit a BuiltinType.I64. */
        T visitI64(BuiltinType i64Type);

        /** Visit a BuiltinType.DOUBLE. */
        T visitDouble(BuiltinType doubleType);

        /** Visit a BuiltinType.STRING. */
        T visitString(BuiltinType stringType);

        /** Visit a BuiltinType.BINARY. */
        T visitBinary(BuiltinType binaryType);

        /** Visit a user-defined enum. */
        T visitEnum(EnumType enumType);

        /** Visit a list type. */
        T visitList(ListType listType);

        /** Visit a set type. */
        T visitSet(SetType setType);

        /** Visit a map type. */
        T visitMap(MapType mapType);

        /** Visit a user-defined struct, union, or exception type. */
        T visitStruct(StructType structType);

        /** Visit a typedef type. */
        T visitTypedef(TypedefType typedefType);

        /** Visit a service type. */
        T visitService(ServiceType serviceType);
    }
}
