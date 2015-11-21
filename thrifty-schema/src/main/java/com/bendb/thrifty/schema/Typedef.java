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

    public String documentation() {
        return element.documentation();
    }

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
        ThriftType tt = linker.resolveType(element.oldName(), Linker.ResolveContext.TYPEDEF);
        if (tt == null || tt == ThriftType.PLACEHOLDER) {
            return false;
        }

        oldType = tt;
        type = ThriftType.typedefOf(oldType, element.newName());
        return false;
    }
}
