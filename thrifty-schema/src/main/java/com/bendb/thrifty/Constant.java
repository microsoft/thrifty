package com.bendb.thrifty;

import com.bendb.thrifty.parser.ConstElement;

import java.util.Map;

public class Constant extends Named {
    private final ConstElement element;

    Constant(ConstElement element, Map<NamespaceScope, String> namespaces) {
        super(element.name(), namespaces);
        this.element = element;
    }
}
