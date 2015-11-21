package com.bendb.thrifty.schema;

import com.bendb.thrifty.schema.parser.ConstElement;

import java.util.Map;

public class Constant extends Named {
    private final ConstElement element;
    private ThriftType type;

    Constant(ConstElement element, Map<NamespaceScope, String> namespaces) {
        super(element.name(), namespaces);
        this.element = element;
    }

    @Override
    public ThriftType type() {
        return type;
    }

    public String documentation() {
        return element.documentation();
    }

    void link(Linker linker) {
        this.type = linker.resolveType(element.type());
    }
}
