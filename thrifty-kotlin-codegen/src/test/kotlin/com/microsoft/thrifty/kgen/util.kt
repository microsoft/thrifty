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
package com.microsoft.thrifty.kgen

import com.squareup.kotlinpoet.FileSpec
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteRecursively

fun List<FileSpec>.shouldCompile() {
    this should compile()
}

fun FileSpec.shouldCompile() {
    listOf(this) should compile()
}

fun compile(): ShouldCompileMatcher {
    return ShouldCompileMatcher()
}

open class ShouldCompileMatcher : Matcher<List<FileSpec>> {
    private val collector = LogEverythingMessageCollector()
    private var debugLoggingEnabled = false

    fun withDebugLogging(): ShouldCompileMatcher {
        debugLoggingEnabled = true
        return this
    }
    override fun test(value: List<FileSpec>): MatcherResult {
        return withTempDir { dir ->
            val code = compileKotlin(dir, value)
            MatcherResult(
                code == ExitCode.OK,
                { formatCompilerErrors(collector) },
                { "compilation should have failed but did not" }
            )
        }
    }

    private fun formatCompilerErrors(collector: LogEverythingMessageCollector): String {
        return buildString {
            append("compilation failed:")
            for (message in collector.messages.filter { isSeverityPrintable(it.severity) }) {
                append("\n\t")
                append(message)
            }
            append("\n")
        }
    }

    private fun isSeverityPrintable(sev: CompilerMessageSeverity): Boolean {
        return debugLoggingEnabled || sev.isError
    }

    // 'deleteRecursively' is far more convenient than any other option, but is
    // unfortunately still "experimental".
    @OptIn(ExperimentalPathApi::class)
    private inline fun <T> withTempDir(fn: (Path) -> T): T {
        val tempDir = Files.createTempDirectory("kotlin-compile")
        try {
            return fn(tempDir)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun compileKotlin(rootDir: Path, files: List<FileSpec>): ExitCode {
        val sources = Files.createDirectory(rootDir.resolve("src"))
        val output = Files.createDirectory(rootDir.resolve("bin"))

        files.forEach { it.writeTo(sources) }

        val args = K2JVMCompilerArguments().apply {
            destination = output.absolutePathString()
            freeArgs += sources.absolutePathString()
            classpath = System.getProperty("java.class.path")

            // This tells the compiler not to try to find kotlin-stdlib and kotlin-reflect
            // on our local machine; it's not guaranteed that any given machine will have
            // a predictable kotlin home.  Moreover, *we already have these on the classpath*
            // and so they'll be available anyway.
            noStdlib = true
            noReflect = true

            // we have a Need for Speed
            useK2 = true
            noOptimize = true
            useFastJarFileSystem = true
            useIR = true
        }

        return K2JVMCompiler().exec(collector, Services.EMPTY, args)
    }
}

data class Message(
    val severity: CompilerMessageSeverity,
    val text: String,
    val location: CompilerMessageSourceLocation?
) {
    override fun toString() = buildString {
        append(severity.presentableName[0])
        append(": ")
        append(text)
        if (location != null) {
            append("(")
            append(location.path)
            append(":")
            append(location.line)
            append(")")
        }
    }
}

private class LogEverythingMessageCollector : MessageCollector {
    private val messageArrayList = arrayListOf<Message>()

    val messages: List<Message>
        get() = messageArrayList

    override fun clear() {
        messageArrayList.clear()
    }

    override fun hasErrors(): Boolean {
        return messageArrayList.isNotEmpty()
    }

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?
    ) {
        messageArrayList += Message(severity, message, location)
    }
}
