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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

open class ThriftyExtension(project: Project) {
    private val objectFactory = project.objects

    private val includeDirs = mutableSetOf<String>()
    private val thriftFiles = mutableSetOf<String>()

    private var kotlinOptions: KotlinOptions? = null
    private var javaOptions: JavaOptions? = null

    // TODO: wildcard globs
    fun thriftFile(vararg files: String) {
        thriftFiles.addAll(files)
    }

    fun includeDir(vararg paths: String) {
        includeDirs.addAll(paths)
    }

    @Input
    @Optional
    fun thriftFiles(): Set<String> {
        return thriftFiles.toSet()
    }

    @Input
    @Optional
    fun includePaths(): Set<String> {
        return includeDirs.toSet()
    }

    fun kotlin(action: Action<KotlinOptions>) {
        val options = objectFactory.newInstance(KotlinOptions::class.java)
        action.execute(options)
        kotlinOptions = options
    }

    fun java(action: Action<JavaOptions>) {
        val options = objectFactory.newInstance(JavaOptions::class.java)
        action.execute(options)
        javaOptions = options
    }

    open class KotlinOptions {

    }

    open class JavaOptions {

    }
}