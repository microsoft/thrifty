package com.bendb.thrifty.schema;

import com.bendb.thrifty.schema.parser.FieldElement;

import javax.annotation.Nullable;

public final class Field {
    private final FieldElement element;
    private ThriftType type;

    Field(FieldElement element) {
        this.element = element;
    }

    public int id() {
        Integer id = element.fieldId();
        if (id == null) {
            // IDs should have been definitively assigned during parse.
            // A missing ID at this point is a parser error.
            throw new AssertionError("Field ID should not be null");
        }
        return id;
    }

    public String name() {
        return element.name();
    }

    public boolean required() {
        return element.required();
    }

    public String documentation() {
        return element.documentation();
    }

    public Constant defaultValue() {
        throw new IllegalStateException("Not implemented");
    }

    public ThriftType type() {
        return type;
    }

    void setType(ThriftType type) {
        this.type = type;
    }

    @Nullable
    public String typedefName() {
        String name = null;
        if (type != null && type.isTypedef()) {
            name = type.name();
        }
        return name;
    }

    void link(Linker linker) {
        type = linker.resolveType(element.type());
    }
}
