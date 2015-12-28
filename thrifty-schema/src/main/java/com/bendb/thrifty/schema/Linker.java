/*
 * Copyright (C) 2015 Benjamin Bader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bendb.thrifty.schema;

import com.bendb.thrifty.schema.parser.ListTypeElement;
import com.bendb.thrifty.schema.parser.MapTypeElement;
import com.bendb.thrifty.schema.parser.ScalarTypeElement;
import com.bendb.thrifty.schema.parser.SetTypeElement;
import com.bendb.thrifty.schema.parser.TypeElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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
    private final Map<String, ThriftType> typesByName = new LinkedHashMap<>();

    private boolean linking = false;
    private boolean linked = false;

    Linker(LinkEnvironment environment, Program program) {
        this.environment = environment;
        this.program = program;
    }

    void link() {
        if (!Thread.holdsLock(environment)) {
            throw new AssertionError("Linking must be locked on the environment!");
        }

        if (linking) {
            environment.addError("Circular link detected; " + program.location().path() + " includes itself");
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
            if (!environment.hasErrors()) {
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
            System.err.print("UNEXPECTED ERROR: " + ignored);
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
                        "No extension found for included file " + included.getAbsolutePath() + ","
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

        for (Service service : program.services()) {
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

        List<Typedef> typedefs = new LinkedList<>(program.typedefs());
        while (!typedefs.isEmpty()) {
            boolean atLeastOneResolved = false;
            Iterator<Typedef> iter = typedefs.iterator();

            while (iter.hasNext()) {
                Typedef typedef = iter.next();
                try {
                    typedef.link(this);
                    register(typedef);
                    atLeastOneResolved = true;
                    iter.remove();
                } catch (LinkFailureException ignored) {
                }
            }

            if (!atLeastOneResolved) {
                for (Typedef typedef : typedefs) {
                    environment.addError("Unresolvable typedef '" + typedef.name() + "' at " + typedef.location());
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
                environment.addError(
                        "Failed to resolve type " + e.getMessage() + " referenced at " + constant.location());
            }
        }
    }

    private void linkStructFields() {
        for (StructType structType : program.structs()) {
            try {
                structType.link(this);
            } catch (LinkFailureException e) {
                environment.addError(
                        "Failed to resolve type " + e.getMessage() + " referenced at " + structType.location());
            }
        }
    }

    private void linkUnionFields() {
        for (StructType union : program.unions()) {
            try {
                union.link(this);
            } catch (LinkFailureException e) {
                environment.addError(
                        "Failed to resolve type " + e.getMessage() + " referenced at " + union.location());
            }
        }
    }

    private void linkExceptionFields() {
        for (StructType exception : program.exceptions()) {
            try {
                exception.link(this);
            } catch (LinkFailureException e) {
                environment.addError(
                        "Failed to resolve type " + e.getMessage() + " referenced at " + exception.location());
            }
        }
    }

    private void linkServices() {
        for (Service service : program.services()) {
            try {
                service.link(this);
            } catch (LinkFailureException e) {
                environment.addError(
                        "Failed to resolve type " + e.getMessage() + " referenced at " + service.location());
            }
        }
    }

    private void validateConstants() {
        for (Constant constant : program.constants()) {
            try {
                constant.validate(this);
            } catch (IllegalStateException e) {
                environment.addError(e.getMessage());
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

    private void validateServices() {
        // TODO: Implement me
    }

    private void register(Named type) {
        typesByName.put(type.name(), type.type());
    }

    @Nonnull
    ThriftType resolveType(TypeElement type) {
        ThriftType tt = typesByName.get(type.name());
        if (tt != null) {
            return tt;
        }

        // NOTE:
        // TypeElement -> ThriftType resolution currently destroys any type-level
        // annotations.  We rely (nb: do we?) on types with identical names being
        // de-duplicated, which makes annotating single instances of a type problematic.
        // Luckily the only case I know of where this feature is useful in the Apache
        // implementation is the 'python.immutable' annotation for Python codegen,
        // which is not a concern.  Conceivably we could make use of type-annotations
        // to control e.g. collection implementation, but we can worry about that
        // in the future, if at all.
        if (type instanceof ListTypeElement) {
            ThriftType elementType = resolveType(((ListTypeElement) type).elementType());
            ThriftType listType = ThriftType.list(elementType);
            typesByName.put(type.name(), listType);
            return listType;
        } else if (type instanceof SetTypeElement) {
            ThriftType elementType = resolveType(((SetTypeElement) type).elementType());
            ThriftType setType = ThriftType.set(elementType);
            typesByName.put(type.name(), setType);
            return setType;
        } else if (type instanceof MapTypeElement) {
            MapTypeElement element = (MapTypeElement) type;
            ThriftType keyType = resolveType(element.keyType());
            ThriftType valueType = resolveType(element.valueType());
            ThriftType mapType = ThriftType.map(keyType, valueType);
            typesByName.put(type.name(), mapType);
            return mapType;
        } else if (type instanceof ScalarTypeElement) {
            tt = ThriftType.get(
                    type.name(),
                    Collections.<NamespaceScope, String>emptyMap()); // Any map will do

            if (tt.isBuiltin()) {
                return tt;
            }

            throw new LinkFailureException(type.name());
        } else {
            throw new AssertionError("Unexpected TypeElement: " + type.getClass());
        }
    }

    @Nullable
    Named lookupSymbol(String symbol) {
        Named named = program.symbols().get(symbol);
        if (named == null && symbol.indexOf('.') != -1) {
            // 'symbol' may be a qualified name for an included type
            ThriftType type = typesByName.get(symbol);
            if (type != null) {
                String name = type.name();
                for (Program includedProgram : program.includes()) {
                    named = includedProgram.symbols().get(name);
                    if (named != null) {
                        break;
                    }
                }
            }
        }
        return named;
    }

    @Nullable
    Named lookupSymbol(ThriftType type) {
        Queue<Program> ps = new ArrayDeque<>(1);
        ps.add(program);

        while (!ps.isEmpty()) {
            Program p = ps.remove();
            Named named = p.symbols().get(type.name());
            if (named != null) {
                return named;
            }

            ps.addAll(p.includes());
        }

        return null;
    }

    void addError(String error) {
        environment.addError(error);
    }

    private static class LinkFailureException extends RuntimeException {
        LinkFailureException() {
        }

        LinkFailureException(String message) {
            super(message);
        }
    }
}
