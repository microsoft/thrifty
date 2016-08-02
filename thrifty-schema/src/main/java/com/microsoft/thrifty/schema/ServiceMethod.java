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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.AnnotationElement;
import com.microsoft.thrifty.schema.parser.FieldElement;
import com.microsoft.thrifty.schema.parser.FunctionElement;

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

    public Location location() {
        return element.location();
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

    void validate(Linker linker) {
        if (oneWay() && !ThriftType.VOID.equals(returnType)) {
            linker.addError(location(), "oneway methods may not have a non-void return type");
        }

        if (oneWay() && !exceptionTypes().isEmpty()) {
            linker.addError(location(), "oneway methods may not throw exceptions");
        }

        for (Field field : exceptionTypes) {
            ThriftType type = field.type();

            if (type.isBuiltin()) {
                linker.addError(field.location(), "Only exception types can be thrown");
            } else {
                Named named = linker.lookupSymbol(type);
                if (named == null) {
                    throw new AssertionError("wtf");
                }

                if (named instanceof StructType) {
                    StructType struct = (StructType) named;
                    if (!struct.isException()) {
                        linker.addError(field.location(), "Only exceptions can be thrown");
                    }
                } else {
                    linker.addError(field.location(), "Only exceptions can be thrown");
                }
            }
        }
    }
}
