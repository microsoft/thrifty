package com.bendb.thrifty.schema;

import com.bendb.thrifty.schema.parser.ConstElement;

import java.util.Map;

public class Constant extends Named {
    private final ConstElement element;

    Constant(ConstElement element, Map<NamespaceScope, String> namespaces) {
        super(element.name(), namespaces);
        this.element = element;
    }

    @Override
    public ThriftType type() {
        return null;
    }
}
