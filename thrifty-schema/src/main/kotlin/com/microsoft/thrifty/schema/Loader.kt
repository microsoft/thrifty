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
package com.microsoft.thrifty.schema

import com.google.common.base.Preconditions
import com.microsoft.thrifty.schema.parser.ThriftFileElement
import com.microsoft.thrifty.schema.parser.ThriftParser
import okio.Okio
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayDeque
import java.util.HashSet
import java.util.LinkedHashMap

/**
 * Loads a [Schema] from a set of Thrift files and include paths.
 *
 * This is the entry-point of the Thrifty parser.
 */
class Loader {

    /**
     * A list of thrift files to be loaded.  If empty, all .thrift files within
     * [.includePaths] will be loaded.
     */
    private val thriftFiles = mutableListOf<Path>()

    /**
     * The search path for imported thrift files.  If [.thriftFiles] is
     * empty, then all .thrift files located on the search path will be loaded.
     */
    private val includePaths = ArrayDeque<Path>()

    private val errorReporter = ErrorReporter()

    private val environment = LinkEnvironment(errorReporter)

    private val loadedPrograms = mutableMapOf<Path, Program>()

    /**
     * Adds the given path to the set of Thrift files to be parsed.
     *
     * `file` must be a regular file that exists.
     *
     * @param file the path to a Thrift file to be parsed; must exist.
     *
     * @return this loader
     *
     * @throws NullPointerException
     * if `file` is `null`.
     *
     * @throws IllegalArgumentException
     * if `file` is not a regular file.
     */
    fun addThriftFile(file: Path): Loader = apply {
        Preconditions.checkNotNull(file, "file")
        Preconditions.checkArgument(Files.isRegularFile(file), "thrift file must be a regular file")
        thriftFiles.add(file)
    }

    /**
     * Adds the given `path` to the set of directories from which included
     * files will be located.
     *
     * @param path A [Path] identifying a directory, containing files to
     * include.
     *
     * @return this loader.
     *
     * @throws NullPointerException
     * if `path` is `null`
     * @throws IllegalArgumentException
     * if `path` is not an existing directory
     */
    fun addIncludePath(path: Path): Loader = apply {
        Preconditions.checkNotNull(path, "path")
        Preconditions.checkArgument(Files.isDirectory(path), "path must be a directory")
        includePaths.add(path.toAbsolutePath())
    }

    /**
     * Parses all previously-given Thrift files, returning a [Schema]
     * containing the parse results.
     *
     * If no Thrift files were specified, then all Thrift files on the include
     * path are parsed.  If the include path is also empty, an exception is
     * thrown.
     *
     * @return a [Schema] containing the parsed structs, consts, etc
     * from the specified Thrift files.
     *
     * @throws LoadFailedException
     * if parsing fails for any reason.
     */
    fun load(): Schema {
        try {
            loadFromDisk()
            linkPrograms()
            return Schema(loadedPrograms.values)
        } catch (e: Exception) {
            throw LoadFailedException(e, errorReporter)
        }
    }

    internal fun errorReporter(): ErrorReporter {
        return errorReporter
    }

    private fun loadFromDisk() {
        val filesToLoad = thriftFiles.toMutableList()
        if (filesToLoad.isEmpty()) {
            for (path in includePaths) {
                Files.walk(path)
                        .filter { p -> p.fileName != null && THRIFT_PATH_MATCHER.matches(p.fileName) }
                        .map { p -> p.normalize().toAbsolutePath() }
                        .forEach { filesToLoad.add(it) }
            }
        }

        if (filesToLoad.isEmpty()) {
            throw IllegalStateException("No files and no include paths containing Thrift files were provided")
        }

        val loadedFiles = LinkedHashMap<Path, ThriftFileElement>()
        for (path in filesToLoad) {
            loadFileRecursively(path, loadedFiles)
        }

        // Convert to Programs
        for (fileElement in loadedFiles.values) {
            val file = Paths.get(fileElement.location.base, fileElement.location.path)
            if (!Files.exists(file)) {
                throw AssertionError(
                        "We have a parsed ThriftFileElement with a non-existing location")
            }
            if (!file.isAbsolute) {
                throw AssertionError("We have a non-canonical path")
            }
            val program = Program(fileElement)
            loadedPrograms[file.normalize().toAbsolutePath()] = program
        }

        // Link included programs together
        val visited = HashSet<Program>(loadedPrograms.size)
        for (program in loadedPrograms.values) {
            program.loadIncludedPrograms(this, visited)
        }
    }

    /**
     * Loads and parses a Thrift file and all files included (both directly and
     * transitively) by it.
     *
     * @param path A relative or absolute path to a Thrift file.
     * @param loadedFiles A mapping of absolute paths to parsed Thrift files.
     */
    private fun loadFileRecursively(path: Path, loadedFiles: MutableMap<Path, ThriftFileElement>) {
        val dir: Path?

        val element: ThriftFileElement
        val file = findFirstExisting(path, null)?.normalize()
        if (file != null) {
            // Resolve symlinks, redundant '.' and '..' segments.
            if (loadedFiles.containsKey(file.toAbsolutePath())) {
                return
            }

            dir = file.parent
            element = loadSingleFile(dir!!, file.fileName) ?: throw FileNotFoundException("Failed to locate $path in $includePaths")
        } else {
            throw FileNotFoundException("Failed to locate $path in $includePaths")
        }

        loadedFiles[file.normalize().toAbsolutePath()] = element

        if (element.includes.isNotEmpty()) {
            includePaths.addFirst(dir)
            for (include in element.includes) {
                if (!include.isCpp) {
                    loadFileRecursively(Paths.get(include.path), loadedFiles)
                }
            }
            includePaths.removeFirst()
        }
    }

    private fun linkPrograms() {
        synchronized(environment) {
            for (program in loadedPrograms.values) {
                val linker = environment.getLinker(program)
                linker.link()
            }

            if (environment.hasErrors) {
                throw IllegalStateException("Linking failed")
            }
        }
    }

    private fun loadSingleFile(base: Path, fileName: Path): ThriftFileElement? {
        val file = base.resolve(fileName)
        if (!Files.exists(file)) {
            return null
        }

        Okio.source(file).use { source ->
            try {
                val location = Location.get("$base", "$fileName")
                val data = Okio.buffer(source).readUtf8()
                return ThriftParser.parse(location, data, errorReporter)
            } catch (e: IOException) {
                throw IOException("Failed to load $fileName from $base", e)
            }
        }
    }

    internal fun resolveIncludedProgram(currentPath: Location, importPath: String): Program {
        val importPathPath = Paths.get(importPath)
        val resolved = findFirstExisting(importPathPath, currentPath)
                ?: throw AssertionError("Included thrift file not found: $importPath")
        return getProgramForPath(resolved.normalize().toAbsolutePath())
    }

    /**
     * Resolves a relative path to the first existing match.
     *
     * Resolution rules favor, in order:
     * 1. Absolute paths
     * 2. The current working location, if given
     * 3. The include path, in the order given.
     *
     * @param path a relative or absolute path to the file being sought.
     * @param currentLocation the current working directory.
     * @return the first matching file on the search path, or `null`.
     */
    private fun findFirstExisting(path: Path, currentLocation: Location?): Path? {
        if (path.isAbsolute) {
            // absolute path, should be loaded as-is
            return if (Files.exists(path)) path else null
        }

        if (currentLocation != null) {
            val maybePath = Paths.get(currentLocation.base, path.toString())
            if (Files.exists(maybePath)) {
                return maybePath
            }
        }

        return includePaths
                .map { it.resolve(path).normalize() }
                .firstOrNull { Files.exists(it) }
    }

    private fun getProgramForPath(absolutePath: Path): Program {
        require(absolutePath.isAbsolute) {
            "Why are you calling getProgramForPath with a relative path?  path=$absolutePath"
        }
        return loadedPrograms[absolutePath]
                ?: throw AssertionError("All includes should have been resolved by now: $absolutePath")
    }
}

private val THRIFT_PATH_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*.thrift")
