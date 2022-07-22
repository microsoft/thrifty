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

import com.google.common.annotations.VisibleForTesting;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * The plugin makes everything happen.
 */
@SuppressWarnings("UnstableApiUsage")
public abstract class ThriftyGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(@NotNull Project project) {
        Properties props = loadVersionProps();
        String version = props.getProperty("THRIFTY_VERSION");
        if (version == null || version.length() == 0) {
            throw new IllegalStateException("Missing THRIFTY_VERSION property");
        }

        ThriftyExtension ext = project.getExtensions().create("thrifty", ThriftyExtension.class);
        ext.getThriftyVersion().convention(version);

        Configuration thriftyConfig = createConfiguration(project, ext.getThriftyVersion());

        TaskProvider<ThriftyTask> thriftTaskProvider = project.getTasks().register("generateThriftFiles", ThriftyTask.class, t -> {
            t.setGroup("thrifty");
            t.setDescription("Generate Thrifty thrift implementations for .thrift files");
            t.getIncludePath().set(ext.getIncludePathEntries().map(dirs -> dirs.stream().map(Directory::getAsFile).collect(Collectors.toList())));
            t.getOutputDirectory().set(ext.getOutputDirectory());
            t.getThriftOptions().set(ext.getThriftOptions());
            t.getShowStacktrace().set(project.getGradle().getStartParameter().getShowStacktrace());
            t.getThriftyClasspath().from(thriftyConfig);
            t.source(ext.getSources().map(ss -> ss.stream().map(it -> it.getSourceDirectorySet()).collect(Collectors.toList())));
        });

        project.getPlugins().withType(JavaBasePlugin.class).configureEach(plugin -> {
            JavaPluginExtension extension = project.getExtensions().getByType(JavaPluginExtension.class);
            extension.getSourceSets().configureEach(ss -> {
                if (ss.getName().equals("main")) {
                    ss.getJava().srcDir(thriftTaskProvider);
                }
            });
        });
    }

    @VisibleForTesting
    static Properties loadVersionProps() {
        try (InputStream is = ThriftyGradlePlugin.class.getClassLoader().getResourceAsStream("thrifty-version.properties")) {
            Properties props = new Properties();
            props.load(is);
            return props;
        } catch (IOException e) {
            throw new GradleException("BOOM", e);
        }
    }

    private Configuration createConfiguration(Project project, final Provider<String> thriftyVersion) {
        Configuration configuration = project
                .getConfigurations().create("thriftyGradle")
                .setDescription("configuration for the Thrifty Gradle Plugin")
                .setVisible(false)
                .setTransitive(true);

        configuration.setCanBeConsumed(false);
        configuration.setCanBeResolved(true);

        configuration.defaultDependencies(deps -> {
            deps.add(project.getDependencies().create("com.microsoft.thrifty:thrifty-schema:" + thriftyVersion.get()));
            deps.add(project.getDependencies().create("com.microsoft.thrifty:thrifty-java-codegen:" + thriftyVersion.get()));
            deps.add(project.getDependencies().create("com.microsoft.thrifty:thrifty-kotlin-codegen:" + thriftyVersion.get()));
            deps.add(project.getDependencies().create("com.microsoft.thrifty:thrifty-compiler-plugins:" + thriftyVersion.get()));
        });

        return configuration;
    }
}
