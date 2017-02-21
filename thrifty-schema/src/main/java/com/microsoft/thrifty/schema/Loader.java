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
import com.google.common.collect.ImmutableList;
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
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class Loader {
    /**
     * Attempts to identify strings that represent absolute filesystem paths.
     * Does not attempt to support more unusual paths like UNC ("\\c\path") or
     * filesystem URIs ("file:///c/path").
     */
    private static final Pattern ABSOLUTE_PATH_PATTERN = Pattern.compile("^(/|\\w:\\\\).*");

    /**
     * A list of thrift files to be loaded.  If empty, all .thrift files within
     * {@link #includePaths} will be loaded.
     */
    private final List<String> thriftFiles = new ArrayList<>();

    /**
     * The search path for imported thrift files.  If {@link #thriftFiles} is
     * empty, then all .thrift files located on the search path will be loaded.
     */
    private final Deque<File> includePaths = new ArrayDeque<>();

    private ErrorReporter errorReporter = new ErrorReporter();

    private final LinkEnvironment environment = new LinkEnvironment(errorReporter);

    private Map<String, Program> loadedPrograms;

    public Loader addThriftFile(String file) {
        Preconditions.checkNotNull(file, "file");
        thriftFiles.add(file);
        return this;
    }

    public Loader addIncludePath(File path) {
        Preconditions.checkNotNull(path, "path");
        Preconditions.checkArgument(path.isDirectory(), "path must be a directory");
        includePaths.add(path.getAbsoluteFile());
        return this;
    }

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
        final List<String> filesToLoad = new ArrayList<>(thriftFiles);
        if (filesToLoad.isEmpty()) {
            for (File file : includePaths) {
                Files.walk(file.toPath())
                        .filter(path -> path.getFileName() != null)
                        .filter(path -> path.getFileName().endsWith(".thrift"))
                        .map(path -> path.normalize().toAbsolutePath().toString())
                        .forEach(filesToLoad::add);
            }
        }

        Map<String, ThriftFileElement> loadedFiles = new LinkedHashMap<>();
        for (String path : filesToLoad) {
            loadFileRecursively(path, loadedFiles);
        }

        // Convert to Programs
        loadedPrograms = new LinkedHashMap<>();
        for (ThriftFileElement fileElement : loadedFiles.values()) {
            File file = new File(fileElement.location().base(), fileElement.location().path());
            if (!file.exists()) throw new AssertionError(
                    "We have a parsed ThriftFileElement with a non-existing location");
            if (!file.isAbsolute()) throw new AssertionError("We have a non-canonical path");
            Program program = new Program(fileElement);
            loadedPrograms.put(file.getCanonicalPath(), program);
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
    private void loadFileRecursively(String path, Map<String, ThriftFileElement> loadedFiles) throws IOException {
        ThriftFileElement element = null;
        File dir = null;

        File file = findFirstExisting(path, null);

        if (file != null) {
            // Resolve symlinks, redundant '.' and '..' segments.
            file = file.getCanonicalFile();

            if (loadedFiles.containsKey(file.getAbsolutePath())) {
                return;
            }

            dir = file.getParentFile();
            element = loadSingleFile(file.getParentFile(), file.getName());
        }

        if (element == null) {
            throw new FileNotFoundException(
                    "Failed to locate " + path + " in " + includePaths);
        }

        loadedFiles.put(file.getAbsolutePath(), element);

        ImmutableList<IncludeElement> includes = element.includes();
        if (includes.size() > 0) {
            includePaths.addFirst(dir);
            for (IncludeElement include : includes) {
                if (!include.isCpp()) {
                    loadFileRecursively(include.path(), loadedFiles);
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

    private ThriftFileElement loadSingleFile(File base, String path) throws IOException {
        File file = new File(base, path).getAbsoluteFile();
        if (!file.exists()) {
            return null;
        }

        Source source = Okio.source(file);
        try {
            Location location = Location.get(base.toString(), path);
            String data = Okio.buffer(source).readUtf8();
            return ThriftParser.parse(location, data, errorReporter);
        } catch (IOException e) {
            throw new IOException("Failed to load " + path + " from " + base, e);
        } finally {
            Closeables.close(source, true);
        }
    }

    Program resolveIncludedProgram(Location currentPath, String importPath) throws IOException {
        File resolved = findFirstExisting(importPath, currentPath);
        if (resolved == null) {
            throw new AssertionError("Included thrift file not found: " + importPath);
        }
        try {
            return getAndCheck(resolved.getCanonicalPath());
        } catch (IOException e) {
            throw new IOException("Failed to get canonical path for file " + resolved.getAbsolutePath(), e);
        }
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
    private File findFirstExisting(String path, @Nullable  Location currentLocation) {
        if (isAbsolutePath(path)) {
            // absolute path, should be loaded as-is
            File f = new File(path);
            return f.exists() ? f : null;
        }

        if (currentLocation != null) {
            File maybeFile = new File(currentLocation.base(), path).getAbsoluteFile();
            if (maybeFile.exists()) {
                return maybeFile;
            }
        }

        for (File includePath : includePaths) {
            File maybeFile = new File(includePath, path).getAbsoluteFile();
            if (maybeFile.exists()) {
                return maybeFile;
            }
        }

        return null;
    }

    private Program getAndCheck(String absolutePath) {
        Program p = loadedPrograms.get(absolutePath);
        if (p == null) {
            throw new AssertionError("All includes should have been resolved by now: " + absolutePath);
        }
        return p;
    }

    /**
     * Checks if the path is absolute in an attempted cross-platform manner.
     */
    private static boolean isAbsolutePath(String path) {
        return ABSOLUTE_PATH_PATTERN.matcher(path).matches();
    }
}
