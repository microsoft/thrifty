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

import com.google.common.base.Joiner;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PluginTest {
    private final File fixturesDir = new File(Joiner.on(File.separator).join("src", "test", "projects"));
    private final GradleRunner runner = GradleRunner.create();

    @ParameterizedTest
    @ValueSource(strings = {
            "kotlin_integration_project",
            "java_project_kotlin_thrifts",
            "java_project_java_thrifts",
            "kotlin_project_kotlin_thrifts",
            "kotlin_project_java_thrifts",
            "kotlin_project_filtered_thrifts",
            "kotlin_multiple_source_dirs",
            "kotlin_project_with_custom_output_dir",
            "kotlin_project_with_include_path",
    })
    void integrationProjectBuildsSuccessfully(String fixtureName) throws Exception {
        BuildResult result = buildFixture(runner, fixtureName, GradleRunner::build);
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateThriftFiles").getOutcome());
    }

    @Test
    void typeProcessorConfigurationWorks() throws Exception {
        BuildResult result = buildFixtureWithSubprojectsAndTask(
            runner,
            "kotlin_type_processor",
            Arrays.asList(":app", ":processor"),
            ":app:build",
            GradleRunner::build);
        assertEquals(TaskOutcome.SUCCESS, result.task(":app:generateThriftFiles").getOutcome());

        Assertions.assertTrue(result.getOutput().contains("I AM IN A TYPE PROCESSOR"));
    }

    private BuildResult buildFixture(GradleRunner runner, String fixtureName, Function<GradleRunner, BuildResult> buildAndAssert) throws Exception {
        return buildFixtureWithSubprojectsAndTask(
            runner,
            fixtureName,
            Collections.emptyList(),
            ":build",
            buildAndAssert);
    }

    private BuildResult buildFixtureWithSubprojectsAndTask(GradleRunner runner, String fixtureName, List<String> subprojects, String task, Function<GradleRunner, BuildResult> buildAndAssert) throws Exception {
        File fixture = new File(fixturesDir, fixtureName);
        File settings = new File(fixture, "settings.gradle");
        File buildDirectory = new File(fixture, "build");
        File gradleDirectory = new File(fixture, ".gradle");

        boolean didCreateSettings = settings.createNewFile();

        try (BufferedWriter w = Files.newBufferedWriter(settings.toPath())) {
            w.write("pluginManagement {\n");
            w.write("  repositories {\n");
            w.write("    mavenLocal()\n");
            w.write("    gradlePluginPortal()\n");
            w.write("  }\n");
            w.write("  plugins {\n");
            w.write("    id 'com.microsoft.thrifty' version '");
            w.write(getThriftyVersion());
            w.write("'\n");
            w.write("  id 'org.jetbrains.kotlin.jvm' version '");
            w.write(getKotlinVersion());
            w.write("'\n");
            w.write("  }\n");
            w.write("}\n");
            w.write("enableFeaturePreview('VERSION_CATALOGS')\n");
            w.write("dependencyResolutionManagement {\n");
            w.write("  repositories {\n");
            w.write("    mavenLocal()\n");
            w.write("    mavenCentral()\n");
            w.write("  }\n");
            w.write("  versionCatalogs {\n");
            w.write("    msft {\n");
            w.write("      version('thrifty', '");
            w.write(getThriftyVersion());
            w.write("')\n");
            w.write("      alias('thrifty-runtime').to('com.microsoft.thrifty', 'thrifty-runtime').versionRef('thrifty')\n");
            w.write("      alias('thrifty-compilerPlugins').to('com.microsoft.thrifty', 'thrifty-compiler-plugins').versionRef('thrifty')\n");
            w.write("    }\n");
            w.write("  }\n");
            w.write("}\n");

            for (String subproject : subprojects) {
                w.write("include '" + subproject + "'\n");
            }

            w.flush();
        }

        try {
            GradleRunner run = runner
                    .withProjectDir(fixture)
                    .withArguments(task, "--stacktrace", "--info", "--no-build-cache", "--no-configuration-cache");
            return buildAndAssert.apply(run);
        } finally {
            if (didCreateSettings) settings.delete();
            if (buildDirectory.exists()) deleteRecursively(buildDirectory);
            if (gradleDirectory.exists()) deleteRecursively(gradleDirectory);
        }
    }

    private void deleteRecursively(File file) throws IOException {
        Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private String getThriftyVersion() throws Exception {
        Properties props = ThriftyGradlePlugin.loadVersionProps();
        return props.getProperty("THRIFTY_VERSION");
    }

    private String getKotlinVersion() throws Exception {
        return ThriftyGradlePlugin.loadVersionProps().getProperty("KOTLIN_VERSION");
    }
}
