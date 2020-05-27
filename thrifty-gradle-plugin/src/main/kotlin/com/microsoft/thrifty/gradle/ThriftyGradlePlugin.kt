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
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class ThriftyGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("thrifty", ThriftyExtension::class.java)

        val outputDir = Paths.get(project.buildDir.canonicalPath, "generated", "sources", "thrifty")

        val defaultSourceDirName = listOf("src", "main", "thrift").joinToString(File.separator)
        val defaultSourceDir = project.file(defaultSourceDirName).toPath()

        val thriftSourceSet = assembleThriftSources(project, ext, defaultSourceDirName)
        val thriftTaskProvider = project.tasks.register("generateThriftFiles", ThriftyTask::class.java) { t ->
            t.group = "thrifty"
            t.description = "Generate Thrifty thrift implementations for .thrift files"
            t.outputDirectory.set(outputDir.toFile())
            t.source(thriftSourceSet)
            t.includePath.set(assembleIncludePath(project, ext, defaultSourceDir))
            t.options.set(ext.thriftOptions)
        }

        val kotlinSources = project.fileTree(outputDir) {
            it.patterns.include("**/*.kt")
        }

        val javaSources = project.fileTree(outputDir) {
            it.patterns.include("**/*.java")
        }

        project.afterEvaluate {
            project.tasks.withType(KotlinCompile::class.java).all {
                it.source(kotlinSources)
                it.dependsOn(thriftTaskProvider)
            }

            project.tasks.withType(JavaCompile::class.java).all {
                it.source(javaSources)
                it.dependsOn(thriftTaskProvider)
            }
        }
    }

    private fun assembleIncludePath(project: Project, ext: ThriftyExtension, defaultSourceDir: Path): Provider<List<Path>> {
        val pathConfiguration = project.configurations.create("thriftPath")
        return ext.includeDirs.map { dirs ->
            dirs.mapNotNull { path ->
                val file = File(path).let { f ->
                    if (f.isAbsolute) f else project.file(f)
                }

                if (!file.isDirectory) {
                    // TODO: fail
                    return@mapNotNull null
                }

                val dep = project.dependencies.create(file)
                pathConfiguration.dependencies.add(dep)

                file.toPath()
            }
        }.map { listOf(defaultSourceDir) + it }
    }

    @Suppress("UnstableApiUsage")
    private fun assembleThriftSources(project: Project, ext: ThriftyExtension, defaultSourceDir: String): SourceDirectorySet {
        val sourceDirs = ext.sourceDirs.map { it.toSet().plus(defaultSourceDir) }
        val sourceSet = project.objects.sourceDirectorySet("thrifty-sources", "Thrift sources for compilation")
        sourceSet.srcDirs(sourceDirs)
        sourceSet.filter.include("**/*.thrift")

        val sourceDependency = project.dependencies.create(sourceSet)
        val sourceConfiguration = project.configurations.create("thriftSource")
        sourceConfiguration.dependencies.add(sourceDependency)

        return sourceSet
    }
}
