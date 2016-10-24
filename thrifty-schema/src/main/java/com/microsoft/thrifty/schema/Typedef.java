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
import com.microsoft.thrifty.schema.parser.TypedefElement;

import java.util.Map;

public final class Typedef extends Named {
    private final TypedefElement element;
    private final ImmutableMap<String, String> annotations;
    private ThriftType oldType;
    private ThriftType type;

    Typedef(TypedefElement element, Map<NamespaceScope, String> namespaces) {
        super(element.newName(), namespaces);
        this.element = element;

        ImmutableMap.Builder<String, String> annotationBuilder = ImmutableMap.builder();
        AnnotationElement anno = element.annotations();
        if (anno != null) {
            annotationBuilder.putAll(anno.values());
        }
        this.annotations = annotationBuilder.build();
    }

    private Typedef(Builder builder) {
        super(builder.element.newName(), builder.namespaces);
        this.element = builder.element;
        this.oldType = builder.oldType;
        this.type = builder.type;
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

    public String oldName() {
        return element.oldType().name();
    }

    public ThriftType oldType() {
        return oldType;
    }

    public ImmutableMap<String, String> annotations() {
        return annotations;
    }

    public ImmutableMap<String, String> sourceTypeAnnotations() {
        return oldType.annotations();
    }

    public Builder toBuilder() {
        return new Builder(element, annotations, oldType, type, namespaces());
    }

    boolean link(Linker linker) {
        oldType = linker.resolveType(element.oldType());
        type = ThriftType.typedefOf(oldType, element.newName());
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Typedef typedef = (Typedef) o;

        if (!element.equals(typedef.element)) {
            return false;
        }
        if (annotations != null ? !annotations.equals(typedef.annotations) : typedef.annotations != null) {
            return false;
        }
        if (oldType != null ? !oldType.equals(typedef.oldType) : typedef.oldType != null) {
            return false;
        }
        return type != null ? type.equals(typedef.type) : typedef.type == null;

    }

    @Override
    public int hashCode() {
        int result = element.hashCode();
        result = 31 * result + (annotations != null ? annotations.hashCode() : 0);
        result = 31 * result + (oldType != null ? oldType.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public static final class Builder {
        private TypedefElement element;
        private ImmutableMap<String, String> annotations;
        private ThriftType oldType;
        private ThriftType type;
        private Map<NamespaceScope, String> namespaces;

        Builder(TypedefElement element,
                       ImmutableMap<String, String> annotations,
                       ThriftType oldType,
                       ThriftType type,
                       Map<NamespaceScope, String> namespaces) {
            this.element = element;
            this.annotations = annotations;
            this.type = type;
            this.oldType = oldType;
            this.namespaces = namespaces;
        }

        public Builder element(TypedefElement element) {
            if (element == null) {
                throw new NullPointerException("element can't be null.");
            }
            this.element = element;
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

        public Builder type(ThriftType type) {
            this.type = type;
            return this;
        }

        public Builder oldType(ThriftType oldType) {
            this.oldType = oldType;
            return this;
        }

        public Typedef build() {
            return new Typedef(this);
        }
    }
}
