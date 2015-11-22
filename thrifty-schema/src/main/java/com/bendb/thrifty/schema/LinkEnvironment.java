package com.bendb.thrifty.schema;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class LinkEnvironment {
    /**
     * A mapping of files to their corresponding {@link Linker} instances.
     */
    private final Map<Program, Linker> linkers = new HashMap<>();

    /**
     * All errors encountered during linking.
     */
    private final List<String> errors = new ArrayList<>();

    @Nonnull
    Linker getLinker(Program program) {
        Linker linker = linkers.get(program);
        if (linker == null) {
            linker = new Linker(this, program);
            linkers.put(program, linker);
        }
        return linker;
    }

    void addError(String error) {
        errors.add(error);
    }

    boolean hasErrors() {
        return errors.size() > 0;
    }

    public ImmutableList<String> getErrors() {
        return ImmutableList.copyOf(errors);
    }
}
