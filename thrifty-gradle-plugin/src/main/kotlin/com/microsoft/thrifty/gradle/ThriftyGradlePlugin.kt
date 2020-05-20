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
import java.io.File

class ThriftyGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        var ext = project.extensions.create("thrifty", ThriftyExtension::class.java, project)

        project.configurations.create("thriftSource")
        project.configurations.create("thriftPath")

        project.tasks.register("generateThriftFiles", ThriftyTask::class.java) { t ->
            t.group = "thrifty"
            t.description = "Generate Thrifty thrift implementations for .thrift files"
        }

        val outputDir = listOf("${project.buildDir}", "generated", "source", "thrifty").joinToString(File.pathSeparator)
    }

}