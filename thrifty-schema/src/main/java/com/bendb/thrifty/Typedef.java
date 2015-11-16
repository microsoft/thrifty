package com.bendb.thrifty;

import com.bendb.thrifty.parser.TypedefElement;

import java.util.Map;

public final class Typedef extends Named {
    private final TypedefElement element;

    Typedef(TypedefElement element, Map<NamespaceScope, String> namespaces) {
        super(element.newName(), namespaces);
        this.element = element;
    }

    public String documentation() {
        return element.documentation();
    }

    public String oldName() {
        return element.oldName();
    }
}
