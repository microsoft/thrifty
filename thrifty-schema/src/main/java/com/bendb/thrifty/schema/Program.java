package com.bendb.thrifty;

import com.bendb.thrifty.parser.EnumElement;
import com.bendb.thrifty.parser.IncludeElement;
import com.bendb.thrifty.parser.NamespaceElement;
import com.bendb.thrifty.parser.ServiceElement;
import com.bendb.thrifty.parser.StructElement;
import com.bendb.thrifty.parser.ThriftFileElement;
import com.bendb.thrifty.parser.TypedefElement;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Set;

/**
 * A Program is the set of elements declared in a Thrift file.  It
 * contains
 */
public final class Program {
    private final ThriftFileElement element;
    private final ImmutableMap<NamespaceScope, String> namespaces;
    private final ImmutableList<String> cppIncludes;
    private final ImmutableList<String> thriftIncludes;
    private final ImmutableList<Typedef> typedefs;
    private final ImmutableList<EnumType> enums;
    private final ImmutableList<StructType> structs;
    private final ImmutableList<StructType> unions;
    private final ImmutableList<StructType> exceptions;
    private final ImmutableList<Service> services;

    private ImmutableList<Program> includedPrograms;

    Program(ThriftFileElement element) {
        this.element = element;

        ImmutableMap.Builder<NamespaceScope, String> ns = ImmutableMap.builder();
        for (NamespaceElement namespaceElement : element.namespaces()) {
            ns.put(namespaceElement.scope(), namespaceElement.namespace());
        }
        namespaces = ns.build();

        ImmutableList.Builder<String> cppIncludes = ImmutableList.builder();
        ImmutableList.Builder<String> thriftIncludes = ImmutableList.builder();
        for (IncludeElement includeElement : element.includes()) {
            if (includeElement.isCpp()) {
                cppIncludes.add(includeElement.path());
            } else {
                thriftIncludes.add(includeElement.path());
            }
        }
        this.cppIncludes = cppIncludes.build();
        this.thriftIncludes = thriftIncludes.build();

        ImmutableList.Builder<StructType> structs = ImmutableList.builder();
        for (StructElement structElement : element.structs()) {
            StructType t = new StructType(
                    structElement,
                    ThriftType.get(structElement.name()),
                    namespaces);
            structs.add(t);
        }
        this.structs = structs.build();

        ImmutableList.Builder<Typedef> typedefs = ImmutableList.builder();
        for (TypedefElement typedefElement : element.typedefs()) {
            Typedef td = new Typedef(typedefElement, namespaces);
            typedefs.add(td);
        }
        this.typedefs = typedefs.build();

        ImmutableList.Builder<EnumType> enums = ImmutableList.builder();
        for (EnumElement enumElement : element.enums()) {
            enums.add(new EnumType(
                    enumElement,
                    ThriftType.get(enumElement.name()),
                    namespaces));
        }
        this.enums = enums.build();

        ImmutableList.Builder<StructType> unions = ImmutableList.builder();
        for (StructElement structElement : element.unions()) {
            StructType u = new StructType(
                    structElement,
                    ThriftType.get(structElement.name()),
                    namespaces);
            unions.add(u);
        }
        this.unions = unions.build();

        ImmutableList.Builder<StructType> exceptions = ImmutableList.builder();
        for (StructElement structElement : element.exceptions()) {
            StructType u = new StructType(
                    structElement,
                    ThriftType.get(structElement.name()),
                    namespaces);
            exceptions.add(u);
        }
        this.exceptions = exceptions.build();

        ImmutableList.Builder<Service> services = ImmutableList.builder();
        for (ServiceElement serviceElement : element.services()) {
            Service svc = new Service(
                    serviceElement,
                    ThriftType.get(serviceElement.name()),
                    namespaces);
            services.add(svc);
        }
        this.services = services.build();
    }

    public Location location() {
        return element.location();
    }

    public ImmutableMap<NamespaceScope, String> namespaces() {
        return this.namespaces;
    }

    public ImmutableList<String> cppIncludes() {
        return this.cppIncludes;
    }

    public ImmutableList<Program> includes() {
        return this.includedPrograms;
    }

    public ImmutableList<EnumType> enums() {
        return this.enums;
    }

    public ImmutableList<StructType> structs() {
        return this.structs;
    }

    public ImmutableList<StructType> unions() {
        return this.unions;
    }

    public ImmutableList<StructType> exceptions() {
        return this.exceptions;
    }

    public ImmutableList<Service> services() {
        return this.services;
    }

    public ImmutableList<Typedef> typedefs() {
        return this.typedefs;
    }

    /**
     * Get all named elements declared in this Program.
     */
    public Iterable<Named> names() {
        FluentIterable<Named> iter = FluentIterable.of(new Named[0]);
        return iter
                .append(typedefs)
                .append(enums)
                .append(structs)
                .append(unions)
                .append(exceptions)
                .append(services);
    }

    /**
     * Loads this program's symbol table and list of included Programs.
     * @param loader
     * @param visited
     */
    void loadIncludedPrograms(Loader loader, Set<Program> visited) {
        Preconditions.checkState(this.includedPrograms == null, "Included programs already resolved");

        if (!visited.add(this)) {
            if (includedPrograms == null) {
                throw new AssertionError("Circular include: " + location().path() + " includes itself transitively");
            }
            return;
        }

        ImmutableList.Builder<Program> includes = ImmutableList.builder();
        for (String thriftImport : thriftIncludes) {
            Program included = loader.resolveIncludedProgram(location(), thriftImport);
            includes.add(included);
        }
        this.includedPrograms = includes.build();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Program)) return false;

        // Programs are considered equal if they are derived from the same file.
        Location mine = location();
        Location other = ((Program) obj).location();
        return mine.base().equals(other.base()) && mine.path().equals(other.path());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(element.location().base(), element.location().path());
    }
}
