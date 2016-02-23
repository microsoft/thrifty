/*
 * Thrifty
 *
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED ON AN  *AS IS* BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING
 * WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE,
 * FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
 *
 * See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */
package com.microsoft.thrifty.schema;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.parser.ConstElement;
import com.microsoft.thrifty.schema.parser.EnumElement;
import com.microsoft.thrifty.schema.parser.IncludeElement;
import com.microsoft.thrifty.schema.parser.NamespaceElement;
import com.microsoft.thrifty.schema.parser.ServiceElement;
import com.microsoft.thrifty.schema.parser.StructElement;
import com.microsoft.thrifty.schema.parser.ThriftFileElement;
import com.microsoft.thrifty.schema.parser.TypedefElement;

import java.util.LinkedHashMap;
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
    private final ImmutableList<Constant> constants;
    private final ImmutableList<EnumType> enums;
    private final ImmutableList<StructType> structs;
    private final ImmutableList<StructType> unions;
    private final ImmutableList<StructType> exceptions;
    private final ImmutableList<Service> services;

    private ImmutableList<Program> includedPrograms;
    private ImmutableMap<String, Named> symbols;

    Program(ThriftFileElement element, FieldNamingPolicy fieldNamingPolicy) {
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
                    ThriftType.get(structElement.name(), namespaces),
                    namespaces,
                    fieldNamingPolicy);
            structs.add(t);
        }
        this.structs = structs.build();

        ImmutableList.Builder<Typedef> typedefs = ImmutableList.builder();
        for (TypedefElement typedefElement : element.typedefs()) {
            Typedef td = new Typedef(typedefElement, namespaces);
            typedefs.add(td);
        }
        this.typedefs = typedefs.build();

        ImmutableList.Builder<Constant> constants = ImmutableList.builder();
        for (ConstElement constElement : element.constants()) {
            Constant constant = new Constant(constElement, namespaces);
            constants.add(constant);
        }
        this.constants = constants.build();

        ImmutableList.Builder<EnumType> enums = ImmutableList.builder();
        for (EnumElement enumElement : element.enums()) {
            enums.add(new EnumType(
                    enumElement,
                    ThriftType.enumType(enumElement.name(), namespaces),
                    namespaces));
        }
        this.enums = enums.build();

        ImmutableList.Builder<StructType> unions = ImmutableList.builder();
        for (StructElement structElement : element.unions()) {
            StructType u = new StructType(
                    structElement,
                    ThriftType.get(structElement.name(), namespaces),
                    namespaces,
                    fieldNamingPolicy);
            unions.add(u);
        }
        this.unions = unions.build();

        ImmutableList.Builder<StructType> exceptions = ImmutableList.builder();
        for (StructElement structElement : element.exceptions()) {
            StructType u = new StructType(
                    structElement,
                    ThriftType.get(structElement.name(), namespaces),
                    namespaces,
                    fieldNamingPolicy);
            exceptions.add(u);
        }
        this.exceptions = exceptions.build();

        ImmutableList.Builder<Service> services = ImmutableList.builder();
        for (ServiceElement serviceElement : element.services()) {
            Service svc = new Service(
                    serviceElement,
                    ThriftType.get(serviceElement.name(), namespaces),
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

    public ImmutableList<Constant> constants() {
        return this.constants;
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

    public ImmutableMap<String, Named> symbols() {
        return this.symbols;
    }

    /**
     * Get all named elements declared in this Program.
     */
    public Iterable<Named> names() {
        // Some type-resolution subtlety eludes me.  I'd have thought that
        // Iterable<EnumType> is castable to Iterable<Named> (inheritance),
        // but the IDE claims otherwise.  So, instead of FluentIterable.<Named>from(enums),
        // we work around by making one from an empty Named array and appending.
        FluentIterable<Named> iter = FluentIterable.of(new Named[0]);
        return iter
                .append(enums)
                .append(structs)
                .append(unions)
                .append(exceptions)
                .append(services)
                .append(typedefs);
    }

    /**
     * Loads this program's symbol table and list of included Programs.
     * @param loader
     * @param visited
     */
    void loadIncludedPrograms(Loader loader, Set<Program> visited) {
        if (!visited.add(this)) {
            if (includedPrograms == null) {
                throw new IllegalStateException("Circular include: " + location().path()
                        + " includes itself transitively");
            }
            return;
        }

        Preconditions.checkState(this.includedPrograms == null, "Included programs already resolved");

        ImmutableList.Builder<Program> includes = ImmutableList.builder();
        for (String thriftImport : thriftIncludes) {
            Program included = loader.resolveIncludedProgram(location(), thriftImport);
            included.loadIncludedPrograms(loader, visited);
            includes.add(included);
        }

        this.includedPrograms = includes.build();

        LinkedHashMap<String, Named> symbolMap = new LinkedHashMap<>();
        for (Named named : names()) {
            Named oldValue = symbolMap.put(named.name(), named);
            if (oldValue != null) {
                throw duplicateSymbol(named.name(), oldValue, named);
            }
        }

        this.symbols = ImmutableMap.copyOf(symbolMap);
    }

    private IllegalStateException duplicateSymbol(String symbol, Named oldValue, Named newValue) {
        throw new IllegalStateException(
                "Duplicate symbols: '" + symbol + "' defined at "
                + oldValue.location() + " and at " + newValue.location());
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
