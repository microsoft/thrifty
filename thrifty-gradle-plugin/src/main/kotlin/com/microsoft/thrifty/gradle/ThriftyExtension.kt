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
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.io.File
import java.util.SortedMap
import java.util.TreeMap
import javax.inject.Inject

@Suppress("UnstableApiUsage")
abstract class ThriftyExtension @Inject constructor(
        private val objects: ObjectFactory,
        private val layout: ProjectLayout
) {
    val includePathEntries: ListProperty<Directory> = objects.listProperty(Directory::class.java)

    val sources: ListProperty<DefaultThriftSourceDirectory> = objects.listProperty(DefaultThriftSourceDirectory::class.java)
            .convention(listOf(DefaultThriftSourceDirectory(objects.sourceDirectorySet("thrift-sources", "Thrift sources")
                    .srcDir(DEFAULT_SOURCE_DIR)
                    .include("**/*.thrift") as SourceDirectorySet)))

    val thriftOptions: Property<ThriftOptions> = objects.property(ThriftOptions::class.java)
            .convention(JavaThriftOptions())

    val outputDirectory: DirectoryProperty = objects.directoryProperty()
            .convention(layout.buildDirectory.dir(DEFAULT_OUTPUT_DIR))

    fun sourceDir(path: String): DefaultThriftSourceDirectory {
        val sd = objects.sourceDirectorySet("thrift-sources", "Thrift sources").apply {
            srcDir(path)
        }

        return objects.newInstance(DefaultThriftSourceDirectory::class.java, sd).also { sources.add(it) }
    }

    fun sourceDir(path: String, action: Action<ThriftSourceDirectory>): DefaultThriftSourceDirectory {
        return sourceDir(path).also { action.execute(it) }
    }

    fun sourceDirs(vararg paths: String): List<DefaultThriftSourceDirectory> {
        return paths.map { sourceDir(it) }
    }

    fun includePath(vararg path: String) {
        for (p in path) {
            val d = layout.projectDirectory.dir(p)
            require(d.asFile.isDirectory) { "Include-path entries must be directories" }
            includePathEntries.add(d)
        }
    }

    fun outputDir(path: String) {
        val f = File(path)
        if (f.isAbsolute) {
            outputDirectory.fileValue(f)
        } else {
            outputDirectory.value(layout.projectDirectory.dir(path))
        }
    }

    fun kotlin(action: Action<KotlinThriftOptions>) {
        val opts = objects.newInstance(KotlinThriftOptions::class.java).apply {
            action.execute(this)
        }
        thriftOptions.set(opts)
    }

    fun java(action: Action<JavaThriftOptions>) {
        val opts = objects.newInstance(JavaThriftOptions::class.java).apply {
            action.execute(this)
        }
        thriftOptions.set(opts)
    }

    companion object {
        @JvmStatic
        private val DEFAULT_SOURCE_DIR = listOf("src", "main", "thrift").joinToString(File.separator)

        @JvmStatic
        private val DEFAULT_OUTPUT_DIR = listOf("generated", "sources", "thrifty").joinToString(File.separator)
    }
}

sealed class ThriftOptions {
    enum class FieldNameStyle(val optionName: String) {
        DEFAULT("default"),
        JAVA("java"),
        PASCAL("pascal")
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

    @Input
    var allowUnknownEnumValues: Boolean = false
        private set

    fun generateServiceClients(shouldGenerate: Boolean) {
        this.emitServiceClients = shouldGenerate
    }

    fun nameStyle(styleName: String) {
        val stylesByName = FieldNameStyle.values()
                .map { it.optionName to it }
                .toCaseInsensitiveMap()

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

    fun allowUnknownEnumValues(allow: Boolean) {
        this.allowUnknownEnumValues = allow
    }
}

open class KotlinThriftOptions : ThriftOptions() {
    enum class ClientStyle(val optionName: String) {
        NONE("none"),
        DEFAULT("default"),
        COROUTINE("coroutine"),
    }

    @Input
    @Optional
    var serviceClientStyle: ClientStyle? = null
        private set

    @Input
    var builderlessDataClasses = false

    fun serviceClientStyle(name: String) {
        val stylesByName = ClientStyle.values()
                .map { it.optionName to it }
                .toCaseInsensitiveMap()

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
    enum class NullabilityAnnotations(val optionName: String) {
        NONE("none"),
        ANDROID_SUPPORT("android-support"),
        ANDROIDX("androidx"),
    }

    @Input
    var nullabilityAnnotationKind: NullabilityAnnotations = NullabilityAnnotations.NONE
        private set

    fun nullabilityAnnotations(name: String) {
        val kindsByName = NullabilityAnnotations.values()
                .map { it.optionName to it }
                .toCaseInsensitiveMap()

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

interface ThriftSourceDirectory {
    fun include(pattern: String)
    fun exclude(pattern: String)
}

open class DefaultThriftSourceDirectory @Inject constructor(
        internal val sourceDirectorySet: SourceDirectorySet
) : ThriftSourceDirectory {
    private var didClearDefaults: Boolean = false

    init {
        sourceDirectorySet.include("**/*.thrift")
    }

    override fun include(pattern: String) {
        clearDefaults()
        sourceDirectorySet.include(pattern)
    }

    override fun exclude(pattern: String) {
        clearDefaults()
        sourceDirectorySet.exclude(pattern)
    }

    private fun clearDefaults() {
        if (didClearDefaults) {
            return
        }

        didClearDefaults = true
        sourceDirectorySet.includes.clear()
        sourceDirectorySet.excludes.clear()
    }
}

private fun <C : Iterable<P>, P : Pair<String, V>, V> C.toCaseInsensitiveMap(): SortedMap<String, V> {
    return toMap(TreeMap(java.lang.String.CASE_INSENSITIVE_ORDER))
}
