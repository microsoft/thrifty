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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.microsoft.thrifty.schema.parser.ServiceElement;
import com.microsoft.thrifty.schema.parser.TypeElement;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServiceType extends UserType {
    private final ImmutableList<ServiceMethod> methods;
    private final TypeElement extendsServiceType;

    // This is intentionally too broad - it is not legal for a service to extend
    // a non-service type, but if we've parsed that we need to keep the invalid
    // state long enough to catch it during link validation.
    private ThriftType extendsService;

    ServiceType(Program program, ServiceElement element) {
        super(program.namespaces(), new UserElementMixin(element));

        this.extendsServiceType = element.extendsService();
        this.methods = element.functions().stream()
                .map(ServiceMethod::new)
                .collect(ImmutableList.toImmutableList());
    }

    private ServiceType(Builder builder) {
        super(builder);
        this.methods = builder.methods;
        this.extendsServiceType = builder.extendsServiceType;
        this.extendsService = builder.extendsService;
    }

    public ImmutableList<ServiceMethod> methods() {
        return methods;
    }

    public ThriftType extendsService() {
        return extendsService;
    }

    @Override
    public boolean isService() {
        return true;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitService(this);
    }

    @Override
    public ThriftType withAnnotations(Map<String, String> annotations) {
        return toBuilder()
                .annotations(merge(this.annotations(), annotations))
                .build();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    void link(Linker linker) {
        for (ServiceMethod method : methods) {
            method.link(linker);
        }

        if (this.extendsServiceType != null) {
            this.extendsService = linker.resolveType(extendsServiceType);
        }
    }

    void validate(Linker linker) {
        // Validate the following properties:
        // 1. If the service extends a type, that the type is itself a service
        // 2. The service contains no duplicate methods, including those inherited from base types.
        // 3. All service methods themselves are valid.

        Map<String, ServiceMethod> methodsByName = new LinkedHashMap<>();

        Deque<ServiceType> hierarchy = new ArrayDeque<>();

        if (extendsService != null) {
            if (!(extendsService.isService())) {
                linker.addError(location(), "Base type '" + extendsService.name() + "' is not a service");
            }
        }

        // Assume base services have already been validated
        ThriftType baseType = extendsService;
        while (baseType != null) {
            if (!baseType.isService()) {
                break;
            }

            ServiceType svc = (ServiceType) baseType;
            hierarchy.add(svc);

            baseType = svc.extendsService;
        }


        while (!hierarchy.isEmpty()) {
            // Process from most- to least-derived services; that way, if there
            // is a name conflict, we'll report the conflict with the least-derived
            // class.
            ServiceType svc = hierarchy.remove();

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
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static final class Builder extends UserType.UserTypeBuilder<ServiceType, Builder> {
        private ImmutableList<ServiceMethod> methods;
        private TypeElement extendsServiceType;
        private ThriftType extendsService;

        Builder(ServiceType type) {
            super(type);
            this.methods = type.methods;
            this.extendsServiceType = type.extendsServiceType;
            this.extendsService = type.extendsService;
        }

        public Builder methods(List<ServiceMethod> methods) {
            Preconditions.checkNotNull(methods, "methods");
            this.methods = ImmutableList.copyOf(methods);
            return this;
        }

        public Builder extendsService(ThriftType extendsService) {
            this.extendsService = Preconditions.checkNotNull(extendsService, "extendsService");
            return this;
        }

        @Override
        public ServiceType build() {
            return new ServiceType(this);
        }
    }
}
