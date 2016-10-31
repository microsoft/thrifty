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
import com.microsoft.thrifty.schema.parser.FieldElement;
import com.microsoft.thrifty.schema.parser.FunctionElement;

import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceMethod implements UserElement {
    private final UserElementMixin mixin;
    private final FunctionElement element;
    private final ImmutableList<Field> parameters;
    private final ImmutableList<Field> exceptions;
    private ThriftType returnType;

    ServiceMethod(FunctionElement element) {
        this.mixin = new UserElementMixin(element);
        this.element = element;

        ImmutableList.Builder<Field> paramsBuilder = ImmutableList.builder();
        for (FieldElement parameter : element.params()) {
            paramsBuilder.add(new Field(parameter));
        }
        this.parameters = paramsBuilder.build();

        ImmutableList.Builder<Field> exceptionsBuilder = ImmutableList.builder();
        for (FieldElement exception : element.exceptions()) {
            exceptionsBuilder.add(new Field(exception));
        }
        this.exceptions = exceptionsBuilder.build();
    }

    public ImmutableList<Field> parameters() {
        return parameters;
    }

    public ImmutableList<Field> exceptions() {
        return exceptions;
    }

    public ThriftType returnType() {
        return returnType;
    }

    public boolean oneWay() {
        return element.oneWay();
    }

    @Override
    public String name() {
        return mixin.name();
    }

    @Override
    public Location location() {
        return mixin.location();
    }

    @Override
    public String documentation() {
        return mixin.documentation();
    }

    @Override
    public ImmutableMap<String, String> annotations() {
        return mixin.annotations();
    }

    @Override
    public boolean hasJavadoc() {
        return mixin.hasJavadoc();
    }

    @Override
    public boolean isDeprecated() {
        return mixin.isDeprecated();
    }

    void link(Linker linker) {
        for (Field parameter : parameters) {
            parameter.link(linker);
        }

        for (Field exception : exceptions) {
            exception.link(linker);
        }

        returnType = linker.resolveType(element.returnType());
    }

    void validate(Linker linker) {
        if (oneWay() && !BuiltinType.VOID.equals(returnType)) {
            linker.addError(location(), "oneway methods may not have a non-void return type");
        }

        if (oneWay() && !exceptions.isEmpty()) {
            linker.addError(location(), "oneway methods may not throw exceptions");
        }

        Map<Integer, Field> fieldsById = new LinkedHashMap<>();
        for (Field param : parameters) {
            Field oldParam = fieldsById.put(param.id(), param);
            if (oldParam != null) {
                String fmt = "Duplicate parameters; param '%s' has the same ID (%s) as param '%s'";
                linker.addError(param.location(), String.format(fmt, param.name(), param.id(), oldParam.name()));

                fieldsById.put(oldParam.id(), oldParam);
            }
        }

        fieldsById.clear();
        for (Field exn : exceptions) {
            Field oldExn = fieldsById.put(exn.id(), exn);
            if (oldExn != null) {
                String fmt = "Duplicate exceptions; exception '%s' has the same ID (%s) as exception '%s'";
                linker.addError(exn.location(), String.format(fmt, exn.name(), exn.id(), oldExn.name()));

                fieldsById.put(oldExn.id(), oldExn);
            }
        }

        for (Field field : exceptions) {
            ThriftType type = field.type();
            if (type.isStruct()) {
                StructType struct = (StructType) type;
                if (struct.isException()) {
                    continue;
                }
            }

            linker.addError(field.location(), "Only exception types can be thrown");
        }
    }
}
