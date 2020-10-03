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

import com.microsoft.thrifty.gen.NullabilityAnnotationType
import com.microsoft.thrifty.gen.ThriftyCodeGenerator
import com.microsoft.thrifty.kgen.KotlinCodeGenerator
import com.microsoft.thrifty.schema.ErrorReporter
import com.microsoft.thrifty.schema.FieldNamingPolicy
import com.microsoft.thrifty.schema.LoadFailedException
import com.microsoft.thrifty.schema.Loader
import com.microsoft.thrifty.schema.Schema
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.ShowStacktrace
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import java.nio.file.Path
import javax.inject.Inject

open class ThriftyTask @Inject constructor(
        objects: ObjectFactory
) : SourceTask() {
    @OutputDirectory
    @Suppress("UnstableApiUsage")
    val outputDirectory: DirectoryProperty = objects.directoryProperty()

    @InputFiles
    val includePath: ListProperty<Path> = objects.listProperty(Path::class.java)

    @Nested
    val options: Property<ThriftOptions> = objects.property(ThriftOptions::class.java)

    @Internal
    @Suppress("UnstableApiUsage")
    val showStacktrace: Property<ShowStacktrace> = objects.property(ShowStacktrace::class.java)
            .convention(ShowStacktrace.INTERNAL_EXCEPTIONS)

    @TaskAction
    fun run() {
        val schema = try {
            val loader = Loader()
            includePath.get().forEach { loader.addIncludePath(it) }
            source.forEach { loader.addThriftFile(it.toPath()) }
            loader.load()
        } catch (e: LoadFailedException) {
            reportThriftParseError(e)
            throw GradleException("Thrift compilation failed")
        }

        try {
            outputDirectory.asFile.get().deleteRecursively()
        } catch (e: IOException) {
            // eh
            logger.warn("Error clearing stale output", e)
        }

        when (val opt = options.get()) {
            is KotlinThriftOptions -> generateKotlinThrifts(schema, opt)
            is JavaThriftOptions -> generateJavaThrifts(schema, opt)
        }
    }

    private fun reportThriftParseError(exception: LoadFailedException) {
        for (report in exception.errorReporter.reports) {
            val logLevel = when (report.level) {
                ErrorReporter.Level.WARNING -> LogLevel.WARN
                ErrorReporter.Level.ERROR -> LogLevel.ERROR
            }

            val message = "${logLevel.name[0]}: ${report.location}: ${report.message}"
            logger.log(logLevel, message)
        }

        when (showStacktrace.get()) {
            ShowStacktrace.ALWAYS,
            ShowStacktrace.ALWAYS_FULL -> {
                logger.error("Thrift compilation failed", exception)
            }

            null,
            ShowStacktrace.INTERNAL_EXCEPTIONS -> {}
        }
    }

    private fun generateJavaThrifts(schema: Schema, options: JavaThriftOptions) {
        val gen = ThriftyCodeGenerator(schema, options.namePolicy).apply {
            emitFileComment(true)
            emitParcelable(options.parcelable)
            failOnUnknownEnumValues(!options.allowUnknownEnumValues)

            options.listType?.let { withListType(it) }
            options.setType?.let { withSetType(it) }
            options.mapType?.let { withMapType(it) }

            val annoType = when (options.nullabilityAnnotationKind) {
                JavaThriftOptions.NullabilityAnnotations.ANDROID_SUPPORT ->
                    NullabilityAnnotationType.ANDROID_SUPPORT

                JavaThriftOptions.NullabilityAnnotations.ANDROIDX ->
                    NullabilityAnnotationType.ANDROIDX

                JavaThriftOptions.NullabilityAnnotations.NONE ->
                    NullabilityAnnotationType.NONE
            }

            nullabilityAnnotationType(annoType)
        }

        gen.generate(outputDirectory.asFile.get())
    }

    private fun generateKotlinThrifts(schema: Schema, options: KotlinThriftOptions) {
        val gen = KotlinCodeGenerator(options.namePolicy).apply {
            emitJvmName()
            filePerType()
            if (options.parcelable) {
                parcelize()
            }

            failOnUnknownEnumValues(!options.allowUnknownEnumValues)

            if (options.builderlessDataClasses) {
                builderlessDataClasses()
            }

            if (!options.emitServiceClients) {
                omitServiceClients()
            } else {
                when (options.serviceClientStyle ?: KotlinThriftOptions.ClientStyle.DEFAULT) {
                    KotlinThriftOptions.ClientStyle.NONE -> omitServiceClients()
                    KotlinThriftOptions.ClientStyle.DEFAULT -> {} // no-op
                    KotlinThriftOptions.ClientStyle.COROUTINE -> coroutineServiceClients()
                }
            }

            options.listType?.let { listClassName(it) }
            options.setType?.let { setClassName(it) }
            options.mapType?.let { mapClassName(it) }
        }

        val dir = outputDirectory.asFile.get()
        gen.generate(schema).forEach { it.writeTo(dir) }
    }

    private val ThriftOptions.namePolicy: FieldNamingPolicy
        get() = when (nameStyle) {
            ThriftOptions.FieldNameStyle.DEFAULT -> FieldNamingPolicy.DEFAULT
            ThriftOptions.FieldNameStyle.JAVA -> FieldNamingPolicy.JAVA
            ThriftOptions.FieldNameStyle.PASCAL -> FieldNamingPolicy.PASCAL
        }
}