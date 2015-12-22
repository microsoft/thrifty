package com.bendb.thrifty.schema;

import com.bendb.thrifty.schema.parser.ConstValueElement;
import com.bendb.thrifty.schema.parser.FieldElement;

import javax.annotation.Nullable;

public final class Field {
    private final FieldElement element;
    private final FieldNamingPolicy fieldNamingPolicy;
    private ThriftType type;

    private transient String javaName;

    Field(FieldElement element, FieldNamingPolicy fieldNamingPolicy) {
        this.element = element;
        this.fieldNamingPolicy = fieldNamingPolicy;
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
        if (javaName == null) {
            javaName = fieldNamingPolicy.apply(element.name());
        }
        return javaName;
    }

    public boolean required() {
        return element.required();
    }

    public String documentation() {
        return element.documentation();
    }

    public boolean hasJavadoc() {
        return JavadocUtil.hasJavadoc(this);
    }

    public ConstValueElement defaultValue() {
        return element.constValue();
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

    void validate(Linker linker) {
        ConstValueElement value = element.constValue();
        if (value != null) {
            Constant.validate(linker, value, type);
        }
    }
}
