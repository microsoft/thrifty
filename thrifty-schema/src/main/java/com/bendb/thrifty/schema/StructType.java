package com.bendb.thrifty.schema;

import com.bendb.thrifty.schema.parser.FieldElement;
import com.bendb.thrifty.schema.parser.StructElement;
import com.google.common.collect.ImmutableList;

import java.util.Map;

public class StructType extends Named {
    private final StructElement element;
    private final ThriftType type;
    private final ImmutableList<Field> fields;

    StructType(StructElement element, ThriftType type, Map<NamespaceScope, String> namespaces) {
        super(element.name(), namespaces);
        this.element = element;
        this.type = type;

        ImmutableList.Builder<Field> fieldsBuilder = ImmutableList.builder();
        for (FieldElement fieldElement : element.fields()) {
            fieldsBuilder.add(new Field(fieldElement));
        }
        this.fields = fieldsBuilder.build();
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

    public ImmutableList<Field> fields() {
        return fields;
    }

    public boolean isStruct() {
        return element.type() == StructElement.Type.STRUCT;
    }

    public boolean isUnion() {
        return element.type() == StructElement.Type.UNION;
    }

    public boolean isException() {
        return element.type() == StructElement.Type.EXCEPTION;
    }
}
