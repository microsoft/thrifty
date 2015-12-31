/*
 * Copyright (C) 2015 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.thrifty.schema;

import com.bendb.thrifty.schema.parser.AnnotationElement;
import com.bendb.thrifty.schema.parser.ConstValueElement;
import com.bendb.thrifty.schema.parser.FieldElement;
import com.google.common.collect.ImmutableMap;

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

    public int id() {
        Integer id = element.fieldId();
        if (id == null) {
            // IDs should have been definitively assigned during parse.
            // A missing ID at this point is a parser error.
            throw new AssertionError("Field ID should not be null");
        }
        return id;
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
            Constant.validate(linker, value, type);
        }
    }
}
