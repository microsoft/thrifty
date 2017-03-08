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
import com.microsoft.thrifty.schema.parser.IncludeElement;
import com.microsoft.thrifty.schema.parser.NamespaceElement;
import com.microsoft.thrifty.schema.parser.ThriftFileElement;

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

        this.namespaces = element.namespaces().stream()
                .collect(ImmutableMap.toImmutableMap(
                        NamespaceElement::scope,
                        NamespaceElement::namespace));

        this.cppIncludes = element.includes().stream()
                .filter(IncludeElement::isCpp)
                .map(IncludeElement::path)
                .collect(toImmutableList());

        this.thriftIncludes = element.includes().stream()
                .filter(includeElement -> !includeElement.isCpp())
                .map(IncludeElement::path)
                .collect(toImmutableList());

        this.structs = element.structs().stream()
                .map(structElement -> new StructType(this, structElement))
                .collect(toImmutableList());

        this.typedefs = element.typedefs().stream()
                .map(typedefElement -> new TypedefType(this, typedefElement))
                .collect(toImmutableList());

        this.constants = element.constants().stream()
                .map(constElement -> new Constant(constElement, namespaces))
                .collect(toImmutableList());

        this.enums = element.enums().stream()
                .map(enumElement -> new EnumType(this, enumElement))
                .collect(toImmutableList());

        this.unions = element.unions().stream()
                .map(structElement -> new StructType(this, structElement))
                .collect(toImmutableList());

        this.exceptions = element.exceptions().stream()
                .map(structElement -> new StructType(this, structElement))
                .collect(toImmutableList());

        this.services = element.services().stream()
                .map(serviceElement -> new ServiceType(this, serviceElement))
                .collect(toImmutableList());
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
                reportDuplicateSymbol(loader.errorReporter(), oldValue, userType);
            }
        }

        this.symbols = ImmutableMap.copyOf(symbolMap);

        LinkedHashMap<String, Constant> constSymbolMap = new LinkedHashMap<>();
        for (Constant constant : constants()) {
            Constant oldValue = constSymbolMap.put(constant.name(), constant);
            if (oldValue != null) {
                reportDuplicateSymbol(loader.errorReporter(), oldValue, constant);
            }
        }

        this.constSymbols = ImmutableMap.copyOf(constSymbolMap);
    }

    private void reportDuplicateSymbol(
            ErrorReporter reporter,
            UserElement oldValue,
            UserElement newValue) {
        String message = "Duplicate symbols: " + oldValue.name() + " defined at "
                + oldValue.location() + " and at " + newValue.location();
        reporter.error(newValue.location(), message);
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
