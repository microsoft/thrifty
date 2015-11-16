package com.bendb.thrifty;

import com.bendb.thrifty.parser.IncludeElement;
import com.bendb.thrifty.parser.NamespaceElement;
import com.bendb.thrifty.parser.StructElement;
import com.bendb.thrifty.parser.ThriftFileElement;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public final class Program {
    private final ThriftFileElement element;
    private final ImmutableMap<NamespaceScope, String> namespaces;
    private final ImmutableList<String> cppImports;
    private final ImmutableList<String> thriftImports;
    private final ImmutableList<StructType> structs;


    private Program(ThriftFileElement element) {
        this.element = element;

        ImmutableMap.Builder<NamespaceScope, String> ns = ImmutableMap.builder();
        for (NamespaceElement namespaceElement : element.namespaces()) {
            ns.put(namespaceElement.scope(), namespaceElement.namespace());
        }
        namespaces = ns.build();

        ImmutableList.Builder<String> cppImports = ImmutableList.builder();
        ImmutableList.Builder<String> thriftImports = ImmutableList.builder();
        for (IncludeElement includeElement : element.includes()) {
            if (includeElement.isCpp()) {
                cppImports.add(includeElement.path());
            } else {
                thriftImports.add(includeElement.path());
            }
        }
        this.cppImports = cppImports.build();
        this.thriftImports = thriftImports.build();

        ImmutableList.Builder<StructType> structs = ImmutableList.builder();
        for (StructElement structElement : element.structs()) {
            StructType t = new StructType(
                    structElement,
                    ThriftType.get(structElement.name()),
                    namespaces);
            structs.add(t);
        }
        this.structs = structs.build();

        ImmutableList.Builder<UnionType> unions = ImmutableList.builder();
        for (StructElement structElement : element.unions()) {
            UnionType u = new UnionType()
        }
    }
}
