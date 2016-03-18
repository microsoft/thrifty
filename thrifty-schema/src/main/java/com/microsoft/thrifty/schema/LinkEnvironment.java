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

    private final ErrorReporter errorReporter;

    LinkEnvironment(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @Nonnull
    Linker getLinker(Program program) {
        Linker linker = linkers.get(program);
        if (linker == null) {
            linker = new Linker(this, program, errorReporter);
            linkers.put(program, linker);
        }
        return linker;
    }

    ErrorReporter reporter() {
        return errorReporter;
    }

    boolean hasErrors() {
        return errorReporter.hasError();
    }

    public ImmutableList<String> getErrors() {
        if (!hasErrors()) {
            return ImmutableList.of();
        }

        ImmutableList<ErrorReporter.Report> reports = errorReporter.reports();
        List<String> errors = new ArrayList<>(reports.size());

        for (ErrorReporter.Report report : reports) {
            String level = report.level().name();
            String msg = level + ": " + report.message() + "(" + report.location() + ")";
            errors.add(msg);
        }

        return ImmutableList.copyOf(errors);
    }
}
