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
package com.microsoft.thrifty.gradle;

import org.gradle.api.file.SourceDirectorySet;

import javax.inject.Inject;

/**
 * The default implementation of {@link ThriftSourceDirectory}.  Backed by a {@link SourceDirectorySet}.
 */
class DefaultThriftSourceDirectory implements ThriftSourceDirectory {
    private final SourceDirectorySet sourceDirectorySet;

    private boolean didClearDefaults = false;

    @Inject
    public DefaultThriftSourceDirectory(SourceDirectorySet sourceDirectorySet) {
        this.sourceDirectorySet = sourceDirectorySet;
    }

    SourceDirectorySet getSourceDirectorySet() {
        return this.sourceDirectorySet;
    }

    @Override
    public void include(String pattern) {
        clearDefaults();
        sourceDirectorySet.include(pattern);
    }

    @Override
    public void exclude(String pattern) {
        clearDefaults();
        sourceDirectorySet.exclude(pattern);
    }

    private void clearDefaults() {
        if (didClearDefaults) {
            return;
        }

        didClearDefaults = true;
        sourceDirectorySet.getIncludes().clear();
        sourceDirectorySet.getExcludes().clear();
    }
}
