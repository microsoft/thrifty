/*
 * Copyright (C) 2015-2016 Benjamin Bader
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
