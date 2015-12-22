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

import com.bendb.thrifty.schema.parser.FunctionElement;
import com.bendb.thrifty.schema.parser.ServiceElement;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.Map;

public final class Service extends Named {
    private final ServiceElement element;
    private final ImmutableList<ServiceMethod> methods;
    private final ThriftType type;

    private ThriftType extendsService;

    Service(ServiceElement element, ThriftType type, Map<NamespaceScope, String> namespaces) {
        super(element.name(), namespaces);
        this.element = element;
        this.type = type;

        String extendsServiceName = element.extendsServiceName();
        if (extendsServiceName != null) {
            this.extendsService = ThriftType.get(extendsServiceName, namespaces);
        }

        ImmutableList.Builder<ServiceMethod> methods = ImmutableList.builder();
        for (FunctionElement functionElement : element.functions()) {
            ServiceMethod method = new ServiceMethod(functionElement);
            methods.add(method);
        }
        this.methods = methods.build();
    }

    @Override
    public ThriftType type() {
        return type;
    }

    public String documentation() {
        return element.documentation();
    }

    @Override
    public Location location() {
        return element.location();
    }

    public ImmutableList<ServiceMethod> methods() {
        return methods;
    }

    public ThriftType extendsService() {
        return extendsService;
    }

    void link(Linker linker) {
        String extendsName = element.extendsServiceName();
        if (!Strings.isNullOrEmpty(extendsName)) {
            extendsService = linker.resolveType(extendsName);
            // TODO: Validate that this is actually a service type
        }

        for (ServiceMethod method : methods) {
            method.link(linker);
        }
    }

    void validate(Linker linker) {
        // TODO: Implement
        throw new IllegalStateException("not implemented");
    }
}
