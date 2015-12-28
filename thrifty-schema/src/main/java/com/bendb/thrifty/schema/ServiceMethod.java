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
import com.bendb.thrifty.schema.parser.FieldElement;
import com.bendb.thrifty.schema.parser.FunctionElement;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;

public final class ServiceMethod {
    private final FunctionElement element;
    private final ImmutableList<Field> paramTypes;
    private final ImmutableList<Field> exceptionTypes;
    private final ImmutableMap<String, String> annotations;

    private ThriftType returnType;

    public ServiceMethod(FunctionElement element) {
        this.element = element;

        ImmutableList.Builder<Field> params = ImmutableList.builder();
        for (FieldElement field : element.params()) {
            params.add(new Field(field, FieldNamingPolicy.DEFAULT));
        }
        this.paramTypes = params.build();

        ImmutableList.Builder<Field> exceptions = ImmutableList.builder();
        for (FieldElement field : element.exceptions()) {
            exceptions.add(new Field(field, FieldNamingPolicy.DEFAULT));
        }
        this.exceptionTypes = exceptions.build();

        ImmutableMap.Builder<String, String> annotationBuilder = ImmutableMap.builder();
        AnnotationElement anno = element.annotations();
        if (anno != null) {
            annotationBuilder.putAll(anno.values());
        }
        this.annotations = annotationBuilder.build();
    }

    public String documentation() {
        return element.documentation();
    }

    public boolean hasJavadoc() {
        return JavadocUtil.hasJavadoc(this);
    }

    public String name() {
        return element.name();
    }

    public boolean oneWay() {
        return element.oneWay();
    }

    public Optional<ThriftType> returnType() {
        return Optional.fromNullable(returnType);
    }

    public List<Field> paramTypes() {
        return paramTypes;
    }

    public List<Field> exceptionTypes() {
        return exceptionTypes;
    }

    public ImmutableMap<String, String> annotations() {
        return annotations;
    }

    void link(Linker linker) {
        if (element.returnType().name().equals("void")) {
            returnType = ThriftType.VOID;
        } else {
            returnType = linker.resolveType(element.returnType());
        }

        for (Field field : paramTypes) {
            field.link(linker);
        }

        for (Field field : exceptionTypes) {
            field.link(linker);
        }
    }
}
