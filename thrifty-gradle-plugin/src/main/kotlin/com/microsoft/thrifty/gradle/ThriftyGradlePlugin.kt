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
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

abstract class ThriftyGradlePlugin : Plugin<Project> {
    @Suppress("UnstableApiUsage")
    override fun apply(project: Project) {
        val ext = project.extensions.create("thrifty", ThriftyExtension::class.java)

        // TODO: Gradle's AntlrPlugin shows how to do this "properly", by reacting
        //       to new SourceSets and setting up source dirs and tasks scoped to those
        //       sets.  It also seems limited to only Java.  We should see if we can
        //       generalize that reactive approach to a) Kotlin b) customizable input dirs.

        val thriftTaskProvider = project.tasks.register("generateThriftFiles", ThriftyTask::class.java) { t ->
            t.group = "thrifty"
            t.description = "Generate Thrifty thrift implementations for .thrift files"
            t.includePath.set(ext.includePathEntries.map { dirs -> dirs.map { it.asFile.toPath() } })
            t.outputDirectory.set(ext.outputDirectory)
            t.options.set(ext.thriftOptions)
            t.showStacktrace.set(project.gradle.startParameter.showStacktrace)
            t.source(ext.sources.map { ss -> ss.map { it.sourceDirectorySet } })
        }

        project.afterEvaluate {
            val sourceSetContainer = project.properties["sourceSets"] as SourceSetContainer
            val main = sourceSetContainer.getByName("main") as SourceSet
            main.java.srcDir(thriftTaskProvider)
        }
    }
}
