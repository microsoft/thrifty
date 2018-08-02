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
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.microsoft.thrifty.gen.ThriftyCodeGenerator
import com.microsoft.thrifty.kgen.KotlinCodeGenerator
import com.microsoft.thrifty.schema.FieldNamingPolicy
import com.microsoft.thrifty.schema.LoadFailedException
import com.microsoft.thrifty.schema.Loader
import com.microsoft.thrifty.schema.Schema
import java.nio.file.FileSystems
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
 * If no .thrift files are given, then all .thrift files located on the search path
 * will be implicitly included; otherwise only the given files (and those included by them)
 * will be compiled.
 */
class ThriftyCompiler {

    private val cli = object : CliktCommand(name = "thrifty-compiler") {
        val outputDirectory: Path by option("-o", "--out", help = "the output directory for generated files")
                .path(fileOkay = false, folderOkay = true)
                .required()
                .validate { Files.isDirectory(it) || !Files.exists(it) }

        val searchPath: List<Path> by option("-p", "--path", help = "the search path for .thrift includes")
                .path(exists = true, folderOkay = true, fileOkay = false)
                .multiple()

        val listTypeName: String? by option("--list-type", help = "when specified, the concrete type to use for lists")
        val setTypeName: String? by option("--set-type", help =  "when specified, the concrete type to use for sets")
        val mapTypeName: String? by option("--map-type", help = "when specified, the concrete type to use for maps")

        val emitNullabilityAnnotations: Boolean by option("--use-android-annotations",
                    help = "When set, will add android.support nullability annotations to fields")
                .flag(default = false)

        val emitParcelable: Boolean by option("--parcelable",
                    help = "When set, generates Parcelable implementations for structs")
                .flag(default = false)

        val useJavaStyleNames by option("--use-java-style-names",
                    help = "When set, converts field names to standard java camelCase")
                .flag(default = false)

        val emitKotlin: Boolean by option("--kotlin", help = "Generate Kotlin instead of Java")
                .flag(default = false)

        val kotlinFilePerType: Boolean by option(
                    "--kt-file-per-type", help = "Generate one .kt file per type; default is one per namespace.")
                .flag(default = false)

        val thriftFiles: List<Path> by argument(help = "All .thrift files to compile")
                .path(exists = true, fileOkay = true, folderOkay = false, readable = true)
                .multiple()

        override fun run() {
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
                for (report in e.errorReporter.formattedReports()) {
                    println(report)
                }

                Runtime.getRuntime().exit(1)
                return
            }

            val fieldNamingPolicy = if (useJavaStyleNames) FieldNamingPolicy.JAVA else FieldNamingPolicy.DEFAULT

            if (emitKotlin) {
                generateKotlin(schema, fieldNamingPolicy)
            } else {
                generateJava(schema, fieldNamingPolicy)
            }
        }

        private fun generateJava(schema: Schema, fieldNamingPolicy: FieldNamingPolicy) {
            var gen = ThriftyCodeGenerator(schema, fieldNamingPolicy)
            listTypeName?.let { gen = gen.withListType(it) }
            setTypeName?.let { gen = gen.withSetType(it) }
            mapTypeName?.let { gen = gen.withMapType(it) }

            val svc = TypeProcessorService.getInstance()
            val processor = svc.javaProcessor
            if (processor != null) {
                gen = gen.usingTypeProcessor(processor)
            }

            gen.emitAndroidAnnotations(emitNullabilityAnnotations)
            gen.emitParcelable(emitParcelable)

            gen.generate(outputDirectory)
        }

        private fun generateKotlin(schema: Schema, fieldNamingPolicy: FieldNamingPolicy) {
            val gen = KotlinCodeGenerator(fieldNamingPolicy)

            if (kotlinFilePerType) {
                gen.filePerType()
            } else {
                gen.filePerNamespace()
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
