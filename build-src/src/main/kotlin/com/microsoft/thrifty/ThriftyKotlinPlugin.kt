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
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Applies Kotlin language plugins and configures them.
 *
 * All this, just to make Dokka optional.
 */
class ThriftyKotlinPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        applyBasePlugins(project)
        addKotlinBom(project)
        configureKotlinTasks(project)
    }

    private fun applyBasePlugins(project: Project) {
        project.plugins.apply<ThriftyJavaPlugin>()
        project.plugins.apply(Plugins.KOTLIN_JVM)
        if (project.shouldSignAndDocumentBuild) {
            project.plugins.apply(Plugins.DOKKA)
        }
    }

    private fun addKotlinBom(project: Project) {
        val catalogs = project.extensions.findByType<VersionCatalogsExtension>()!!
        val catalog = catalogs.named("libs")
        val maybeBomProvider = catalog.findLibrary("kotlin-bom")
        check(maybeBomProvider.isPresent) { "No kotlin-bom dependency found" }

        val kotlinBom: Dependency = maybeBomProvider.get().get()
        val kotlinBomPlatformDependency = project.dependencies.platform(kotlinBom)
        project.configurations
            .getByName("api")
            .dependencies
            .add(kotlinBomPlatformDependency)
    }

    private fun configureKotlinTasks(project: Project) {
        val kotlinCompileTasks = project.tasks.withType<KotlinCompile>()
        kotlinCompileTasks.configureEach { task ->
            task.kotlinOptions { opts ->
                opts.jvmTarget = "1.8"
                opts.freeCompilerArgs = listOf("-Xjvm-default=all")
            }
        }
    }
}
