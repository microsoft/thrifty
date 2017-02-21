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

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * A Program is the set of elements declared in a Thrift file.  It
 * contains all types, namespaces, constants, and inclusions defined therein.
 */
public class Program {
    private final ThriftFileElement element;
    private final ImmutableMap<NamespaceScope, String> namespaces;
    private final ImmutableList<String> cppIncludes;
    private final ImmutableList<String> thriftIncludes;
    private final ImmutableList<TypedefType> typedefs;
    private final ImmutableList<Constant> constants;
    private final ImmutableList<EnumType> enums;
    private final ImmutableList<StructType> structs;
    private final ImmutableList<StructType> unions;
    private final ImmutableList<StructType> exceptions;
    private final ImmutableList<ServiceType> services;

    private ImmutableList<Program> includedPrograms;
    private ImmutableMap<String, UserType> symbols;
    private ImmutableMap<String, Constant> constSymbols;

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
            structs.add(new StructType(this, structElement));
        }
        this.structs = structs.build();

        ImmutableList.Builder<TypedefType> typedefs = ImmutableList.builder();
        for (TypedefElement typedefElement : element.typedefs()) {
            typedefs.add(new TypedefType(this, typedefElement));
        }
        this.typedefs = typedefs.build();

        ImmutableList.Builder<Constant> constants = ImmutableList.builder();
        for (ConstElement constElement : element.constants()) {
            constants.add(new Constant(constElement, namespaces));
        }
        this.constants = constants.build();

        ImmutableList.Builder<EnumType> enums = ImmutableList.builder();
        for (EnumElement enumElement : element.enums()) {
            enums.add(new EnumType(this, enumElement));
        }
        this.enums = enums.build();

        ImmutableList.Builder<StructType> unions = ImmutableList.builder();
        for (StructElement structElement : element.unions()) {
            unions.add(new StructType(this, structElement));
        }
        this.unions = unions.build();

        ImmutableList.Builder<StructType> exceptions = ImmutableList.builder();
        for (StructElement structElement : element.exceptions()) {
            exceptions.add(new StructType(this, structElement));
        }
        this.exceptions = exceptions.build();

        ImmutableList.Builder<ServiceType> services = ImmutableList.builder();
        for (ServiceElement serviceElement : element.services()) {
            services.add(new ServiceType(this, serviceElement));
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

    public ImmutableList<ServiceType> services() {
        return this.services;
    }

    public ImmutableList<TypedefType> typedefs() {
        return this.typedefs;
    }

    public ImmutableMap<String, UserType> symbols() {
        return this.symbols;
    }

    public ImmutableMap<String, Constant> constantMap() {
        return this.constSymbols;
    }

    /**
     * Get all named types declared in this Program.
     *
     * Note that this does not include {@link #constants()}, which are
     * not types.
     *
     * @return all user-defined types contained in this Program.
     */
    public Iterable<UserType> allUserTypes() {
        return Stream.of(enums, structs, unions, exceptions, services, typedefs)
                .flatMap(Collection::stream)
                .collect(toImmutableList());
    }

    /**
     * Loads this program's symbol table and list of included Programs.
     * @param loader
     * @param visited
     */
    void loadIncludedPrograms(Loader loader, Set<Program> visited) throws IOException {
        if (!visited.add(this)) {
            if (includedPrograms == null) {
                loader.errorReporter().error(location(), "Circular include; file includes itself transitively");
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

        LinkedHashMap<String, UserType> symbolMap = new LinkedHashMap<>();
        for (UserType userType : allUserTypes()) {
            UserType oldValue = symbolMap.put(userType.name(), userType);
            if (oldValue != null) {
                throw duplicateSymbol(userType.name(), oldValue, userType);
            }
        }

        this.symbols = ImmutableMap.copyOf(symbolMap);

        LinkedHashMap<String, Constant> constSymbolMap = new LinkedHashMap<>();
        for (Constant constant : constants()) {
            Constant oldValue = constSymbolMap.put(constant.name(), constant);
            if (oldValue != null) {
                throw duplicateSymbol(constant.name(), oldValue, constant);
            }
        }

        this.constSymbols = ImmutableMap.copyOf(constSymbolMap);
    }

    private IllegalStateException duplicateSymbol(String symbol, UserElement oldValue, UserElement newValue) {
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
