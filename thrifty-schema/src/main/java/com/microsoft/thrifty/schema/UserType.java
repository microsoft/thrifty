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

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Base type of all user-defined Thrift IDL types, including structs, unions,
 * exceptions, services, and typedefs.
 */
public abstract class UserType extends ThriftType implements UserElement {
    private final ImmutableMap<NamespaceScope, String> namespaces;
    private final UserElementMixin mixin; // visible for subtype builders

    UserType(Program program, UserElementMixin mixin) {
        super(mixin.name());
        this.namespaces = program.namespaces();
        this.mixin = mixin;
    }

    protected UserType(UserTypeBuilder<? extends UserType, ? extends UserTypeBuilder<?, ?>> builder) {
        super(builder.mixin.name());
        this.namespaces = builder.namespaces;
        this.mixin = builder.mixin;
    }

    @Nullable
    public String getNamespaceFor(NamespaceScope namespace) {
        String ns = namespaces.get(namespace);
        if (ns == null && namespace != NamespaceScope.ALL) {
            ns = namespaces.get(NamespaceScope.ALL);
        }

        return ns;
    }

    public ImmutableMap<NamespaceScope, String> namespaces() {
        return namespaces;
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
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;

        UserType that = (UserType) o;
        if (!mixin.equals(that.mixin)) return false;
        if (!namespaces.equals(that.namespaces)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mixin, namespaces);
    }

    public abstract static class UserTypeBuilder<
            TType extends UserType,
            TBuilder extends UserTypeBuilder<TType, TBuilder>> extends AbstractUserElementBuilder<TType, TBuilder> {

        private ImmutableMap<NamespaceScope, String> namespaces;

        UserTypeBuilder(TType type) {
            super(((UserType) type).mixin);
            this.namespaces = ((UserType) type).namespaces;
        }

        @SuppressWarnings("unchecked")
        public TBuilder namespaces(Map<NamespaceScope, String> namespaces) {
            this.namespaces = ImmutableMap.copyOf(Preconditions.checkNotNull(namespaces, "namespaces"));
            return (TBuilder) this;
        }
    }
}
