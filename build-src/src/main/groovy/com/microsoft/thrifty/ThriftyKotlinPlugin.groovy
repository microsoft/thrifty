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
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Applies Kotlin language plugins and configures them.
 *
 * All this, just to make Dokka optional.
 */
class ThriftyKotlinPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        applyBasePlugins(project)
        addKotlinBom(project)
        configureKotlinTasks(project)
    }

    private static void applyBasePlugins(Project project) {
        project.plugins.apply("thrifty-jvm-module")
        project.plugins.apply("org.jetbrains.kotlin.jvm")

        if (VersionUtil.isReleaseBuild(project)) {
            project.plugins.apply("org.jetbrains.dokka")
        }
    }

    private static void addKotlinBom(Project project) {
        VersionCatalogsExtension catalogs = project.extensions.findByType(VersionCatalogsExtension)
        VersionCatalog catalog = catalogs.named("libs")
        Dependency kotlinBom = catalog.findLibrary("kotlin-bom").get().get()
        Dependency kotlinBomPlatformDependency = project.dependencies.platform(kotlinBom)

        project.configurations.findByName("api").dependencies.add(kotlinBomPlatformDependency)
    }

    private static void configureKotlinTasks(Project project) {
        project.tasks.withType(KotlinCompile).configureEach { t ->
            t.kotlinOptions {
                jvmTarget = '1.8'
                freeCompilerArgs = ['-Xjvm-default=all']
            }
        }
    }
}
