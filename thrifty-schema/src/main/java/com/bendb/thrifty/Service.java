package com.bendb.thrifty;

import com.bendb.thrifty.parser.FunctionElement;
import com.bendb.thrifty.parser.ServiceElement;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.Map;

public final class Service extends Named {
    private final ServiceElement element;
    private final ImmutableList<ServiceMethod> methods;
    private final ThriftType type;

    private ThriftType extendsService;

    Service(ServiceElement element, ThriftType type, Map<NamespaceScope, String> namespaces) {
        super(element.name(), namespaces);
        this.element = element;
        this.type = type;
        this.extendsService = ThriftType.get(element.extendsServiceName());

        ImmutableList.Builder<ServiceMethod> methods = ImmutableList.builder();
        for (FunctionElement functionElement : element.functions()) {
            ServiceMethod method = new ServiceMethod(functionElement);
            methods.add(method);
        }
        this.methods = methods.build();
    }

    @Override
    public ThriftType type() {
        return type;
    }

    public String documentation() {
        return element.documentation();
    }

    public ImmutableList<ServiceMethod> methods() {
        return methods;
    }

    public ThriftType extendsService() {
        return extendsService;
    }

    void link(Linker linker) {
        String extendsName = element.extendsServiceName();
        if (!Strings.isNullOrEmpty(extendsName)) {
            extendsService = linker.resolveType(extendsName);
            // TODO: Validate that this is actually a service type
        }

        for (ServiceMethod method : methods) {
            method.link(linker);
        }
    }
}
