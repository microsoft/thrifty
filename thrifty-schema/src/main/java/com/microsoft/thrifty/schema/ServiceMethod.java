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

        //returnType = linker.resolveType(element.returnType());
    }

    void validate(Linker linker) {
        // TODO: implement
    }
}
