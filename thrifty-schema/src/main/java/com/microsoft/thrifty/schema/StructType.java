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

import com.microsoft.thrifty.schema.parser.AnnotationElement;
import com.microsoft.thrifty.schema.parser.FieldElement;
import com.microsoft.thrifty.schema.parser.StructElement;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.Map;

public class StructType extends Named {
    private final StructElement element;
    private final ThriftType type;
    private final ImmutableList<Field> fields;
    private final ImmutableMap<String, String> annotations;

    StructType(
            StructElement element,
            ThriftType type,
            Map<NamespaceScope, String> namespaces,
            FieldNamingPolicy fieldNamingPolicy) {
        super(element.name(), namespaces);
        this.element = element;
        this.type = type;

        ImmutableList.Builder<Field> fieldsBuilder = ImmutableList.builder();
        for (FieldElement fieldElement : element.fields()) {
            fieldsBuilder.add(new Field(fieldElement, fieldNamingPolicy));
        }
        this.fields = fieldsBuilder.build();

        ImmutableMap.Builder<String, String> annotationBuilder = ImmutableMap.builder();
        AnnotationElement anno = element.annotations();
        if (anno != null) {
            annotationBuilder.putAll(anno.values());
        }
        this.annotations = annotationBuilder.build();
    }

    private StructType(Builder builder) {
        super(builder.element.name(), builder.namespaces);
        this.element = builder.element;
        this.type = builder.type;
        this.fields = builder.fields;
        this.annotations = builder.annotations;
    }

    @Override
    public ThriftType type() {
        return type;
    }

    @Override
    public String documentation() {
        return element.documentation();
    }

    @Override
    public Location location() {
        return element.location();
    }

    public ImmutableList<Field> fields() {
        return fields;
    }

    public ImmutableMap<String, String> annotations() {
        return annotations;
    }

    public boolean isStruct() {
        return element.type() == StructElement.Type.STRUCT;
    }

    public boolean isUnion() {
        return element.type() == StructElement.Type.UNION;
    }

    public boolean isException() {
        return element.type() == StructElement.Type.EXCEPTION;
    }

    public Builder toBuilder() {
        return new Builder(element, type, fields, annotations, namespaces());
    }

    public static final class Builder {
        private StructElement element;
        private ThriftType type;
        private ImmutableList<Field> fields;
        private ImmutableMap<String, String> annotations;
        private Map<NamespaceScope, String> namespaces;

        Builder(StructElement element,
                       ThriftType type,
                       ImmutableList<Field> fields,
                       ImmutableMap<String, String> annotations,
                       Map<NamespaceScope, String> namespaces) {
            this.element = element;
            this.type = type;
            this.fields = fields;
            this.annotations = annotations;
            this.namespaces = namespaces;
        }

        public Builder element(StructElement element) {
            if (element == null) {
                throw new NullPointerException("element can't be null.");
            }
            this.element = element;
            return this;
        }

        public Builder type(ThriftType type) {
            this.type = type;
            return this;
        }

        public Builder fields(ImmutableList<Field> fields) {
            this.fields = fields;
            return this;
        }

        public Builder annotations(ImmutableMap<String, String> annotations) {
            this.annotations = annotations;
            return this;
        }

        public Builder namespaces(Map<NamespaceScope, String> namespaces) {
            this.namespaces = namespaces;
            return this;
        }

        public StructType build() {
            return new StructType(this);
        }
    }

    @Override
    public boolean isDeprecated() {
        return super.isDeprecated()
                || annotations.containsKey("deprecated")
                || annotations.containsKey("thrifty.deprecated");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        StructType that = (StructType) o;

        if (!element.equals(that.element)) { return false; }
        if (type != null ? !type.equals(that.type) : that.type != null) { return false; }
        if (fields != null ? !fields.equals(that.fields) : that.fields != null) { return false; }
        return annotations != null ? annotations.equals(that.annotations) : that.annotations == null;

    }

    @Override
    public int hashCode() {
        int result = element.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        result = 31 * result + (annotations != null ? annotations.hashCode() : 0);
        return result;
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
}
