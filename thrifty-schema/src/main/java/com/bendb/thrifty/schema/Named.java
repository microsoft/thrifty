/*
 * Copyright (C) 2015-2016 Benjamin Bader
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

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
    public abstract ThriftType type();

    public boolean hasJavadoc() {
        return JavadocUtil.hasJavadoc(this);
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
