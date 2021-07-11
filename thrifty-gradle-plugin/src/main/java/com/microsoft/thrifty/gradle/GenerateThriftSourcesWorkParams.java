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

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.configuration.ShowStacktrace;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkParameters;

import java.io.File;

/**
 * Encapsulates all input to Thrifty compilation, in a {@link java.io.Serializable Serializable}
 * form.
 */
@SuppressWarnings("UnstableApiUsage")
public interface GenerateThriftSourcesWorkParams extends WorkParameters {
    DirectoryProperty getOutputDirectory();
    ListProperty<File> getIncludePath();
    ConfigurableFileCollection getSource();
    Property<SerializableThriftOptions> getThriftOptions();
    Property<ShowStacktrace> getShowStacktrace();
}
