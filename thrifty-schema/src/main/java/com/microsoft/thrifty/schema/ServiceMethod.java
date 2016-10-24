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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.AnnotationElement;
import com.microsoft.thrifty.schema.parser.FieldElement;
import com.microsoft.thrifty.schema.parser.FunctionElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ServiceMethod {
    private final FunctionElement element;
    private final ImmutableList<Field> paramTypes;
    private final ImmutableList<Field> exceptionTypes;
    private final ImmutableMap<String, String> annotations;

    private ThriftType returnType;

    public ServiceMethod(FunctionElement element, FieldNamingPolicy fieldNamingPolicy) {
        this.element = element;

        ImmutableList.Builder<Field> params = ImmutableList.builder();
        for (FieldElement field : element.params()) {
            params.add(new Field(field, fieldNamingPolicy));
        }
        this.paramTypes = params.build();

        ImmutableList.Builder<Field> exceptions = ImmutableList.builder();
        for (FieldElement field : element.exceptions()) {
            exceptions.add(new Field(field, fieldNamingPolicy));
        }
        this.exceptionTypes = exceptions.build();

        ImmutableMap.Builder<String, String> annotationBuilder = ImmutableMap.builder();
        AnnotationElement anno = element.annotations();
        if (anno != null) {
            annotationBuilder.putAll(anno.values());
        }
        this.annotations = annotationBuilder.build();
    }

    private ServiceMethod(Builder builder) {
        this.element = builder.element;
        this.paramTypes = builder.paramTypes;
        this.exceptionTypes = builder.exceptionTypes;
        this.annotations = builder.annotations;
        this.returnType = builder.returnType;
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

    public ThriftType returnType() {
        return returnType;
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

    public Builder toBuilder() {
        return new Builder(element, paramTypes, exceptionTypes, annotations, returnType);
    }

    public static final class Builder {
        private FunctionElement element;
        private ImmutableList<Field> paramTypes;
        private ImmutableList<Field> exceptionTypes;
        private ImmutableMap<String, String> annotations;
        private ThriftType returnType;

        Builder(FunctionElement element,
                       ImmutableList<Field> paramTypes,
                       ImmutableList<Field> exceptionTypes,
                       ImmutableMap<String, String> annotations,
                       ThriftType thriftType) {
            this.element = element;
            this.paramTypes = paramTypes;
            this.exceptionTypes = exceptionTypes;
            this.annotations = annotations;
            this.returnType = thriftType;
        }

        public Builder element(FunctionElement element) {
            if (element == null) {
                throw new NullPointerException("element can't be null.");
            }
            this.element = element;
            return this;
        }

        public Builder paramTypes(ImmutableList<Field> paramTypes) {
            this.paramTypes = paramTypes;
            return this;
        }

        public Builder exceptionTypes(ImmutableList<Field> exceptionTypes) {
            this.exceptionTypes = exceptionTypes;
            return this;
        }

        public Builder annotations(ImmutableMap<String, String> annotations) {
            this.annotations = annotations;
            return this;
        }

        public Builder returnType(ThriftType returnType) {
            this.returnType = returnType;
            return this;
        }

        public ServiceMethod build() {
            return new ServiceMethod(this);
        }
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

        Map<Integer, Field> fieldsById = new HashMap<>();
        for (Field param : paramTypes) {
            Field oldParam = fieldsById.put(param.id(), param);
            if (oldParam != null) {
                String fmt = "Duplicate parameters; param '%s' has the same ID (%s) as param '%s'";
                linker.addError(param.location(), String.format(fmt, param.name(), param.id(), oldParam.name()));

                fieldsById.put(oldParam.id(), oldParam);
            }
        }

        fieldsById.clear();
        for (Field exn : exceptionTypes) {
            Field oldExn = fieldsById.put(exn.id(), exn);
            if (oldExn != null) {
                String fmt = "Duplicate exceptions; exception '%s' has the same ID (%s) as exception '%s'";
                linker.addError(exn.location(), String.format(fmt, exn.name(), exn.id(), oldExn.name()));

                fieldsById.put(oldExn.id(), oldExn);
            }
        }

        for (Field field : exceptionTypes) {
            ThriftType type = field.type();

            Named named = linker.lookupSymbol(type);
            if (named instanceof StructType) {
                StructType struct = (StructType) named;
                if (struct.isException()) {
                    continue;
                }
            }

            linker.addError(field.location(), "Only exception types can be thrown");
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

        ServiceMethod that = (ServiceMethod) o;

        if (!element.equals(that.element)) {
            return false;
        }
        if (paramTypes != null ? !paramTypes.equals(that.paramTypes) : that.paramTypes != null) {
            return false;
        }
        if (exceptionTypes != null ? !exceptionTypes.equals(that.exceptionTypes) : that.exceptionTypes != null) {
            return false;
        }
        if (annotations != null ? !annotations.equals(that.annotations) : that.annotations != null) {
            return false;
        }
        return returnType != null ? returnType.equals(that.returnType) : that.returnType == null;

    }

    @Override
    public int hashCode() {
        int result = element.hashCode();
        result = 31 * result + (paramTypes != null ? paramTypes.hashCode() : 0);
        result = 31 * result + (exceptionTypes != null ? exceptionTypes.hashCode() : 0);
        result = 31 * result + (annotations != null ? annotations.hashCode() : 0);
        result = 31 * result + (returnType != null ? returnType.hashCode() : 0);
        return result;
    }
}
