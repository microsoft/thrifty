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
import com.microsoft.thrifty.schema.parser.FunctionElement;
import com.microsoft.thrifty.schema.parser.ServiceElement;
import com.microsoft.thrifty.schema.parser.TypeElement;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Service extends Named {
    private final ServiceElement element;
    private final ImmutableList<ServiceMethod> methods;
    private final ThriftType type;
    private final ImmutableMap<String, String> annotations;

    private ThriftType extendsService;

    Service(
            ServiceElement element,
            ThriftType type,
            Map<NamespaceScope, String> namespaces,
            FieldNamingPolicy fieldNamingPolicy) {
        super(element.name(), namespaces);
        this.element = element;
        this.type = type;

        ImmutableList.Builder<ServiceMethod> methods = ImmutableList.builder();
        for (FunctionElement functionElement : element.functions()) {
            ServiceMethod method = new ServiceMethod(functionElement, fieldNamingPolicy);
            methods.add(method);
        }
        this.methods = methods.build();

        ImmutableMap.Builder<String, String> annotationBuilder = ImmutableMap.builder();
        AnnotationElement anno = element.annotations();
        if (anno != null) {
            annotationBuilder.putAll(anno.values());
        }
        this.annotations = annotationBuilder.build();
    }

    private Service(Builder builder) {
        super(builder.element.name(), builder.namespaces);
        this.element = builder.element;
        this.methods = builder.methods;
        this.type = builder.type;
        this.annotations = builder.annotations;
        this.extendsService = builder.extendsService;
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

    public ImmutableMap<String, String> annotations() {
        return annotations;
    }

    public Builder toBuilder() {
        return new Builder(element, methods, type, annotations, namespaces(), extendsService);
    }

    public static final class Builder {
        private ServiceElement element;
        private ImmutableList<ServiceMethod> methods;
        private ThriftType type;
        private ImmutableMap<String, String> annotations;
        private Map<NamespaceScope, String> namespaces;
        private ThriftType extendsService;

        Builder(ServiceElement element,
               ImmutableList<ServiceMethod> methods,
               ThriftType type,
               ImmutableMap<String, String> annotations,
               Map<NamespaceScope, String> namespaces,
               ThriftType extendsService) {
            this.element = element;
            this.methods = methods;
            this.type = type;
            this.annotations = annotations;
            this.namespaces = namespaces;
            this.extendsService = extendsService;
        }

        public Builder element(ServiceElement element) {
            if (element == null) {
                throw new NullPointerException("element may not be null");
            }
            this.element = element;
            return this;
        }

        public Builder methods(ImmutableList<ServiceMethod> methods) {
            if (methods == null) {
                throw new NullPointerException("methods may not be null");
            }
            this.methods = methods;
            return this;
        }

        public Builder type(ThriftType type) {
            this.type = type;
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

        public Builder extendsService(ThriftType extendsService) {
            this.extendsService = extendsService;
            return this;
        }

        public Service build() {
            return new Service(this);
        }
    }

    @Override
    public boolean isDeprecated() {
        return super.isDeprecated()
                || annotations.containsKey("deprecated")
                || annotations.containsKey("thrifty.deprecated");
    }

    void link(Linker linker) {
        TypeElement extendsType = element.extendsService();
        if (extendsType != null) {
            extendsService = linker.resolveType(extendsType);
            // TODO: Validate that this is actually a service type
        }

        for (ServiceMethod method : methods) {
            method.link(linker);
        }
    }

    void validate(Linker linker) {
        // Validate the following properties:
        // 1. If the service extends a type, that the type is itself a service
        // 2. The service contains no duplicate methods, including those inherited from base types.
        // 3. All service methods themselves are valid.

        Map<String, ServiceMethod> methodsByName = new LinkedHashMap<>();

        Deque<Service> hierarchy = new ArrayDeque<>();

        if (extendsService != null) {
            Named named = linker.lookupSymbol(extendsService());
            if (!(named instanceof Service)) {
                linker.addError(location(), "Base type '" + extendsService.name() + "' is not a service");
            }
        }

        // Assume base services have already been validated
        ThriftType baseType = extendsService;
        while (baseType != null) {
            Named named = linker.lookupSymbol(baseType);
            if (!(named instanceof Service)) {
                break;
            }

            Service svc = (Service) named;
            hierarchy.add(svc);

            baseType = svc.extendsService;
        }


        while (!hierarchy.isEmpty()) {
            // Process from most- to least-derived services; that way, if there
            // is a name conflict, we'll report the conflict with the least-derived
            // class.
            Service svc = hierarchy.remove();

            for (ServiceMethod serviceMethod : svc.methods()) {
                // Add the base-type method names to the map.  In this case,
                // we don't care about duplicates because the base types have
                // already been validated and we have already reported that error.
                methodsByName.put(serviceMethod.name(), serviceMethod);
            }
        }

        for (ServiceMethod method : methods) {
            ServiceMethod conflictingMethod = methodsByName.put(method.name(), method);
            if (conflictingMethod != null) {
                methodsByName.put(conflictingMethod.name(), conflictingMethod);

                linker.addError(method.location(), "Duplicate method; '" + method.name()
                        + "' conflicts with another method declared at " + conflictingMethod.location());
            }
        }

        for (ServiceMethod method : methods) {
            method.validate(linker);
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

        Service service = (Service) o;

        if (!element.equals(service.element)) {
            return false;
        }
        if (!methods.equals(service.methods)) {
            return false;
        }
        if (type != null ? !type.equals(service.type) : service.type != null) {
            return false;
        }
        if (annotations != null ? !annotations.equals(service.annotations) : service.annotations != null) {
            return false;
        }
        return extendsService != null ? extendsService.equals(service.extendsService) : service.extendsService == null;

    }

    @Override
    public int hashCode() {
        int result = element.hashCode();
        result = 31 * result + methods.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (annotations != null ? annotations.hashCode() : 0);
        result = 31 * result + (extendsService != null ? extendsService.hashCode() : 0);
        return result;
    }
}
