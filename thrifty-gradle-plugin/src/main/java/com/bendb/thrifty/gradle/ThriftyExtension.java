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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.gradle.api.Action;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implements the 'thrifty' Gradle extension.
 *
 * <p>This is the public interface of our Gradle plugin to build scripts.  Renaming
 * or removing a method is a breaking change!
 */
public abstract class ThriftyExtension {
    private static final String DEFAULT_SOURCE_DIR = Joiner.on(File.separator).join("src", "main", "thrift");
    private static final String DEFAULT_OUTPUT_DIR = Joiner.on(File.separator).join("generated", "sources", "thrifty");

    private final ObjectFactory objects;
    private final ProjectLayout layout;

    private final ListProperty<Directory> includePathEntries;
    private final ListProperty<DefaultThriftSourceDirectory> sources;
    private final Property<ThriftOptions> thriftOptions;
    private final DirectoryProperty outputDirectory;
    private final Property<String> thriftyVersion;

    @Inject
    public ThriftyExtension(ObjectFactory objects, ProjectLayout layout) {
        this.objects = objects;
        this.layout = layout;

        this.includePathEntries = objects.listProperty(Directory.class);
        this.sources = objects.listProperty(DefaultThriftSourceDirectory.class)
                .convention(Collections.singletonList(
                        new DefaultThriftSourceDirectory(
                                getDefaultSourceDirectorySet())));
        this.thriftOptions = objects.property(ThriftOptions.class).convention(new KotlinThriftOptions());
        this.outputDirectory = objects.directoryProperty().convention(layout.getBuildDirectory().dir(DEFAULT_OUTPUT_DIR));
        this.thriftyVersion = objects.property(String.class);
    }

    private SourceDirectorySet getDefaultSourceDirectorySet() {
        return (SourceDirectorySet) objects.sourceDirectorySet("thrift-sources", "Thrift Sources")
                .srcDir(DEFAULT_SOURCE_DIR)
                .include("**/*.thrift");
    }

    Provider<List<Directory>> getIncludePathEntries() {
        return includePathEntries;
    }

    Provider<List<File>> getIncludePath() {
        return getIncludePathEntries().map(
            dirs -> dirs.stream()
                .map(Directory::getAsFile)
                .collect(Collectors.toList())
        );
    }

    Provider<List<DefaultThriftSourceDirectory>> getSources() {
        return sources;
    }

    Provider<List<SourceDirectorySet>> getSourceDirectorySets() {
        return getSources().map(
            ss -> ss.stream()
                .map(DefaultThriftSourceDirectory::getSourceDirectorySet)
                .collect(Collectors.toList()));
    }

    Provider<ThriftOptions> getThriftOptions() {
        return thriftOptions;
    }

    Provider<Directory> getOutputDirectory() {
        return outputDirectory;
    }

    public Property<String> getThriftyVersion() {
        return thriftyVersion;
    }

    public ThriftSourceDirectory sourceDir(String path) {
        SourceDirectorySet sd = objects.sourceDirectorySet("thrift-sources", "Thrift Sources");
        sd.srcDir(path);

        DefaultThriftSourceDirectory dtsd = objects.newInstance(DefaultThriftSourceDirectory.class, sd);
        sources.add(dtsd);

        return dtsd;
    }

    public ThriftSourceDirectory sourceDir(String path, Action<ThriftSourceDirectory> action) {
        ThriftSourceDirectory tsd = sourceDir(path);
        action.execute(tsd);
        return tsd;
    }

    public List<ThriftSourceDirectory> sourceDirs(String... paths) {
        return Arrays.stream(paths).map(this::sourceDir).collect(Collectors.toList());
    }

    public void includePath(String... paths) {
        for (String path : paths) {
            Directory dir = layout.getProjectDirectory().dir(path);
            Preconditions.checkArgument(
                dir.getAsFile().isDirectory(),
                "Include-path '%s' is not a directory",
                path);
            includePathEntries.add(dir);
        }
    }

    public void outputDir(String path) {
        File f = new File(path);
        if (f.isAbsolute()) {
            outputDirectory.fileValue(f);
        } else {
            outputDirectory.value(layout.getProjectDirectory().dir(path));
        }
    }

    public void kotlin(Action<KotlinThriftOptions> action) {
        KotlinThriftOptions opts = objects.newInstance(KotlinThriftOptions.class);
        action.execute(opts);
        thriftOptions.set(opts);
    }

    public void java(Action<JavaThriftOptions> action) {
        JavaThriftOptions opts = objects.newInstance(JavaThriftOptions.class);
        action.execute(opts);
        thriftOptions.set(opts);
    }
}
