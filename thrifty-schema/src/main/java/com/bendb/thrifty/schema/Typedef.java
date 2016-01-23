/*
 * Copyright (C) 2015-2016 Benjamin Bader
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
import com.bendb.thrifty.schema.parser.TypedefElement;
import com.google.common.collect.ImmutableMap;

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

    boolean link(Linker linker) {
        oldType = linker.resolveType(element.oldType());
        type = ThriftType.typedefOf(oldType, element.newName());
        return true;
    }
}
