package com.bendb.thrifty;

import com.bendb.thrifty.parser.FieldElement;
import com.bendb.thrifty.parser.FunctionElement;
import com.google.common.base.Optional;

import java.util.List;

public final class ServiceMethod {
    private final FunctionElement element;

    private ThriftType returnType;
    private List<ThriftType> paramTypes;
    private List<ThriftType> exceptionTypes;

    public ServiceMethod(FunctionElement element) {
        this.element = element;
    }

    public String documentation() {
        return element.documentation();
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

    public List<ThriftType> paramTypes() {
        return paramTypes;
    }

    public List<ThriftType> exceptionTypes() {
        return exceptionTypes;
    }

    void link(Linker linker) {
        // TODO: finish
        if (element.returnType().equals("void")) {
            returnType = ThriftType.VOID;
        } else {
            returnType = linker.resolveType(element.returnType());
        }

        for (FieldElement paramElement : element.params()) {

        }
    }
}
