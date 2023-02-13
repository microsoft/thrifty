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
