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
package com.microsoft.thrifty

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.testing.base.TestingExtension

class ThriftyJavaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        applyBasePlugins(project)
        applyLibrarySettings(project)
        applyJavaSettings(project)
        configureTestTasks(project)
    }

    private fun applyBasePlugins(project: Project) {
        with(project.plugins) {
            apply(Plugins.JAVA)
            apply(Plugins.TEST_SUITE)
            apply(Plugins.IDEA)
            apply(Plugins.JACOCO)
        }
    }

    private fun applyLibrarySettings(project: Project) {
        project.group = project.findProperty("GROUP") as String
        project.version = project.findProperty("VERSION_NAME") as String
    }

    private fun applyJavaSettings(project: Project) {
        val java = project.extensions.findByType<JavaPluginExtension>()!!
        java.sourceCompatibility = JavaVersion.VERSION_1_8
        java.targetCompatibility = JavaVersion.VERSION_1_8

        project.tasks
            .withType(JavaCompile::class.java)
            .configureEach { task ->
                task.options.isFork = true
                task.options.isIncremental = true
            }
    }

    @Suppress("UnstableApiUsage")
    private fun configureTestTasks(project: Project) {
        val catalogs = project.extensions.findByType<VersionCatalogsExtension>()!!
        val catalog = catalogs.named("libs")
        val maybeJunitVersion = catalog.findVersion("junit")
        check(maybeJunitVersion.isPresent) { "No junit version found" }

        val junitVersion = maybeJunitVersion.get()

        project.extensions.configure(TestingExtension::class.java) { ext ->
            ext.suites.named("test") {
                val suite = it as JvmTestSuite

                suite.useJUnitJupiter("$junitVersion")
                suite.targets.configureEach { target ->
                    target.testTask.configure { task ->
                        task.testLogging { logging ->
                            logging.events(TestLogEvent.FAILED)
                            logging.exceptionFormat = TestExceptionFormat.FULL
                            logging.showStackTraces = true
                            logging.showExceptions = true
                            logging.showCauses = true
                        }
                    }
                }
            }
        }
    }
}
