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
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.EnumMemberElement;

/**
 * A named member of an {@link EnumType}.
 */
public class EnumMember implements UserElement {
    private final UserElementMixin mixin;
    private final int value;

    EnumMember(EnumMemberElement element) {
        this.mixin = new UserElementMixin(element);
        this.value = element.value();
    }

    private EnumMember(Builder builder) {
        this.mixin = builder.mixin;
        this.value = builder.value;
    }

    public int value() {
        return value;
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

    @Override
    public String toString() {
        return name();
    }

    public static class Builder extends AbstractUserElementBuilder<EnumMember, Builder> {
        private int value;

        protected Builder(EnumMember member) {
            super(member.mixin);
            this.value = member.value;
        }

        public Builder value(int value) {
            Preconditions.checkArgument(value >= 0, "Enum values cannot be less than zero");
            this.value = value;
            return this;
        }

        @Override
        public EnumMember build() {
            return new EnumMember(this);
        }
    }
}
