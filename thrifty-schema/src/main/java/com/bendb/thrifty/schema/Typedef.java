package com.bendb.thrifty.schema;

import com.bendb.thrifty.schema.parser.TypedefElement;

import java.util.Map;

public final class Typedef extends Named {
    private final TypedefElement element;
    private ThriftType oldType;
    private ThriftType type;

    Typedef(TypedefElement element, Map<NamespaceScope, String> namespaces) {
        super(element.newName(), namespaces);
        this.element = element;
    }

    @Override
    public ThriftType type() {
        return type;
    }

    @Override
    public String documentation() {
        return element.documentation();
    }

    @Override
    public Location location() {
        return element.location();
    }

    public String oldName() {
        return element.oldName();
    }

    public ThriftType oldType() {
        return oldType;
    }

    boolean link(Linker linker) {
        oldType = linker.resolveType(element.oldName());
        type = ThriftType.typedefOf(oldType, element.newName());
        return true;
    }
}
