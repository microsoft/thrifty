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
    private final Map<NamespaceScope, String> namespaces;
    private ThriftType oldType;
    private ThriftType type;

    Typedef(TypedefElement element, Map<NamespaceScope, String> namespaces) {
        super(element.newName(), namespaces);
        this.element = element;
        this.namespaces = namespaces;

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
        this.annotations = builder.annotations;
        this.namespaces = builder.namespaces;
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

    public Builder toBuilder(Typedef typeDef) {
        return new Builder(typeDef.element, typeDef.annotations, typeDef.namespaces);
    }

    boolean link(Linker linker) {
        oldType = linker.resolveType(element.oldType());
        type = ThriftType.typedefOf(oldType, element.newName());
        return true;
    }

    public static final class Builder {
        private TypedefElement element;
        private ImmutableMap<String, String> annotations;
        private Map<NamespaceScope, String> namespaces;

        Builder(TypedefElement element,
                       ImmutableMap<String, String> annotations,
                       Map<NamespaceScope, String> namespaces) {
            this.element = element;
            this.annotations = annotations;
            this.namespaces = namespaces;
        }

        public Builder element(TypedefElement element) {
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

        public Typedef build() {
            return new Typedef(this);
        }
    }
}
