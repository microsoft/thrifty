/*
 * Thrifty
 *
 * Copyright (c) Benjamin Bader
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
package com.bendb.thrifty.gradle;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.configuration.ShowStacktrace;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;

/**
 * The Gradle task responsible for triggering generation of Thrifty source files.
 *
 * <p>In practice, just a thin layer around a Worker API action which does the heavy
 * lifting.
 */
public abstract class ThriftyTask extends SourceTask {
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @InputFiles
    public abstract ListProperty<File> getIncludePath();

    @Nested
    public abstract Property<ThriftOptions> getThriftOptions();

    @Internal
    public abstract Property<ShowStacktrace> getShowStacktrace();

    @Classpath
    public abstract ConfigurableFileCollection getThriftyClasspath();

    @Inject
    abstract public WorkerExecutor getWorkerExecutor();

    @TaskAction
    public void run() {
        WorkQueue workQueue = getWorkerExecutor().classLoaderIsolation(spec -> {
            spec.getClasspath().from(getThriftyClasspath());
        });

        workQueue.submit(GenerateThriftSourcesWorkAction.class, params -> {
            params.getOutputDirectory().set(getOutputDirectory());
            params.getIncludePath().set(getIncludePath());
            params.getSource().from(getSource());
            params.getThriftOptions().set(new SerializableThriftOptions(getThriftOptions().get()));
            params.getShowStacktrace().set(getShowStacktrace());
        });
    }
}
