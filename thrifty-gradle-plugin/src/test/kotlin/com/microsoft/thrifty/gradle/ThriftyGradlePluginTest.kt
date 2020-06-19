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
package com.microsoft.thrifty.gradle

import io.kotest.matchers.shouldBe
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File

class ThriftyGradlePluginTest {
    private val fixturesDir = File(listOf("src", "test", "projects").joinToString(File.separator))
    private val runner = GradleRunner.create().withPluginClasspath()

    @Test fun `it does not fail when applied prior to any kotlin or java plugins`() {
        val project = ProjectBuilder.builder()
                .withName("foo")
                .build()
        project.plugins.apply(ThriftyGradlePlugin::class.java)
        project.plugins.apply("org.jetbrains.kotlin.jvm")

        (project as ProjectInternal).evaluate()
    }

    @Test fun `build kotlin integration project`() {
        val result = runner.buildFixture("kotlin_integration_project") { build() }
        result.task(":generateThriftFiles")!!.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test fun `java project with kotlin thrifts`() {
        runner.buildFixture("java_project_kotlin_thrifts") { build() }
    }

    @Test fun `java project with java thrifts`() {
        runner.buildFixture("java_project_java_thrifts") { build() }
    }

    @Test fun `kotlin project with kotlin thrifts`() {
        runner.buildFixture("kotlin_project_kotlin_thrifts") { build() }
    }

    @Test fun `kotlin project with java thrifts`() {
        runner.buildFixture("kotlin_project_java_thrifts") { build() }
    }

    @Test fun `kotlin project with filtered thrift inputs`() {
        runner.buildFixture("kotlin_project_filtered_thrifts") { build() }
    }

    @Test fun `kotlin project with multiple thrift dirs`() {
        runner.buildFixture("kotlin_multiple_source_dirs") { build() }
    }

    @Test fun `kotlin project with include path`() {
        runner.buildFixture("kotlin_project_with_include_path") { build() }
    }

    private fun GradleRunner.buildFixture(fixtureName: String, buildAndAssert: GradleRunner.() -> BuildResult): BuildResult {
        val fixture = File(fixturesDir, fixtureName)
        val settings = File(fixture, "settings.gradle")
        val didCreateSettings = settings.createNewFile()

        val buildDirectory = File(fixture, "build")

        try {
            return withProjectDir(fixture).withArguments(":build", "--stacktrace", "--info").buildAndAssert()
        } finally {
            if (didCreateSettings) settings.delete()
            if (buildDirectory.exists()) buildDirectory.deleteRecursively()
        }
    }
}