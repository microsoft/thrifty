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

import javax.annotation.Nullable;
import java.util.Locale;

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
}
