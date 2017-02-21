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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.microsoft.thrifty.schema.parser.AnnotationElement;
import com.microsoft.thrifty.schema.parser.ListTypeElement;
import com.microsoft.thrifty.schema.parser.MapTypeElement;
import com.microsoft.thrifty.schema.parser.ScalarTypeElement;
import com.microsoft.thrifty.schema.parser.SetTypeElement;
import com.microsoft.thrifty.schema.parser.TypeElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * An object that can resolve the types of typdefs, struct fields, and service
 * method parameters based on types declared in Thrift {@link Program}s and their
 * transitive included Programs.
 *
 * In other words, a type-checker.
 */
class Linker {
    private final LinkEnvironment environment;
    private final Program program;
    private final ErrorReporter reporter;
    private final Map<String, ThriftType> typesByName = new LinkedHashMap<>();

    private boolean linking = false;
    private boolean linked = false;

    Linker(LinkEnvironment environment, Program program, ErrorReporter reporter) {
        this.environment = environment;
        this.program = program;
        this.reporter = reporter;
    }

    void link() {
        if (!Thread.holdsLock(environment)) {
            throw new AssertionError("Linking must be locked on the environment!");
        }

        if (linking) {
            reporter.error(program.location(), "Circular link detected; file transitively includes itself.");
            return;
        }

        if (linked) {
            return;
        }

        linking = true;

        try {
            linkIncludedPrograms();

            registerDeclaredTypes();

            // Next, figure out what types typedefs are aliasing.
            resolveTypedefs();

            // At this point, all types defined
            linkConstants();
            linkStructFields();
            linkExceptionFields();
            linkUnionFields();
            linkServices();

            // Only validate the schema if linking succeeded; no point otherwise.
            if (!reporter.hasError()) {
                validateTypedefs();
                validateConstants();
                validateStructs();
                validateExceptions();
                validateUnions();
                validateServices();
            }

            linked = !environment.hasErrors();
        } catch (LinkFailureException ignored) {
            // The relevant errors will have already been
            // added to the environment; just let the caller
            // handle them.
        } finally {
            linking = false;
        }
    }

    private void linkIncludedPrograms() {
        // First, link included programs and add their resolved types
        // to our own map
        for (Program p : program.includes()) {
            Linker l = environment.getLinker(p);
            l.link();

            File included = new File(p.location().base(), p.location().path());
            String name = included.getName();
            int ix = name.indexOf('.');
            if (ix == -1) {
                throw new AssertionError(
                        "No extension found for included file " + included.getAbsolutePath() + ", "
                        + "invalid include statement");
            }
            String prefix = name.substring(0, ix);

            for (Map.Entry<String, ThriftType> entry : l.typesByName.entrySet()) {
                // Include types defined directly within the included program,
                // but _not_ qualified names defined in programs that _it_ includes.
                // Include-chains like top.mid.bottom.SomeType are illegal.
                if (entry.getKey().indexOf('.') < 0) {
                    String qualifiedName = prefix + "." + entry.getKey();
                    typesByName.put(qualifiedName, entry.getValue());
                }
            }
        }

        // Linking included programs may have failed - if so, bail.
        if (environment.hasErrors()) {
            throw new LinkFailureException();
        }
    }

    private void registerDeclaredTypes() {
        // Register all types defined in this program
        for (StructType structType : program.structs()) {
            register(structType);
        }

        for (StructType union : program.unions()) {
            register(union);
        }

        for (StructType exception : program.exceptions()) {
            register(exception);
        }

        for (EnumType anEnum : program.enums()) {
            register(anEnum);
        }

        for (ServiceType service : program.services()) {
            register(service);
        }
    }

    private void resolveTypedefs() {
        // The strategy for resolving typedefs is:
        // Make a list of typedefs, then loop through it.  If the typedef is
        // successfully linked (i.e. its alias is resolvable), then remove it
        // from the list.  If not, skip it and continue through the list.
        //
        // Keep iterating over the list until it is either empty or contains only unresolvable
        // typedefs.  In the latter case, linking fails.
        // TODO: Surely there must be a more efficient way to do this.

        List<TypedefType> typedefs = new LinkedList<>(program.typedefs());
        while (!typedefs.isEmpty()) {
            boolean atLeastOneResolved = false;
            Iterator<TypedefType> iter = typedefs.iterator();

            while (iter.hasNext()) {
                TypedefType typedef = iter.next();
                try {
                    typedef.link(this);
                    register(typedef);
                    atLeastOneResolved = true;
                    iter.remove();
                } catch (LinkFailureException ignored) {
                }
            }

            if (!atLeastOneResolved) {
                for (TypedefType typedef : typedefs) {
                    reporter.error(typedef.location(), "Unresolvable typedef '" + typedef.name() + "'");
                }
                break;
            }
        }

        if (environment.hasErrors()) {
            throw new LinkFailureException();
        }
    }

    private void linkConstants() {
        for (Constant constant : program.constants()) {
            try {
                constant.link(this);
            } catch (LinkFailureException e) {
                reporter.error(constant.location(), "Failed to resolve type '" + e.getMessage() + "'");
            }
        }
    }

    private void linkStructFields() {
        for (StructType structType : program.structs()) {
            try {
                structType.link(this);
            } catch (LinkFailureException e) {
                reporter.error(structType.location(), "Failed to resolve type '" + e.getMessage() + "'");
            }
        }
    }

    private void linkUnionFields() {
        for (StructType union : program.unions()) {
            try {
                union.link(this);
            } catch (LinkFailureException e) {
                reporter.error(union.location(), "Failed to resolve type " + e.getMessage() + "'");
            }
        }
    }

    private void linkExceptionFields() {
        for (StructType exception : program.exceptions()) {
            try {
                exception.link(this);
            } catch (LinkFailureException e) {
                reporter.error(exception.location(), "Failed to resolve type " + e.getMessage() + "'");
            }
        }
    }

    private void linkServices() {
        for (ServiceType service : program.services()) {
            try {
                service.link(this);
            } catch (LinkFailureException e) {
                reporter.error(service.location(), "Failed to resolve type " + e.getMessage() + "'");
            }
        }
    }

    private void validateConstants() {
        for (Constant constant : program.constants()) {
            try {
                constant.validate(this);
            } catch (IllegalStateException e) {
                reporter.error(constant.location(), e.getMessage());
            }
        }
    }

    private void validateStructs() {
        for (StructType struct : program.structs()) {
            struct.validate(this);
        }
    }

    private void validateExceptions() {
        for (StructType exception : program.exceptions()) {
            exception.validate(this);
        }
    }

    private void validateUnions() {
        for (StructType union : program.unions()) {
            union.validate(this);
        }
    }

    private void validateTypedefs() {
        for (TypedefType typedef : program.typedefs()) {
            typedef.validate(this);
        }
    }

    private void validateServices() {
        // Services form an inheritance tree
        Set<ServiceType> visited = new LinkedHashSet<>(program.services().size());
        Multimap<ServiceType, ServiceType> parentToChildren = HashMultimap.create();
        Queue<ServiceType> servicesToValidate = new ArrayDeque<>(program.services().size());

        for (ServiceType service : program.services()) {
            // If this service extends another, add the parent -> child relationship to the multmap.
            // Otherwise, this is a root node, and should be added to the processing queue.
            ThriftType baseType = service.extendsService();
            if (baseType != null) {
                if (baseType.isService()) {
                    parentToChildren.put((ServiceType) baseType, service);
                } else {
                    // We know that this is an error condition; queue this type up for validation anyways
                    // so that any other errors lurking here can be reported.
                    servicesToValidate.add(service);
                }
            } else {
                // Root node - add it to the queue
                servicesToValidate.add(service);
            }
        }

        checkForCircularInheritance();

        while (!servicesToValidate.isEmpty()) {
            ServiceType service = servicesToValidate.remove();
            if (visited.add(service)) {
                service.validate(this);
                for (ServiceType child : parentToChildren.get(service)) {
                    servicesToValidate.add(child);
                }
            }
        }
    }

    private void checkForCircularInheritance() {
        Set<ThriftType> visited = new LinkedHashSet<>();
        List<ThriftType> stack = new ArrayList<>();
        Set<ThriftType> totalVisited = new LinkedHashSet<>();

        for (ServiceType svc : program.services()) {
            ThriftType type = svc;

            if (totalVisited.contains(type)) {
                // We've already validated this hierarchy
                continue;
            }

            visited.clear();
            stack.clear();
            visited.add(type);
            stack.add(type);

            type = svc.extendsService();
            while (type != null) {
                stack.add(type);
                if (!visited.add(type)) {
                    StringBuilder sb = new StringBuilder("Circular inheritance detected: ");
                    String arrow = " -> ";
                    for (ThriftType t : stack) {
                        sb.append(t.name());
                        sb.append(arrow);
                    }
                    sb.setLength(sb.length() - arrow.length());
                    addError(svc.location(), sb.toString());
                    break;
                }

                if (!type.isService()) {
                    // Service extends a non-service type?
                    // This is an error but is reported in
                    // Service#validate(Linker).
                    break;
                }

                type = ((ServiceType) type).extendsService();
            }

            totalVisited.addAll(visited);
        }
    }

    private void register(UserType type) {
        typesByName.put(type.name(), type);
    }

    @Nonnull
    ThriftType resolveType(TypeElement type) {
        AnnotationElement annotationElement = type.annotations();
        ImmutableMap<String, String> annotations = annotationElement != null
                ? annotationElement.values()
                : ImmutableMap.<String, String>of();

        ThriftType tt = typesByName.get(type.name());
        if (tt != null) {
            // If we are resolving e.g. the type of a field element, the type
            // may carry annotations that are not part of the canonical type.
            if (!annotations.isEmpty()) {
                return tt.withAnnotations(annotations);
            } else {
                return tt;
            }
        }

        if (type instanceof ListTypeElement) {
            ThriftType elementType = resolveType(((ListTypeElement) type).elementType());
            ThriftType listType = new ListType(elementType);
            typesByName.put(type.name(), listType);
            return listType.withAnnotations(annotations);
        } else if (type instanceof SetTypeElement) {
            ThriftType elementType = resolveType(((SetTypeElement) type).elementType());
            ThriftType setType = new SetType(elementType);
            typesByName.put(type.name(), setType);
            return setType.withAnnotations(annotations);
        } else if (type instanceof MapTypeElement) {
            MapTypeElement element = (MapTypeElement) type;
            ThriftType keyType = resolveType(element.keyType());
            ThriftType valueType = resolveType(element.valueType());
            ThriftType mapType = new MapType(keyType, valueType);
            typesByName.put(type.name(), mapType);
            return mapType.withAnnotations(annotations);
        } else if (type instanceof ScalarTypeElement) {
            // At this point, all user-defined types should have been registered.
            // If we are resolving a built-in type, then that's fine.  If not, then
            // we have an error.
            tt = BuiltinType.get(type.name());

            if (tt != null) {
                return tt.withAnnotations(annotations);
            }

            throw new LinkFailureException(type.name());
        } else {
            throw new AssertionError("Unexpected TypeElement: " + type.getClass());
        }
    }

    @Nullable
    Constant lookupConst(String symbol) {
        Constant constant = program.constantMap().get(symbol);
        if (constant == null) {
            // As above, 'symbol' may be a reference to an included
            // constant.
            int ix = symbol.indexOf('.');
            if (ix != -1) {
                String includeName = symbol.substring(0, ix);
                String qualifiedName = symbol.substring(ix + 1);
                String expectedPath = includeName + ".thrift";
                constant = program.includes().stream()
                        .filter(p -> p.location().path().equals(expectedPath))
                        .map(p -> p.constantMap().get(qualifiedName))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
            }
        }
        return constant;
    }

    void addError(Location location, String error) {
        reporter.error(location, error);
    }

    private static class LinkFailureException extends RuntimeException {
        LinkFailureException() {
        }

        LinkFailureException(String message) {
            super(message);
        }
    }
}
