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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JvmEcosystemPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestReport
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File

abstract class ThriftyCodeCoveragePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply(Plugins.JACOCO)
        target.tasks.register<JacocoReport>("codeCoverageReport") {
            configureReportTask(target, it)
        }
    }

    private fun configureReportTask(project: Project, task: JacocoReport) {
        project.subprojects { sp ->
            // Multiplatform projects will have an "allTests" task that is not, itself,
            // a Test subclass - it is a KotlinTestReport.  Jacoco, the way we're using it,
            // uses what Gradle considers to be output from allTests; if we don't explicitly
            // declare the dependency, Gradle will fail builds as of version 8.0.
            task.dependsOn(sp.tasks.withType<Test>())
            task.dependsOn(sp.tasks.withType<TestReport>().matching { it.name == "allTests" })

            sp.plugins.withType(JvmEcosystemPlugin::class.java).configureEach {
                sp.extensions
                    .findByType<SourceSetContainer>()!!
                    .configureEach {
                        if (it.name == "main" || it.name.endsWith("Main")) {
                            task.sourceSets(it)
                        }
                    }
            }
        }

        val rootPath = project.rootDir.absoluteFile
        val execData = project.fileTree(rootPath).include("**/build/jacoco/*.exec")
        task.executionData(execData)

        val filters = listOf(
            "**/AutoValue_*",
            "**/antlr/*",
            "com/microsoft/thrifty/test/gen/*"
        )

        val filteredClassFiles = project.files(
            task.classDirectories.files.map {
                project.fileTree(mapOf("dir" to it, "exclude" to filters))
            }
        )
        task.classDirectories.setFrom(filteredClassFiles)

        val reportFilePath = listOf("reports", "jacoco", "report.xml").joinToString(File.separator)
        task.reports {
            it.xml.required.set(true)
            it.xml.outputLocation.set(project.layout.buildDirectory.file(reportFilePath))
            it.html.required.set(false)
            it.csv.required.set(false)
        }
    }
}
