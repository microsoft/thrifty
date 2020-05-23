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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.util.TreeMap

open class ThriftyExtension(project: Project) {
    private val objectFactory = project.objects

    val includeDirs: ListProperty<String> = project.objects.listProperty(String::class.java)
    val sourceDirs: ListProperty<String> = project.objects.listProperty(String::class.java)

    private val sourceThriftOptions: Property<ThriftOptions> = project.objects.property(ThriftOptions::class.java)
    val thriftOptions: Provider<ThriftOptions> = sourceThriftOptions.map { it ?: JavaThriftOptions() }

    fun sourceDir(path: String) {
        sourceDirs.add(path)
    }

    fun sourceDirs(vararg paths: String) {
        sourceDirs.addAll(*paths)
    }

    fun includeDir(path: String) {
        includeDirs.add(path)
    }

    fun includeDirs(vararg paths: String) {
        includeDirs.addAll(*paths)
    }

    fun kotlin(action: Action<KotlinThriftOptions>) {
        val opts = objectFactory.newInstance(KotlinThriftOptions::class.java).apply {
            action.execute(this)
        }
        sourceThriftOptions.set(opts)
    }

    fun java(action: Action<JavaThriftOptions>) {
        val opts = objectFactory.newInstance(JavaThriftOptions::class.java).apply {
            action.execute(this)
        }
        sourceThriftOptions.set(opts)
    }
}

sealed class ThriftOptions {
    // Language-independent compiler options we *aren't* exposing here:
    //
    // - generatedAnnotationType:
    //   We have perfect information here in the form of sourceCompatibility
    //   or Kotlin compiler arguments; no need to specify what the proper
    //   annotation is, we can figure that out ourselves.
    //
    // - omitGeneratedAnnotations:
    //   It seems even more important in _transient_ generated code to include
    //   this annotation than it does in tool-generated code.

    enum class FieldNameStyle {
        DEFAULT,
        JAVA,
        PASCAL
    }

    @Input
    var emitServiceClients: Boolean = true
        private set

    @Input
    @Optional
    var nameStyle: FieldNameStyle = FieldNameStyle.DEFAULT
        private set

    @Input
    @Optional
    var listType: String? = null
        private set

    @Input
    @Optional
    var setType: String? = null
        private set

    @Input
    @Optional
    var mapType: String? = null
        private set

    @Input
    var parcelable: Boolean = false

    fun generateServiceClients(shouldGenerate: Boolean) {
        this.emitServiceClients = shouldGenerate
    }

    fun nameStyle(styleName: String) {
        val stylesByName = caseInsensitiveMapOf(
                "default" to FieldNameStyle.DEFAULT,
                "java" to FieldNameStyle.JAVA,
                "pascal" to FieldNameStyle.PASCAL
        )

        val style = requireNotNull(stylesByName[styleName]) {
            stylesByName.keys.joinToString(
                    prefix = "Invalid name style; allowed values are:\n",
                    separator = "\n") { "\t- $it" }
        }

        nameStyle(style)
    }

    fun nameStyle(style: FieldNameStyle) {
        nameStyle = style
    }

    fun listType(name: String) {
        this.listType = name
    }

    fun listType(clazz: Class<*>) {
        this.listType = clazz.canonicalName!!
    }

    fun setType(name: String) {
        this.setType = name
    }

    fun setType(clazz: Class<*>) {
        this.setType = clazz.canonicalName!!
    }

    fun mapType(name: String) {
        this.mapType = name
    }

    fun mapType(clazz: Class<*>) {
        this.mapType = clazz.canonicalName!!
    }
}

open class KotlinThriftOptions : ThriftOptions() {
    enum class ClientStyle {
        NONE,
        DEFAULT,
        COROUTINE,
    }

    // Compiler options we intentionally are not exposing:
    // - file-per-type vs file-per-namespace:
    //   nobody is checking in code generated via this plugin,
    //   and there's a notable perf gain in compilation from
    //   file-per-type.  Ergo, we'll just use that.

    @Input
    @Optional
    var serviceClientStyle: ClientStyle? = null
        private set

    @Input
    var builderlessDataClasses = false

    fun serviceClientStyle(name: String) {
        val stylesByName = caseInsensitiveMapOf(
                "name" to ClientStyle.NONE,
                "default" to ClientStyle.DEFAULT,
                "coroutine" to ClientStyle.COROUTINE
        )

        val style = requireNotNull(stylesByName[name]) {
            stylesByName.keys.joinToString(
                    prefix = "Invalid client style name; valid style names are:\n",
                    separator = "\n") { "\t- $it" }
        }

        serviceClientStyle(style)
    }

    fun serviceClientStyle(style: ClientStyle) {
        generateServiceClients(style != ClientStyle.NONE)
        serviceClientStyle = style
    }
}

open class JavaThriftOptions : ThriftOptions() {
    enum class NullabilityAnnotations {
        NONE,
        ANDROID_SUPPORT,
        ANDROIDX,
    }

    @Input
    var nullabilityAnnotationKind: NullabilityAnnotations = NullabilityAnnotations.NONE
        private set

    fun nullabilityAnnotations(name: String) {
        val kindsByName = caseInsensitiveMapOf(
                "none" to NullabilityAnnotations.NONE,
                "android-support" to NullabilityAnnotations.ANDROID_SUPPORT,
                "androidx" to NullabilityAnnotations.ANDROIDX
        )

        val kind = requireNotNull(kindsByName[name]) {
            kindsByName.keys.joinToString(
                    prefix = "Invalid nullability annotation name; allowed values are:\n",
                    separator = "\n") { "\t- $it" }
        }

        nullabilityAnnotations(kind)
    }

    fun nullabilityAnnotations(kind: NullabilityAnnotations) {
        this.nullabilityAnnotationKind = kind
    }
}

fun <T> caseInsensitiveMapOf(vararg pairs: Pair<String, T>): Map<String, T> {
    if (pairs.isEmpty()) return emptyMap()

    return pairs.fold(TreeMap<String,T>(java.lang.String.CASE_INSENSITIVE_ORDER)) { acc, (name, value) ->
        acc[name] = value
        acc
    }
}

val ThriftOptions.isJava: Boolean
    get() = JavaThriftOptions::class.java.isAssignableFrom(javaClass)

val ThriftOptions.isKotlin: Boolean
    get() = KotlinThriftOptions::class.java.isAssignableFrom(javaClass)