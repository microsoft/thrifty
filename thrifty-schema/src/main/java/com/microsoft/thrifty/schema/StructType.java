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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.microsoft.thrifty.schema.parser.StructElement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a 'structured' type in Thrift.  A StructType could be any of
 * 'struct', 'union', or 'exception'.
 */
public class StructType extends UserType {
    private final StructElement.Type structType;
    private final ImmutableList<Field> fields;

    StructType(Program program, StructElement element) {
        super(program, new UserElementMixin(element));

        this.structType = element.type();
        this.fields = element.fields().stream()
                .map(Field::new)
                .collect(ImmutableList.toImmutableList());
    }

    private StructType(Builder builder) {
        super(builder);
        this.structType = builder.structType;
        this.fields = builder.fields;
    }

    public ImmutableList<Field> fields() {
        return fields;
    }

    @Override
    public boolean isStruct() {
        return true;
    }

    public boolean isUnion() {
        return structType == StructElement.Type.UNION;
    }

    public boolean isException() {
        return structType == StructElement.Type.EXCEPTION;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitStruct(this);
    }

    @Override
    public ThriftType withAnnotations(Map<String, String> annotations) {
        return toBuilder()
                .annotations(merge(this.annotations(), annotations))
                .build();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    void link(Linker linker) {
        for (Field field : fields) {
            field.link(linker);
        }
    }

    void validate(Linker linker) {
        for (Field field : fields) {
            field.validate(linker);
        }

        Map<Integer, Field> fieldsById = new LinkedHashMap<>(fields.size());
        for (Field field : fields) {
            Field dupe = fieldsById.put(field.id(), field);
            if (dupe != null) {
                linker.addError(dupe.location(),
                        "Duplicate field IDs: " + field.name() + " and " + dupe.name()
                                + " both have the same ID (" + field.id() + ")");
            }

            if (isUnion() && field.required()) {
                linker.addError(field.location(), "Unions may not have required fields: " + field.name());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;

        StructType that = (StructType) o;
        return this.structType.equals(that.structType)
                && this.fields.equals(that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), structType, fields);
    }

    public static final class Builder extends UserType.UserTypeBuilder<StructType, Builder> {
        private StructElement.Type structType;
        private ImmutableList<Field> fields;

        Builder(StructType type) {
            super(type);
            this.structType = type.structType;
            this.fields = type.fields;
        }

        public Builder asUnion() {
            return structType(StructElement.Type.UNION);
        }

        public Builder asStruct() {
            return structType(StructElement.Type.STRUCT);
        }

        public Builder asException() {
            return structType(StructElement.Type.EXCEPTION);
        }

        public Builder structType(StructElement.Type structType) {
            this.structType = Preconditions.checkNotNull(structType, "structType");
            return this;
        }

        public Builder fields(List<Field> fields) {
            Preconditions.checkNotNull(fields, "fields");
            this.fields = ImmutableList.copyOf(fields);
            return this;
        }

        @Override
        public StructType build() {
            return new StructType(this);
        }
    }
}
