package com.bendb.thrifty;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
        // Typedefs require special handling
        for (Typedef typedef : program.typedefs()) {
            typesByName.put(typedef.name(), ThriftType.PLACEHOLDER);
        }

        // TODO: Surely there must be a more efficient way to do this.
        List<Typedef> typedefs = new LinkedList<>(program.typedefs());
        while (!typedefs.isEmpty()) {
            boolean atLeastOneResolved = false;
            Iterator<Typedef> iter = typedefs.iterator();
            while (iter.hasNext()) {
                Typedef typedef = iter.next();
                ThriftType tt = resolveType(typedef.oldName());
                if (tt != ThriftType.PLACEHOLDER) {
                    ThriftType td = ThriftType.typedefOf(tt, typedef.name());
                    typesByName.put(td.name(), td);
                    atLeastOneResolved = true;

                    iter.remove();
                }
            }

            if (!atLeastOneResolved) {
                for (Typedef typedef : typedefs) {
                    environment.addError("Unresolvable typedef '" + typedef.name() + "' at " + typedef.location());
                }
                linking = false;
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

        environment.addError("Failed to resolve type: " + type);
        throw new RuntimeException("Failed to resolve type: " + type);
    }
}
