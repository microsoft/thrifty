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
import com.microsoft.thrifty.schema.parser.FunctionElement;
import com.microsoft.thrifty.schema.parser.ServiceElement;
import com.microsoft.thrifty.schema.parser.TypeElement;

import java.util.List;
import java.util.Map;

public class ServiceType extends UserType {
    private final ImmutableList<ServiceMethod> methods;
    private final TypeElement extendsServiceType;
    private ThriftType extendsService;

    ServiceType(Program program, ServiceElement element) {
        super(program, new UserElementMixin(element));

        this.extendsServiceType = element.extendsService();

        ImmutableList.Builder<ServiceMethod> methodListBuilder = ImmutableList.builder();
        for (FunctionElement functionElement : element.functions()) {
            methodListBuilder.add(new ServiceMethod(functionElement));
        }
        this.methods = methodListBuilder.build();
    }

    private ServiceType(Builder builder) {
        super(builder);
        this.methods = builder.methods;
        this.extendsServiceType = builder.extendsServiceType;
        this.extendsService = builder.extendsService;
    }

    public ThriftType extendsService() {
        return extendsService;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitService(this);
    }

    @Override
    ThriftType withAnnotations(Map<String, String> annotations) {
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

        this.extendsService = linker.resolveType(extendsServiceType);
    }

    void validate(Linker linker) {

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
