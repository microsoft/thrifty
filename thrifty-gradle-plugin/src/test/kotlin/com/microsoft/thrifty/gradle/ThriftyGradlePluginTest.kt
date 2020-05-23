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

import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.shouldNot
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class ThriftyGradlePluginTest {
    private val runner = GradleRunner.create().withPluginClasspath()

    @Test fun `it does nothing`() {
        ThriftyGradlePlugin() shouldNot beNull()
    }

    @Test fun `build kotlin project`() {
        val result = runner.buildFixture(File("src/test/projects/kotlin_project_kotlin_thrifts"))


    }

    private fun GradleRunner.buildFixture(fixture: File): BuildResult {
        val settings = File(fixture, "settings.gradle")
        val didCreateSettings = settings.createNewFile()

        val buildDirectory = File(fixture, "build")

        try {
            return withProjectDir(fixture).withArguments("compileKotlin", "--stacktrace", "--info").build()
        } finally {
            if (didCreateSettings) settings.delete()
            if (buildDirectory.exists()) buildDirectory.deleteRecursively()
        }
    }
}