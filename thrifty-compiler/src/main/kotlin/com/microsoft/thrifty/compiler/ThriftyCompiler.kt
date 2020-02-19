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
package com.microsoft.thrifty.compiler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.transformAll
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import com.microsoft.thrifty.gen.NullabilityAnnotationType
import com.microsoft.thrifty.gen.ThriftyCodeGenerator
import com.microsoft.thrifty.kgen.KotlinCodeGenerator
import com.microsoft.thrifty.schema.FieldNamingPolicy
import com.microsoft.thrifty.schema.LoadFailedException
import com.microsoft.thrifty.schema.Loader
import com.microsoft.thrifty.schema.Schema
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList

/**
 * A program that compiles Thrift IDL files into Java source code for use
 * with thrifty-runtime.
 *
 * ```
 * java -jar thrifty-compiler.jar --out=/path/to/output
 * [--path=dir/for/search/path]
 * [--list-type=java.util.ArrayList]
 * [--set-type=java.util.HashSet]
 * [--map-type=java.util.HashMap]
 * [--lang=[java|kotlin]]
 * [--kt-file-per-type]
 * [--parcelable]
 * [--use-android-annotations]
 * [--nullability-annotation-type=[none|android-support|androidx]]
 * [--omit-file-comments]
 * [--omit-generated-annotations]
 * [--generated-annotation-type=[jdk8|jdk9|native]]
 * file1.thrift
 * file2.thrift
 * ...
 * ```
 *
 * `--out` is required, and specifies the directory to which generated
 * Java sources will be written.
 *
 * `--path` can be given multiple times.  Each directory so specified
 * will be placed on the search path.  When resolving `include` statements
 * during thrift compilation, these directories will be searched for included files.
 *
 * `--list-type` is optional.  When provided, the compiler will use the given
 * class name when instantiating list-typed values.  Defaults to [ArrayList].
 *
 * `--set-type` is optional.  When provided, the compiler will use the given
 * class name when instantiating set-typed values.  Defaults to [java.util.HashSet].
 *
 * `--map-type` is optional.  When provided, the compiler will use the given
 * class name when instantiating map-typed values.  Defaults to [java.util.HashMap].
 * Android users will likely wish to substitute `android.support.v4.util.ArrayMap`.
 *
 * `--lang=[java|kotlin]` is optional, defaulting to Java.  When provided, the
 * compiler will generate code in the specified language.
 *
 * `--kt-file-per-type` is optional.  When specified, one Kotlin file will be generated
 * for each top-level generated Thrift type.  When absent (the default), all generated
 * types in a single package will go in one file named `ThriftTypes.kt`.  Implies
 * `--lang=kotlin`.
 *
 * `--parcelable` is optional.  When provided, generated types will contain a
 * `Parcelable` implementation.  Kotlin types will use the `@Parcelize` extension.
 *
 * `--use-android-annotations` (deprecated) is optional.  When specified, generated Java classes
 * will have `@android.support.annotation.Nullable` or `@android.support.annotation.NotNull`
 * annotations, as appropriate.  Has no effect on Kotlin code.  Note: This option is superseded by
 * `--nullability-annotation-type`.  Setting this is equivalent to
 * `--nullability-annotation-type=android-support`.
 *
 * `--nullability-annotation-type=[none|android-support|androidx]` is optional, defaulting to
 * `none`.  When specified as something other than `none`, generated Java classes will have
 * `@Nullable` or `@NotNull` annotations, as appropriate.  Since AndroidX was introduced, these
 * annotations were repackaged from `android.support.annotation` to `androidx.annotation`.  Use
 * the `android-support` option for projects that are using the Android Support Library and have
 * not migrated to AndroidX.  Use the `androidx` option for projects that have migrated to AndroidX.
 * Has no effect on Kotlin code.
 *
 * `--omit-file-comments` is optional.  When specified, no file-header comment is generated.
 * The default behavior is to prefix generated files with a comment indicating that they
 * are generated by Thrifty, and should probably not be modified by hand.
 *
 * `--omit-generated-annotations` is optional.  When specified, generated types will not
 * have `@Generated` annotations applied.  The default behavior is to annotate all generated
 * types with `javax.annotation.Generated`.
 *
 * `--generated-annotatation-type=[jdk8|jdk9|native]` is optional, defaulting to `jdk8`.
 * This option controls the type of `@Generated` annotation added to generated types.  The default
 * of `jdk8` results in `javax.annotation.Generated`.  `jdk9` results in
 * `javax.annotation.processing.Generated`.  `native` will use whichever version is present
 * in the Java runtime used to run the compiler.  This option is a sad consequence of the decision
 * to repackage the `@Generated` annotation starting in Java 9.  Code generated with one annotation
 * will not compile on Java versions that do not have this annotation.  Because Thrifty is intended
 * for use on Android, we default to jdk8 (Android doesn't support JDK9 and probably never* will).
 *
 * If no .thrift files are given, then all .thrift files located on the search path
 * will be implicitly included; otherwise only the given files (and those included by them)
 * will be compiled.
 */
class ThriftyCompiler {

    enum class Language {
        JAVA,
        KOTLIN
    }

    private object GeneratedAnnotationTypes {
        const val jdk8 = "javax.annotation.Generated"
        const val jdk9 = "javax.annotation.processing.Generated"
    }

    private val cli = object : CliktCommand(
            name = "thrifty-compiler",
            help = "Generate Java or Kotlin code from .thrift files"
    ) {


        private val javaVersion: Double by lazy {
            val javaVersionText = System.getProperty("java.version")
            val versionNumberExpr = Regex("^(\\d+(\\.\\d+)?).*")
            val matcher = versionNumberExpr.toPattern().matcher(javaVersionText)
            if (!matcher.matches()) {
                error("Incomprehensible Java version: $javaVersionText")
            }

            val versionNumberText = matcher.group(1)
            versionNumberText.toDouble()
        }

        private val nativeGeneratedAnnotation: String by lazy {
            when {
                javaVersion <= 1.8 -> GeneratedAnnotationTypes.jdk8
                else -> GeneratedAnnotationTypes.jdk9
            }
        }

        val outputDirectory: Path by option("-o", "--out", help = "the output directory for generated files")
                .path(fileOkay = false, folderOkay = true)
                .required()
                .validate { Files.isDirectory(it) || !Files.exists(it) }

        val searchPath: List<Path> by option("-p", "--path", help = "the search path for .thrift includes")
                .path(exists = true, folderOkay = true, fileOkay = false)
                .multiple()

        val language: Language? by option(
                        "-l", "--lang", help = "the target language for generated code.  Default is java.")
                .choice("java" to Language.JAVA, "kotlin" to Language.KOTLIN)

        val nameStyle: FieldNamingPolicy by option(
                        "--name-style",
                        help = "Format style for generated names.  Default is to leave names unaltered.")
                .choice("default" to FieldNamingPolicy.DEFAULT, "java" to FieldNamingPolicy.JAVA)
                .default(FieldNamingPolicy.DEFAULT)

        val listTypeName: String? by option("--list-type", help = "when specified, the concrete type to use for lists")
        val setTypeName: String? by option("--set-type", help =  "when specified, the concrete type to use for sets")
        val mapTypeName: String? by option("--map-type", help = "when specified, the concrete type to use for maps")

        val emitNullabilityAnnotations: Boolean by option("--use-android-annotations",
                    help = "Deprecated.  When set, will add android.support nullability annotations to fields.  Equivalent to --nullability-annotation-type=android-support.")
                .flag(default = false)

        val nullabilityAnnotationType: NullabilityAnnotationType by option(
                        "--nullability-annotation-type",
                        help = "the type of nullability annotations, if any, to add to fields.  Default is none.")
                .choice(
                        "none" to NullabilityAnnotationType.NONE,
                        "android-support" to NullabilityAnnotationType.ANDROID_SUPPORT,
                        "androidx" to NullabilityAnnotationType.ANDROIDX)
                .transformAll {
                    it.lastOrNull() ?: if (emitNullabilityAnnotations) {
                        NullabilityAnnotationType.ANDROID_SUPPORT
                    } else {
                        NullabilityAnnotationType.NONE
                    }
                }

        val emitParcelable: Boolean by option("--parcelable",
                    help = "When set, generates Parcelable implementations for structs")
                .flag(default = false)

        val omitFileComments: Boolean by option("--omit-file-comments",
                    help = "When set, don't add file comments to generated files")
                .flag(default = false)

        val omitGeneratedAnnotations: Boolean by option("--omit-generated-annotations",
                    help = "When set, @Generated annotations will be suppressed")
                .flag(default = false)

        val generatedAnnotationType: String by option(
                        "--generated-annotation-type",
                        help = "JDK 9 repackaged the traditional @Generated annotation.  The current platform's annotation is used by default, unless overridden with this option")
                .choice(
                        "jdk8" to GeneratedAnnotationTypes.jdk8,
                        "jdk9" to GeneratedAnnotationTypes.jdk9,
                        "native" to nativeGeneratedAnnotation)
                .default("javax.annotation.Generated")

        val kotlinFilePerType: Boolean by option(
                    "--kt-file-per-type", help = "Generate one .kt file per type; default is one per namespace.")
                .flag(default = false)

        val kotlinBuilderlessDataClasses: Boolean by option("--experimental-kt-builderless-structs")
                .flag(default = false)

        val kotlinCoroutineClients: Boolean by option("--kt-coroutine-clients")
                .flag(default = false)

        val thriftFiles: List<Path> by argument(help = "All .thrift files to compile")
                .path(exists = true, fileOkay = true, folderOkay = false, readable = true)
                .multiple()

        private val generatedAnnotationClassName: String? by lazy {
            when {
                omitGeneratedAnnotations -> null
                else -> generatedAnnotationType
            }
        }

        override fun run() {
            if (javaVersion > 1.8 && generatedAnnotationClassName == GeneratedAnnotationTypes.jdk8) {
                TermUi.echo("WARNING: You are using Java $javaVersion, but generating code annotated with $generatedAnnotationClassName")
            }

            val loader = Loader()
            for (thriftFile in thriftFiles) {
                loader.addThriftFile(thriftFile)
            }

            loader.addIncludePath(Paths.get(System.getProperty("user.dir")))
            for (dir in searchPath) {
                loader.addIncludePath(dir)
            }

            val schema: Schema
            try {
                schema = loader.load()
            } catch (e: LoadFailedException) {
                if (!e.errorReporter.hasError && e.cause != null) {
                    println(e.cause)
                }
                for (report in e.errorReporter.formattedReports()) {
                    println(report)
                }

                Runtime.getRuntime().exit(1)
                return
            }

            val impliedLanguage = when {
                kotlinBuilderlessDataClasses -> Language.KOTLIN
                kotlinFilePerType -> Language.KOTLIN
                nullabilityAnnotationType != NullabilityAnnotationType.NONE -> Language.JAVA
                else -> null
            }

            if (language != null && impliedLanguage != null && impliedLanguage != language) {
                TermUi.echo(
                        "You specified $language, but provided options implying $impliedLanguage (which will be ignored).",
                        err = true)
            }

            if (emitNullabilityAnnotations) {
                TermUi.echo("Warning: --use-android-annotations is deprecated and superseded by the --nullability-annotation-type option.")
            }

            when (language ?: impliedLanguage) {
                null,
                Language.JAVA -> generateJava(schema)
                Language.KOTLIN -> generateKotlin(schema)
            }
        }

        private fun generateJava(schema: Schema) {
            var gen = ThriftyCodeGenerator(schema, nameStyle)
            listTypeName?.let { gen = gen.withListType(it) }
            setTypeName?.let { gen = gen.withSetType(it) }
            mapTypeName?.let { gen = gen.withMapType(it) }

            val svc = TypeProcessorService.getInstance()
            val processor = svc.javaProcessor
            if (processor != null) {
                gen = gen.usingTypeProcessor(processor)
            }

            gen.nullabilityAnnotationType(nullabilityAnnotationType)
            gen.emitFileComment(!omitFileComments)
            gen.emitParcelable(emitParcelable)
            gen.emitGeneratedAnnotations(generatedAnnotationClassName)

            gen.generate(outputDirectory)
        }

        private fun generateKotlin(schema: Schema) {
            val gen = KotlinCodeGenerator(nameStyle)
                    .emitGeneratedAnnotations(generatedAnnotationClassName)

            if (nullabilityAnnotationType != NullabilityAnnotationType.NONE) {
                TermUi.echo("Warning: Nullability annotations are unnecessary in Kotlin and will not be generated")
            }

            if (emitParcelable) {
                gen.parcelize()
            }

            if (kotlinFilePerType) {
                gen.filePerType()
            } else {
                gen.filePerNamespace()
            }

            listTypeName?.let { gen.listClassName(it) }
            setTypeName?.let { gen.setClassName(it) }
            mapTypeName?.let { gen.mapClassName(it) }

            if (kotlinBuilderlessDataClasses) {
                gen.builderlessDataClasses()
            }

            if (kotlinCoroutineClients) {
                gen.coroutineServiceClients()
            }

            val svc = TypeProcessorService.getInstance()
            svc.kotlinProcessor?.let {
                gen.processor = it
            }

            val specs = gen.generate(schema)

            specs.forEach { it.writeTo(outputDirectory) }
        }
    }

    fun compile(args: Array<String>) = cli.main(args)

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            try {
                ThriftyCompiler().compile(args)
            } catch (e: Exception) {
                TermUi.echo("Unhandled exception", err = true)
                e.printStackTrace(System.err)
                System.exit(1)
            }
        }
    }
}
