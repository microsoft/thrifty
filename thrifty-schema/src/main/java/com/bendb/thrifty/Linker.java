package com.bendb.thrifty;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.*;

class Linker {
    private final List<Program> programs;
    private final Map<String, ThriftType> typesByName = new LinkedHashMap<>();

    private Stack<Program> currentlyResolving = new Stack<>();

    Linker(List<Program> programs) {
        this.programs = new ArrayList<>(programs);
    }

    void link() {
        for (Program program : programs) {
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

            // Typedef values will be registered with throwaway ThriftTypes;
            //
            for (Typedef typedef : program.typedefs()) {
                typesByName.put(typedef.name(), ThriftType.PLACEHOLDER);
            }
        }

        Set<Typedef> deferred = new HashSet<>();
        Queue<Typedef> toResolve = new ArrayDeque<>();
        Multimap<String, Typedef> unresolved = HashMultimap.create();
        for (Program program : programs) {
            for (Typedef typedef : program.typedefs()) {
                toResolve.add(typedef);
            }
        }

        while (!toResolve.isEmpty()) {
            Typedef td = toResolve.remove();

            if (td.link(this)) {
                typesByName.put(td.name(), td.type());

                Collection<Typedef> typedefs = unresolved.removeAll(td.name());
            } else {
                unresolved.put(td.oldName(), td);
            }
        }


    }

    private void register(Named type) {
        typesByName.put(type.name(), type.type());
    }

    ThriftType resolveType(String type) {
        ThriftType tt = typesByName.get(type);
        if (tt != null) {
            return tt;
        }

        tt = ThriftType.get(type);
        if (tt.isBuiltin()) {
            return tt;
        }

        if (tt.isList()) {
            ThriftType.ListType lt = (ThriftType.ListType) tt;
            lt.elementType()
        } else if (tt.isSet()) {

        } else if (tt.isMap()) {

        }
    }

    private void resolveTypes() {
        Queue<>
    }

    private abstract static class Request {
        abstract String
    }

    private static class DeferredRequest extends Request {
        Request originalRequest;

        DeferredRequest(Request originalRequest) {
            this.originalRequest = originalRequest;
        }
    }
}
