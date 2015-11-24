package com.bendb.thrifty.schema;

import com.bendb.thrifty.schema.parser.FieldElement;
import com.bendb.thrifty.schema.parser.FunctionElement;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.List;

public final class ServiceMethod {
    private final FunctionElement element;

    private ThriftType returnType;
    private ImmutableList<Field> paramTypes;
    private ImmutableList<Field> exceptionTypes;

    public ServiceMethod(FunctionElement element) {
        this.element = element;

        ImmutableList.Builder<Field> params = ImmutableList.builder();
        for (FieldElement field : element.params()) {
            params.add(new Field(field));
        }
        this.paramTypes = params.build();

        ImmutableList.Builder<Field> exceptions = ImmutableList.builder();
        for (FieldElement field : element.exceptions()) {
            exceptions.add(new Field(field));
        }
        this.exceptionTypes = exceptions.build();
    }

    public String documentation() {
        return element.documentation();
    }

    public boolean hasJavadoc() {
        return JavadocUtil.hasJavadoc(this);
    }

    public String name() {
        return element.name();
    }

    public boolean oneWay() {
        return element.oneWay();
    }

    public Optional<ThriftType> returnType() {
        return Optional.fromNullable(returnType);
    }

    public List<Field> paramTypes() {
        return paramTypes;
    }

    public List<Field> exceptionTypes() {
        return exceptionTypes;
    }

    void link(Linker linker) {
        if (element.returnType().equals("void")) {
            returnType = ThriftType.VOID;
        } else {
            returnType = linker.resolveType(element.returnType());
        }

        for (Field field : paramTypes) {
            field.link(linker);
        }

        for (Field field : exceptionTypes) {
            field.link(linker);
        }
    }
}
