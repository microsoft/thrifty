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

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.nio.file.Path

class ThriftyGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("thrifty", ThriftyExtension::class.java, project)

        val outputDirName = listOf("${project.buildDir}", "generated", "sources", "thrifty").joinToString(File.separator)
        val outputDir = project.file(outputDirName).toPath()

        val defaultSourceDirName = listOf("src", "main", "thrift").joinToString(File.separator)
        val defaultSourceDir = project.file(defaultSourceDirName).toPath()

        val includePath = assembleIncludePath(project, ext, defaultSourceDir)
        val thriftFiles = assembleThriftSources(project, ext, defaultSourceDirName)

        val sourceCompatibilityVersion = detectSourceCompatibilityVersion(project)
        if (sourceCompatibilityVersion <= JavaVersion.VERSION_1_8) {
            // If we're targeting Java 8 or below, we're tagging generated classes
            // with @javax.annotation.Generated, which is _not_ included in the JDK
            // by default.  So to make sure our code can be compiled, we'll force a
            // dependency on javax.annotation-api.
            //
            // TODO: This is disgusting.  Find a better way, if at all possible.
            project.dependencies.add("compileClasspath", "javax.annotation:javax.annotation-api:+")
        }

        val provider = project.tasks.register("generateThriftFiles", ThriftyTask::class.java) { t ->
            t.group = "thrifty"
            t.description = "Generate Thrifty thrift implementations for .thrift files"
            t.outputDirectory.set(outputDir.toFile())
            t.sourceCompatibility = sourceCompatibilityVersion
            t.source(thriftFiles)
            t.includePath.set(includePath)
            t.options.set(ext.thriftOptions)
        }

        project.afterEvaluate {
            // We're doing an afterEvaluate because
            // a) compile tasks appear not to be available for configuration beforehand
            // b) our own extension isn't fully configured, apparently, until later

            val options = ext.thriftOptions.get()
            project.tasks.withType(KotlinCompile::class.java).configureEach {
                if (options is KotlinThriftOptions) {
                    it.source(outputDir)
                }
                it.dependsOn(provider)
            }

            project.tasks.withType(JavaCompile::class.java).configureEach {
                if (options is JavaThriftOptions) {
                    it.source(outputDir)
                }
                it.dependsOn(provider)
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

    private fun detectSourceCompatibilityVersion(project: Project): JavaVersion {
        val kotlinTasks = project.tasks.withType(KotlinCompile::class.java)
        val kotlinJvmTarget = kotlinTasks.map { JavaVersion.toVersion(it.kotlinOptions.jvmTarget) }.max()
        if (kotlinJvmTarget != null) {
            project.logger.debug("Found a Kotlin JVM target of {}", kotlinJvmTarget)
            return kotlinJvmTarget
        }

        val javaPlugin = project.extensions.findByType(JavaPluginExtension::class.java)
        if (javaPlugin != null) {
            return javaPlugin.sourceCompatibility
        }

        error("Please apply either Java or Kotlin plugins to use Thrifty in this module.")
    }
}