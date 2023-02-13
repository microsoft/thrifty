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

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

val Project.isReleaseBuild: Boolean
    get() {
        val versionName = project.findProperty("VERSION_NAME") as String?
        return versionName != null && !versionName.endsWith("-SNAPSHOT")
    }

val Project.isPublishingSnapshot: Boolean
    get() = project.findProperty("PUBLISH_SNAPSHOT")?.toString() == "true"

val Project.shouldSignAndDocumentBuild: Boolean
    get() = isReleaseBuild || isPublishingSnapshot

inline fun <reified T : Task> TaskCollection<in Task>.withType(): TaskCollection<T> {
    return withType(T::class.java)
}

inline fun <reified T> ExtensionContainer.findByType(): T? = findByType(T::class.java)

inline fun <reified T : Plugin<Project>> PluginContainer.apply(): T = apply(T::class.java)

inline fun <reified T : Task> TaskContainer.register(name: String, action: Action<in T>): TaskProvider<T> {
    return register(name, T::class.java, action)
}
