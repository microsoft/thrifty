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

import java.util.Map;

public class BuiltinThriftType extends ThriftType {
    public static final ThriftType BOOL   = new BuiltinThriftType("bool");
    public static final ThriftType BYTE   = new BuiltinThriftType("byte");
    public static final ThriftType I8     = new BuiltinThriftType("i8");
    public static final ThriftType I16    = new BuiltinThriftType("i16");
    public static final ThriftType I32    = new BuiltinThriftType("i32");
    public static final ThriftType I64    = new BuiltinThriftType("i64");
    public static final ThriftType DOUBLE = new BuiltinThriftType("double");
    public static final ThriftType STRING = new BuiltinThriftType("string");
    public static final ThriftType BINARY = new BuiltinThriftType("binary");
    public static final ThriftType VOID   = new BuiltinThriftType("void");

    private static final ImmutableMap<String, ThriftType> BUILTINS;

    static {
        BUILTINS = ImmutableMap.<String, ThriftType>builder()
                .put(BOOL.name(),   BOOL)
                .put(BYTE.name(),   BYTE)
                .put(I8.name(),     I8)
                .put(I16.name(),    I16)
                .put(I32.name(),    I32)
                .put(I64.name(),    I64)
                .put(DOUBLE.name(), DOUBLE)
                .put(STRING.name(), STRING)
                .put(BINARY.name(), BINARY)
                .put(VOID.name(),   VOID)
                .build();
    }

    public static ThriftType get(String name) {
        return BUILTINS.get(name);
    }

    private ImmutableMap<String, String> annotations;

    BuiltinThriftType(String name) {
        this(name, ImmutableMap.<String, String>of());
    }

    BuiltinThriftType(String name, ImmutableMap<String, String> annotations) {
        super(name);
        this.annotations = annotations;
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        if (this.equals(BOOL)) {
            return visitor.visitBool(this);
        } else if (this.equals(BYTE)) {
            return visitor.visitByte(this);
        } else if (this.equals(I8)) {
            // Synonym for byte
            return visitor.visitByte(this);
        } else if (this.equals(I16)) {
            return visitor.visitI16(this);
        } else if (this.equals(I32)) {
            return visitor.visitI32(this);
        } else if (this.equals(I64)) {
            return visitor.visitI64(this);
        } else if (this.equals(DOUBLE)) {
            return visitor.visitDouble(this);
        } else if (this.equals(STRING)) {
            return visitor.visitString(this);
        } else if (this.equals(BINARY)) {
            return visitor.visitBinary(this);
        } else {
            throw new AssertionError("Unexpected ThriftType: " + name());
        }
    }

    @Override
    public ImmutableMap<String, String> annotations() {
        return annotations;
    }

    @Override
    ThriftType withAnnotations(Map<String, String> annotations) {
        return new BuiltinThriftType(name(), merge(this.annotations, annotations));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (getClass() != o.getClass()) return false;

        BuiltinThriftType that = (BuiltinThriftType) o;

        if (this.name().equals(that.name())) {
            return true;
        }

        // 'byte' and 'i8' are synonyms
        if (this.name().equals(BYTE.name()) && that.name().equals(I8.name())) {
            return true;
        }

        if (this.name().equals(I8.name()) && that.name().equals(BYTE.name())) {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        String name = name();
        if (name.equals(I8.name())) {
            name = BYTE.name();
        }
        return name.hashCode();
    }
}
