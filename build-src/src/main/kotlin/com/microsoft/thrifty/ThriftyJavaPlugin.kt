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
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

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

    private fun configureTestTasks(project: Project) {
        project.tasks.withType<Test>().configureEach { task ->
            task.useJUnitPlatform()
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
