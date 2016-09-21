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
import com.microsoft.thrifty.schema.parser.AnnotationElement;
import com.microsoft.thrifty.schema.parser.ConstValueElement;
import com.microsoft.thrifty.schema.parser.FieldElement;

import java.util.Locale;

import javax.annotation.Nullable;

public final class Field {
    private final FieldElement element;
    private final FieldNamingPolicy fieldNamingPolicy;
    private final ImmutableMap<String, String> annotations;
    private ThriftType type;

    private String javaName;

    Field(FieldElement element, FieldNamingPolicy fieldNamingPolicy) {
        this.element = element;
        this.fieldNamingPolicy = fieldNamingPolicy;

        ImmutableMap.Builder<String, String> annotationBuilder = ImmutableMap.builder();
        AnnotationElement anno = element.annotations();
        if (anno != null) {
            annotationBuilder.putAll(anno.values());
        }
        this.annotations = annotationBuilder.build();
    }

    private Field(Builder builder) {
        this.element = builder.element;
        this.fieldNamingPolicy = builder.fieldNamingPolicy;
        this.annotations = builder.annotations;
        this.type = builder.type;
    }

    public Location location() {
        return element.location();
    }

    public int id() {
        Integer id = element.fieldId();
        if (id == null) {
            // IDs should have been definitively assigned during parse.
            // A missing ID at this point is a parser error.
            throw new AssertionError("Field ID should not be null");
        }
        return id;
    }

    public String thriftName() {
        return element.name();
    }

    public String name() {
        if (javaName == null) {
            javaName = fieldNamingPolicy.apply(element.name());
        }
        return javaName;
    }

    public boolean required() {
        return element.requiredness() == Requiredness.REQUIRED;
    }

    public boolean optional() {
        return element.requiredness() == Requiredness.OPTIONAL;
    }

    public String documentation() {
        return element.documentation();
    }

    public boolean hasJavadoc() {
        return JavadocUtil.hasJavadoc(this);
    }

    public ConstValueElement defaultValue() {
        return element.constValue();
    }

    public ThriftType type() {
        return type;
    }

    public ImmutableMap<String, String> annotations() {
        return annotations;
    }

    public boolean isRedacted() {
        return annotations.containsKey("thrifty.redacted")
                || annotations.containsKey("redacted")
                || (hasJavadoc() && documentation().toLowerCase(Locale.US).contains("@redacted"));
    }

    public boolean isObfuscated() {
        return annotations.containsKey("thrifty.obfuscated")
                || annotations.containsKey("obfuscated")
                || (hasJavadoc() && documentation().toLowerCase(Locale.US).contains("@obfuscated"));
    }

    public boolean isDeprecated() {
        return annotations.containsKey("deprecated")
                || annotations.containsKey("thrifty.deprecated")
                || (hasJavadoc() && documentation().toLowerCase(Locale.US).contains("@deprecated"));
    }

    public Builder toBuilder() {
        return new Builder(element, fieldNamingPolicy, annotations, type);
    }

    public static final class Builder {
        private FieldElement element;
        private FieldNamingPolicy fieldNamingPolicy;
        private ImmutableMap<String, String> annotations;
        private ThriftType type;

        Builder(FieldElement element,
                       FieldNamingPolicy fieldNamingPolicy,
                       ImmutableMap<String, String> annotations,
                       ThriftType type) {
            this.element = element;
            this.fieldNamingPolicy = fieldNamingPolicy;
            this.annotations = annotations;
            this.type = type;
        }

        public Builder element(FieldElement element) {
            if (element == null) {
                throw new NullPointerException("element may not be null.");
            }
            this.element = element;
            return this;
        }

        public Builder fieldNamingPolicy(FieldNamingPolicy fieldNamingPolicy) {
            this.fieldNamingPolicy = fieldNamingPolicy;
            return this;
        }

        public Builder annotations(ImmutableMap<String, String> annotations) {
            this.annotations = annotations;
            return this;
        }

        public Builder type(ThriftType type) {
            this.type = type;
            return this;
        }

        public Field build() {
            return new Field(this);
        }
    }

    void setType(ThriftType type) {
        this.type = type;
    }

    @Nullable
    public String typedefName() {
        String name = null;
        if (type != null && type.isTypedef()) {
            name = type.name();
        }
        return name;
    }

    void link(Linker linker) {
        type = linker.resolveType(element.type());
    }

    void validate(Linker linker) {
        ConstValueElement value = element.constValue();
        if (value != null) {
            try {
                Constant.validate(linker, value, type);
            } catch (IllegalStateException e) {
                linker.addError(value.location(), e.getMessage());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Field field = (Field) o;

        if (!element.equals(field.element)) {
            return false;
        }
        if (fieldNamingPolicy != null ? !fieldNamingPolicy.equals(field.fieldNamingPolicy)
                : field.fieldNamingPolicy != null) {
            return false;
        }
        if (annotations != null ? !annotations.equals(field.annotations) : field.annotations != null) {
            return false;
        }
        if (type != null ? !type.equals(field.type) : field.type != null) {
            return false;
        }
        return javaName != null ? javaName.equals(field.javaName) : field.javaName == null;

    }

    @Override
    public int hashCode() {
        int result = element.hashCode();
        result = 31 * result + (fieldNamingPolicy != null ? fieldNamingPolicy.hashCode() : 0);
        result = 31 * result + (annotations != null ? annotations.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (javaName != null ? javaName.hashCode() : 0);
        return result;
    }
}
