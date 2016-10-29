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
import com.microsoft.thrifty.schema.parser.TypeElement;
import com.microsoft.thrifty.schema.parser.TypedefElement;

import java.util.Map;
import java.util.Objects;

public class TypedefType extends UserType {
    private final TypeElement oldTypeElement;

    private ThriftType oldType;

    TypedefType(Program program, TypedefElement element) {
        super(program, new UserElementMixin(element));
        this.oldTypeElement = element.oldType();
    }

    private TypedefType(Builder builder) {
        super(builder);
        this.oldTypeElement = builder.oldTypeElement;
        this.oldType = builder.oldType;
    }

    void link(Linker linker) {
        this.oldType = linker.resolveType(oldTypeElement);
    }

    void validate(Linker linker) {
        if (oldType.isService()) {
            linker.addError(location(), "Cannot declare a typedef of a service");
        }

        if (oldType.equals(BuiltinThriftType.VOID)) {
            linker.addError(location(), "Cannot declare a typedef of void");
        }

        // Typedef cycles are validated during linking
    }

    public ThriftType oldType() {
        return oldType;
    }

    @Override
    public boolean isTypedef() {
        return true;
    }

    @Override
    public ThriftType getTrueType() {
        return oldType;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitTypedef(this);
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

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;

        TypedefType that = (TypedefType) o;

        return this.oldTypeElement.equals(that.oldTypeElement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), oldTypeElement);
    }

    public static final class Builder extends UserType.UserTypeBuilder<TypedefType, Builder> {
        private TypeElement oldTypeElement;
        private ThriftType oldType;

        Builder(TypedefType typedef) {
            super(typedef);
            oldTypeElement = typedef.oldTypeElement;
            oldType = typedef.oldType;
        }

        public Builder oldTypeElement(TypeElement oldTypeElement) {
            this.oldTypeElement = Preconditions.checkNotNull(oldTypeElement, "oldTypeElement");
            return this;
        }

        public Builder oldType(ThriftType oldType) {
            this.oldType = Preconditions.checkNotNull(oldType, "oldType");
            return this;
        }

        @Override
        public TypedefType build() {
            return new TypedefType(this);
        }
    }
}
