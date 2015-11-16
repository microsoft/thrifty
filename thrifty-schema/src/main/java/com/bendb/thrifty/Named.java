package com.bendb.thrifty;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
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

    /**
     * Checks if a given {@link Named} has a name conflict with this instance.
     *
     * <p>
     * A name conflict exists if both items have the same {@link #name()}
     * and at least one namespace whose scope and name are equal.  Alternately,
     * a conflict exists if both items have the same name and no namespaces.
     * </p>
     * @param other the potentially-conflicting thrift element.
     * @return {@code true} if there is a conflict, otherwise {@code false}.
     */
    public boolean conflictsWith(Named other) {
        if (name.equals(other.name)) {
            if (namespaces.isEmpty() && other.namespaces.isEmpty()) {
                return true;
            }

            for (Map.Entry<NamespaceScope, String> entry : other.namespaces.entrySet()) {
                String ours = namespaces.get(entry.getKey());
                if (ours != null && ours.equals(entry.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets a list of all of this element's fully-qualified names.
     *
     * A fully-qualified name is a string of the form {@code "[scope]:[namespace]/[name]"}.
     * It
     * @return
     */
    public ImmutableList<String> getAllNames() {
        if (namespaces.isEmpty()) {
            return ImmutableList.of(name);
        }

        ImmutableList.Builder<String> builder = ImmutableList.builder();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<NamespaceScope, String> entry : namespaces.entrySet()) {
            sb.append(entry.getKey().name());
            sb.append(":");
            sb.append(entry.getValue());
            sb.append("/");
            sb.append(name);
            builder.add(sb.toString());
            sb.setLength(0);
        }
        return builder.build();
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
