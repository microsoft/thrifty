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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class ThriftyGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("thrifty", ThriftyExtension::class.java)

        val outputDir = Paths.get(project.buildDir.canonicalPath, "generated", "sources", "thrifty")

        val thriftTaskProvider = project.tasks.register("generateThriftFiles", ThriftyTask::class.java) { t ->
            t.group = "thrifty"
            t.description = "Generate Thrifty thrift implementations for .thrift files"
            t.outputDirectory.set(outputDir.toFile())
            t.options.set(ext.thriftOptions)
            t.showStacktrace.set(project.gradle.startParameter.showStacktrace)
        }

        val kotlinSources = project.fileTree(outputDir) {
            it.patterns.include("**/*.kt")
        }

        val javaSources = project.fileTree(outputDir) {
            it.patterns.include("**/*.java")
        }

        project.afterEvaluate {
            thriftTaskProvider.configure { t ->
                assembleSourceSets(project, ext).forEach { t.source(it) }
                t.includePath.set(assembleIncludePath(project, ext))
            }

            val thriftOptions = ext.thriftOptions.get()
            val javaTasks = project.tasks.withType(JavaCompile::class.java)
            val kotlinTasks = project.tasks.withType(KotlinCompile::class.java)

            javaTasks.configureEach {
                it.dependsOn(thriftTaskProvider)
                it.source(javaSources)
            }

            kotlinTasks.configureEach {
                it.dependsOn(thriftTaskProvider)
                it.source(kotlinSources)
            }

            if (thriftOptions is JavaThriftOptions && kotlinTasks.isNotEmpty()) {
                val sourceSetContainer = project.properties["sourceSets"] as SourceSetContainer
                val main = sourceSetContainer.getByName("main") as SourceSet
                main.java.srcDirs(outputDir)
            }
        }
    }

    private fun assembleSourceSets(project: Project, ext: ThriftyExtension): List<SourceDirectorySet> {
        val sourceConfiguration = project.configurations.create(SOURCE_CONFIGURATION_NAME)
        val sourceSets = ext.sources.get().map { it.sourceDirectorySet }

        for (sds in sourceSets) {
            val dependency = project.dependencies.create(sds)
            sourceConfiguration.dependencies.add(dependency)
        }

        return sourceSets
    }

    private fun assembleIncludePath(project: Project, ext: ThriftyExtension): List<Path> {
        val pathConfiguration = project.configurations.create(PATH_CONFIGURATION_NAME)
        val includeDirsAsFiles = ext.includeDirs.get().map(project::file)

        for (dir in includeDirsAsFiles) {
            require(dir.exists()) { "Thrift include-path entry '$dir' does not exist" }
            require(dir.isDirectory) { "Thrift include-path entry '$dir' must be a directory, but is a file" }

            val dep = project.dependencies.create(project.files(dir))
            pathConfiguration.dependencies.add(dep)
        }

        return includeDirsAsFiles.map(File::toPath)
    }

    companion object {
        private const val SOURCE_CONFIGURATION_NAME = "thriftSources"
        private const val PATH_CONFIGURATION_NAME = "thriftIncludePath"

        internal val DEFAULT_SOURCE_DIR = listOf("src", "main", "thrift").joinToString(File.separator)
    }
}
