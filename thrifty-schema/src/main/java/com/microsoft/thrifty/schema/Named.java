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

import java.util.Locale;
import java.util.Map;

/**
 * Represents a Thrift element that has a name and zero or more namespaces.
 */
public abstract class Named {
    private final String name;
    private final ImmutableMap<NamespaceScope, String> namespaces;

    protected Named(String name, Map<NamespaceScope, String> namespaces) {
        this.name = Preconditions.checkNotNull(name, "name");
        this.namespaces = ImmutableMap.copyOf(namespaces);
    }

    public String name() {
        return name;
    }

    public ImmutableMap<NamespaceScope, String> namespaces() {
        return namespaces;
    }

    public abstract String documentation();
    public abstract Location location();
    public abstract OldThriftType type();

    public boolean hasJavadoc() {
        return JavadocUtil.hasJavadoc(this);
    }

    /**
     * Returns {@code true} if the elements is documented to be deprecated,
     * {@code false} otherwise.
     */
    public boolean isDeprecated() {
        return hasJavadoc() && documentation().toLowerCase(Locale.US).contains("@deprecated");
    }

    /**
     * Returns the namespace specified for the given scope, or null.
     *
     * If the requested scope is not found, but a default namespace is present
     * (i.e. from {@link NamespaceScope#ALL}) then that will be returned.
     * There is thus no guarantee that the value returned will be syntactically
     * legal in the requested scope.
     */
    public String getNamespaceFor(NamespaceScope scope) {
        String namespace = namespaces().get(scope);
        if (namespace != null) {
            return namespace;
        }

        return namespaces().get(NamespaceScope.ALL);
    }
}
