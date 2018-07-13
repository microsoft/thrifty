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
package com.microsoft.thrifty.schema;

import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;
import com.microsoft.thrifty.schema.parser.IncludeElement;
import com.microsoft.thrifty.schema.parser.ThriftFileElement;
import com.microsoft.thrifty.schema.parser.ThriftParser;
import okio.Okio;
import okio.Source;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads a {@link Schema} from a set of Thrift files and include paths.
 *
 * This is the entry-point of the Thrifty parser.
 */
public final class Loader {

    private static final PathMatcher THRIFT_PATH_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*.thrift");

    /**
     * A list of thrift files to be loaded.  If empty, all .thrift files within
     * {@link #includePaths} will be loaded.
     */
    private final List<Path> thriftFiles = new ArrayList<>();

    /**
     * The search path for imported thrift files.  If {@link #thriftFiles} is
     * empty, then all .thrift files located on the search path will be loaded.
     */
    private final Deque<Path> includePaths = new ArrayDeque<>();

    private ErrorReporter errorReporter = new ErrorReporter();

    private final LinkEnvironment environment = new LinkEnvironment(errorReporter);

    private Map<Path, Program> loadedPrograms;

    /**
     * Adds the file identified by the given string to the set of Thrift files
     * to be parsed.
     *
     * {@code file} must resolve to a regular file that exists.
     *
     * @param file the path to a Thrift file to be parsed; must exist.
     *
     * @return this loader
     *
     * @throws NullPointerException
     *         if {@code file} is {@code null}.
     *
     * @throws IllegalArgumentException
     *         if {@code file} is not a regular file.
     *
     * @deprecated Prefer {@link #addThriftFile(Path)} to this method.
     */
    @Deprecated
    public Loader addThriftFile(String file) {
        Preconditions.checkNotNull(file, "file");
        Path path = Paths.get(file);
        return addThriftFile(path);
    }

    /**
     * Adds the given path to the set of Thrift files to be parsed.
     *
     * {@code file} must be a regular file that exists.
     *
     * @param file the path to a Thrift file to be parsed; must exist.
     *
     * @return this loader
     *
     * @throws NullPointerException
     *         if {@code file} is {@code null}.
     *
     * @throws IllegalArgumentException
     *         if {@code file} is not a regular file.
     */
    public Loader addThriftFile(Path file) {
        Preconditions.checkNotNull(file, "file");
        Preconditions.checkArgument(Files.isRegularFile(file), "thrift file must be a regular file");
        thriftFiles.add(file);
        return this;
    }

    /**
     * Adds the given {@code path} to the set of directories from which included
     * files will be located.
     *
     * @param path A {@link File} identifying a directory, containing files to
     *             include.
     * @return this loader.
     *
     * @throws NullPointerException
     *         if {@code path} is {@code null}
     * @throws IllegalArgumentException
     *         if {@code path} is not an existing directory
     *
     * @deprecated Prefer {@link #addIncludePath(Path)} to this method.
     */
    @Deprecated
    public Loader addIncludePath(File path) {
        Preconditions.checkNotNull(path, "path");
        return addIncludePath(path.toPath());
    }

    /**
     * Adds the given {@code path} to the set of directories from which included
     * files will be located.
     *
     * @param path A {@link Path} identifying a directory, containing files to
     *             include.
     *
     * @return this loader.
     *
     * @throws NullPointerException
     *         if {@code path} is {@code null}
     * @throws IllegalArgumentException
     *         if {@code path} is not an existing directory
     */
    public Loader addIncludePath(Path path) {
        Preconditions.checkNotNull(path, "path");
        Preconditions.checkArgument(Files.isDirectory(path), "path must be a directory");
        includePaths.add(path.toAbsolutePath());
        return this;
    }

    /**
     * Parses all previously-given Thrift files, returning a {@link Schema}
     * containing the parse results.
     *
     * If no Thrift files were specified, then all Thrift files on the include
     * path are parsed.  If the include path is also empty, an exception is
     * thrown.
     *
     * @return a {@link Schema} containing the parsed structs, consts, etc
     *         from the specified Thrift files.
     *
     * @throws LoadFailedException
     *         if parsing fails for any reason.
     */
    public Schema load() throws LoadFailedException {
        try {
            loadFromDisk();
            linkPrograms();
            return new Schema(loadedPrograms.values());
        } catch (Exception e) {
            throw new LoadFailedException(e, errorReporter);
        }
    }

    ErrorReporter errorReporter() {
        return errorReporter;
    }

    private void loadFromDisk() throws IOException {
        final List<Path> filesToLoad = new ArrayList<>(thriftFiles);
        if (filesToLoad.isEmpty()) {
            for (Path path : includePaths) {
                Files.walk(path)
                        .filter(p -> p.getFileName() != null)
                        .filter(p -> THRIFT_PATH_MATCHER.matches(p.getFileName()))
                        .map(p -> p.normalize().toAbsolutePath())
                        .forEach(filesToLoad::add);
            }
        }

        if (filesToLoad.isEmpty()) {
            throw new IllegalStateException("No files and no include paths containing Thrift files were provided");
        }

        Map<Path, ThriftFileElement> loadedFiles = new LinkedHashMap<>();
        for (Path path : filesToLoad) {
            loadFileRecursively(path, loadedFiles);
        }

        // Convert to Programs
        loadedPrograms = new LinkedHashMap<>();
        for (ThriftFileElement fileElement : loadedFiles.values()) {
            Path file = Paths.get(fileElement.location().base(), fileElement.location().path());
            if (!Files.exists(file)) {
                throw new AssertionError(
                        "We have a parsed ThriftFileElement with a non-existing location");
            }
            if (!file.isAbsolute()) {
                throw new AssertionError("We have a non-canonical path");
            }
            Program program = new Program(fileElement);
            loadedPrograms.put(file.normalize().toAbsolutePath(), program);
        }

        // Link included programs together
        Set<Program> visited = new HashSet<>(loadedPrograms.size());
        for (Program program : loadedPrograms.values()) {
            program.loadIncludedPrograms(this, visited);
        }
    }

    /**
     * Loads and parses a Thrift file and all files included (both directly and
     * transitively) by it.
     *
     * @param path A relative or absolute path to a Thrift file.
     * @param loadedFiles A mapping of absolute paths to parsed Thrift files.
     */
    private void loadFileRecursively(Path path, Map<Path, ThriftFileElement> loadedFiles) throws IOException {
        ThriftFileElement element = null;
        Path dir = null;

        Path file = findFirstExisting(path, null);

        if (file != null) {
            // Resolve symlinks, redundant '.' and '..' segments.
            file = file.normalize();

            if (loadedFiles.containsKey(file.toAbsolutePath())) {
                return;
            }

            dir = file.getParent();
            element = loadSingleFile(dir, file.getFileName());
        }

        if (element == null) {
            throw new FileNotFoundException(
                    "Failed to locate " + path + " in " + includePaths);
        }

        loadedFiles.put(file.normalize().toAbsolutePath(), element);

        List<IncludeElement> includes = element.includes();
        if (includes.size() > 0) {
            includePaths.addFirst(dir);
            for (IncludeElement include : includes) {
                if (!include.isCpp()) {
                    loadFileRecursively(Paths.get(include.path()), loadedFiles);
                }
            }
            includePaths.removeFirst();
        }
    }

    private void linkPrograms() {
        synchronized (environment) {
            for (Program program : loadedPrograms.values()) {
                Linker linker = environment.getLinker(program);
                linker.link();
            }

            if (environment.hasErrors()) {
                throw new IllegalStateException("Linking failed");
            }
        }
    }

    private ThriftFileElement loadSingleFile(Path base, Path fileName) throws IOException {
        Path file = base.resolve(fileName);
        if (!Files.exists(file)) {
            return null;
        }

        Source source = Okio.source(file);
        try {
            Location location = Location.Companion.get(base.toString(), fileName.toString());
            String data = Okio.buffer(source).readUtf8();
            return ThriftParser.parse(location, data, errorReporter);
        } catch (IOException e) {
            throw new IOException("Failed to load " + fileName + " from " + base, e);
        } finally {
            Closeables.close(source, true);
        }
    }

    Program resolveIncludedProgram(Location currentPath, String importPath) {
        Path importPathPath = Paths.get(importPath);
        Path resolved = findFirstExisting(importPathPath, currentPath);
        if (resolved == null) {
            throw new AssertionError("Included thrift file not found: " + importPath);
        }
        return getAndCheck(resolved.normalize().toAbsolutePath());
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
     * @return the first matching file on the search path, or {@code null}.
     */
    private Path findFirstExisting(Path path, @Nullable  Location currentLocation) {
        if (path.isAbsolute()) {
            // absolute path, should be loaded as-is
            return Files.exists(path) ? path : null;
        }

        if (currentLocation != null) {
            Path maybePath = Paths.get(currentLocation.base(), path.toString());
            if (Files.exists(maybePath)) {
                return maybePath;
            }
        }

        return includePaths.stream()
                .map(include -> include.resolve(path).normalize())
                .filter(Files::exists)
                .findFirst()
                .orElse(null);
    }

    private Program getAndCheck(Path absolutePath) {
        Program p = loadedPrograms.get(absolutePath);
        if (p == null) {
            throw new AssertionError("All includes should have been resolved by now: " + absolutePath);
        }
        return p;
    }
}
