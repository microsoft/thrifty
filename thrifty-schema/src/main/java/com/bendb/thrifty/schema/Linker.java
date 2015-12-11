package com.bendb.thrifty.schema;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
            resolveTypdefs();

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

            typesByName.putAll(l.typesByName);
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

    private void resolveTypdefs() {
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
                } catch (TypedefResolutionException ignored) {
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
    ThriftType resolveType(String type) {
        ThriftType tt = typesByName.get(type);
        if (tt != null) {
            return tt;
        }

        if (type.startsWith("list<")) {
            String elementTypeName = type.substring(5, type.length() - 1).trim();
            ThriftType elementType = resolveType(elementTypeName);
            ThriftType listType = ThriftType.list(elementType);
            typesByName.put(type, listType);
            return listType;
        }

        if (type.startsWith("set<")) {
            String elementTypeName = type.substring(4, type.length() - 1).trim();
            ThriftType elementType = resolveType(elementTypeName);
            ThriftType setType = ThriftType.set(elementType);
            typesByName.put(type, setType);
            return setType;
        }

        if (type.startsWith("map<")) {
            String[] elementTypeNames = type.substring(4, type.length() - 1)
                    .trim()
                    .split(",", 2);
            if (elementTypeNames.length != 2) {
                // At this point, container type names should have already
                // been verified.
                throw new AssertionError("Malformed map type name: " + type);
            }

            ThriftType keyType = resolveType(elementTypeNames[0].trim());
            ThriftType valueType = resolveType(elementTypeNames[1].trim());
            ThriftType mapType = ThriftType.map(keyType, valueType);
            typesByName.put(mapType.name(), mapType);
            return mapType;
        }

        tt = ThriftType.get(type, Collections.<NamespaceScope, String>emptyMap()); // Any map will do
        if (tt.isBuiltin()) {
            return tt;
        }

        throw new LinkFailureException(type);
    }

    @Nullable
    Named lookupSymbol(String symbol) {
        return program.symbols().get(symbol);
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

    private static class TypedefResolutionException extends LinkFailureException {
        final String name;

        TypedefResolutionException(String name) {
            super("Failed to resolve type: " + name);
            this.name = name;
        }
    }
}
