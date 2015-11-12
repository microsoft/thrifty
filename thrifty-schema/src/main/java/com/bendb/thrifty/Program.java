package com.bendb.thrifty;

import com.bendb.thrifty.parser.NamespaceElement;
import com.bendb.thrifty.parser.ThriftFileElement;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public final class Program {
    private final ThriftFileElement element;
    private final ImmutableMap<NamespaceScope, String> namespaces;


    private Program(ThriftFileElement element) {
        this.element = element;

        ImmutableMap.Builder<NamespaceScope, String> ns = ImmutableMap.builder();
        for (NamespaceElement namespaceElement : element.namespaces()) {
            ns.put(namespaceElement.scope(), namespaceElement.namespace());
        }
        namespaces = ns.build();
    }
}
