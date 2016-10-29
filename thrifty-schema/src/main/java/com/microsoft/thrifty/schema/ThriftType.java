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
 */
public abstract class ThriftType {
    private final String name;

    ThriftType(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public abstract <T> T accept(Visitor<T> visitor);

    public boolean isBuiltin() {
        return false;
    }

    public boolean isList() {
        return false;
    }

    public boolean isSet() {
        return false;
    }

    public boolean isMap() {
        return false;
    }

    public boolean isEnum() {
        return false;
    }

    public boolean isStruct() {
        return false;
    }

    public boolean isTypedef() {
        return false;
    }

    public boolean isService() {
        return false;
    }

    public ThriftType getTrueType() {
        return this;
    }

    abstract ThriftType withAnnotations(Map<String, String> annotations);

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

    public interface Visitor<T> {
        T visitVoid(BuiltinThriftType voidType);
        T visitBool(BuiltinThriftType boolType);
        T visitByte(BuiltinThriftType byteType);
        T visitI16(BuiltinThriftType i16Type);
        T visitI32(BuiltinThriftType i32Type);
        T visitI64(BuiltinThriftType i64Type);
        T visitDouble(BuiltinThriftType doubleType);
        T visitString(BuiltinThriftType stringType);
        T visitBinary(BuiltinThriftType binaryType);

        T visitEnum(EnumType enumType);

        T visitList(ListType listType);

        T visitSet(SetType setType);

        T visitMap(MapType mapType);

        T visitStruct(StructType structType);

        T visitTypedef(TypedefType typedefType);

        T visitService(ServiceType serviceType);
    }
}
