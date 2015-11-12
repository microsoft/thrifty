package com.bendb.thrifty;

import com.bendb.thrifty.parser.ServiceElement;
import com.google.common.collect.ImmutableList;

public final class Service {
    private final ServiceElement element;
    private final ImmutableList<ServiceMethod> methods;

    private Service(ServiceElement element) {
        this.element = element;
        this.methods = ImmutableList.of();
    }

    public String name() {
        return element.name();
    }

    public String documentation() {
        return element.documentation();
    }

    public ImmutableList<ServiceMethod> methods() {
        return methods;
    }
}
