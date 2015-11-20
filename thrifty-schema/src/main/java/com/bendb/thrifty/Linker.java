package com.bendb.thrifty;

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

        // First, link included programs and add their resolved types
        // to our own map
        for (Program p : program.includes()) {
            Linker l = environment.getLinker(p);
            l.link();

            typesByName.putAll(l.typesByName);
        }

        // Linking included programs may have failed - if so, bail.
        if (environment.hasErrors()) {
            linking = false;
            return;
        }

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

        try {
            resolveTypdefs();

            linkStructFields();
            linkExceptionFields();
            linkUnionFields();
            linkServices();
        } catch (Exception e) {
            linking = false;
            return;
        }

        // Now that types are registered and and typedefs are resolved, we can
        // link fields of struct types and parameters of service methods.

        linking = false;
        linked = true;
    }

    private void resolveTypdefs() {
        // Typedefs require special handling.
        for (Typedef typedef : program.typedefs()) {
            typesByName.put(typedef.name(), ThriftType.PLACEHOLDER);
        }

        // TODO: Surely there must be a more efficient way to do this.

        // The strategy for resolving typedefs is:
        // First, register all typedefs with a placeholder ThriftType.
        // Next, make a list of typedefs, then loop through it.  A
        // typedef will resolve in one of three ways:
        // 1. to a known type - perfect!  remove it from the list.
        // 2. to a placeholder - it's a typedef of an unresolved typedef.  Keep going, try again later.
        // 3. to null - we will never be able to resolve this.  fail.
        //
        // Keep iterating over the list until it is either empty or contains only unresolvable
        // typedefs.  In the latter case, linking fails.

        List<Typedef> typedefs = new LinkedList<>(program.typedefs());
        List<Typedef> unresolvableTypedefs = new ArrayList<>();
        while (!typedefs.isEmpty()) {
            boolean atLeastOneResolved = false;
            unresolvableTypedefs.clear();
            Iterator<Typedef> iter = typedefs.iterator();

            while (iter.hasNext()) {
                Typedef typedef = iter.next();
                ThriftType tt = resolveType(typedef.oldName(), ResolveContext.TYPEDEF);
                if (tt == null) {
                    unresolvableTypedefs.add(typedef);
                } else if (tt == ThriftType.PLACEHOLDER) {
                    // This is a typedef of a typedef, the latter of which
                    // has not yet been linked.  Skip it for now.
                } else {
                    ThriftType td = ThriftType.typedefOf(tt, typedef.name());
                    typesByName.put(td.name(), td);
                    atLeastOneResolved = true;

                    iter.remove();
                }
            }

            if (unresolvableTypedefs.size() > 0) {
                for (Typedef typedef : unresolvableTypedefs) {
                    environment.addError("Unresolvable typedef '" + typedef.name() + "' at " + typedef.location());
                }
                return;
            }

            if (!atLeastOneResolved) {
                for (Typedef typedef : typedefs) {
                    environment.addError("Unresolvable typedef '" + typedef.name() + "' at " + typedef.location());
                }
                return;
            }
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

    private void register(Named type) {
        typesByName.put(type.name(), type.type());
    }

    @Nonnull
    ThriftType resolveType(String type) {
        //noinspection ConstantConditions ResolveContext.NORMAL guarantees that the result is non-null or exceptional.
        return resolveType(type, ResolveContext.NORMAL);
    }

    @Nullable
    ThriftType resolveType(String type, ResolveContext context) {
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
                environment.addError("Malformed map type name: " + type);
                throw new RuntimeException("Malformed map type name: " + type);
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

        if (context.throwOnUnresolved()) {
            environment.addError("Failed to resolve type: " + type);
            throw new RuntimeException("Failed to resolve type: " + type);
        } else {
            return null;
        }
    }

    private enum ResolveContext {
        NORMAL,
        TYPEDEF {
            @Override
            boolean throwOnUnresolved() {
                return false;
            }
        };

        boolean throwOnUnresolved() {
            return true;
        }
    }
}
