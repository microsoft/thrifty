package com.bendb.thrifty.schema;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

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

            validateConstants();
            validateStructs();
            validateExceptions();
            validateUnions();
            validateServices();

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
            constant.link(this);
        }
    }

    private void linkStructFields() {
        for (StructType structType : program.structs()) {
            for (Field field : structType.fields()) {
                field.link(this);
            }
        }
    }

    private void linkUnionFields() {
        for (StructType union : program.unions()) {
            for (Field field : union.fields()) {
                field.link(this);
            }
        }
    }

    private void linkExceptionFields() {
        for (StructType exception : program.exceptions()) {
            for (Field field : exception.fields()) {
                field.link(this);
            }
        }
    }

    private void linkServices() {
        for (Service service : program.services()) {
            service.link(this);
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

    }

    private void validateExceptions() {

    }

    private void validateUnions() {

    }

    private void validateServices() {

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
                    .split(",");
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

        tt = ThriftType.get(type);
        if (tt.isBuiltin()) {
            return tt;
        }

        throw new TypedefResolutionException(type);
    }

    @Nullable
    Named lookupSymbol(String symbol) {
        return program.symbols().get(symbol);
    }

    private static class LinkFailureException extends RuntimeException {
        LinkFailureException() {}

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
