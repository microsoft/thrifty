package com.bendb.thrifty;

import com.bendb.thrifty.parser.IncludeElement;
import com.bendb.thrifty.parser.ThriftFileElement;
import com.bendb.thrifty.parser.ThriftParser;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import okio.Okio;
import okio.Source;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public final class Loader {
    /**
     * A list of thrift files to be loaded.  If empty, all .thrift files within
     * {@link #includePaths} will be loaded.
     */
    private final List<String> thriftFiles = new ArrayList<>();

    /**
     * The search path for imported thrift files.  If {@link #thriftFiles} is
     * empty, then all .thrift files located on the search path will be loaded.
     */
    private final List<File> includePaths = new ArrayList<>();

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

    private void loadFromDisk() throws IOException {
        final Deque<String> filesToLoad = new ArrayDeque<>(thriftFiles);
        if (filesToLoad.isEmpty()) {
            for (File file : includePaths) {
                FluentIterable<File> iterable = Files.fileTreeTraverser()
                        .breadthFirstTraversal(file)
                        .filter(IS_THRIFT);

                for (File thriftFile : iterable) {
                    filesToLoad.add(thriftFile.getAbsolutePath());
                }
            }
        }

        Map<String, ThriftFileElement> loadedFiles = new LinkedHashMap<>();
        while (!filesToLoad.isEmpty()) {
            String path = filesToLoad.removeFirst();
            if (loadedFiles.containsKey(path)) {
                continue;
            }

            ThriftFileElement element = null;

            if (path.startsWith("/")) {
                File file = new File(path);
                File dir = file.getParentFile();
                element = loadSingleFile(dir, file.getName());
            } else {
                for (File base : includePaths) {
                    element = loadSingleFile(base, path);
                    if (element != null) {
                        break;
                    }
                }
            }

            if (element == null) {
                throw new FileNotFoundException(
                        "Failed to locate " + path + " in " + includePaths);
            }

            loadedFiles.put(path, element);

            for (IncludeElement include : element.includes()) {
                filesToLoad.addLast(include.path());
            }
        }

        // Convert to Programs
        List<Program> programs = new ArrayList<>();
        for (ThriftFileElement fileElement : loadedFiles.values()) {
            programs.add(new Program(fileElement));
        }

        // Load symbols
        Set<Program> visited = new HashSet<>(programs.size());
        for (Program program : programs) {
            program.loadSymbols(this, visited);
        }

        loadedPrograms = new HashMap<>();
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
            return ThriftParser.parse(location, data);
        } catch (IOException e) {
            throw new IOException("Failed to load " + path + " from " + base, e);
        } finally {
            Closeables.close(source, true);
        }
    }

    Program resolveImport(Location currentPath, String importPath) {
        File resolved = findFirstExisting(importPath, currentPath);
        if (resolved == null) {
            throw new AssertionError("Included thrift file not found: " + importPath);
        }
        return getAndCheck(resolved.getAbsolutePath());
    }

    /**
     * Resolves a relative path to the first existing match.
     *
     * Resolution rules favor, in order:
     * 1. Absolute paths
     * 2. The current working location, if given
     * 3. The include path, in the order given.
     *
     * @param path
     * @param currentLocation
     * @return
     */
    private File findFirstExisting(String path, Location currentLocation) {
        if (path.startsWith("/")) {
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

    private static final Predicate<File> IS_THRIFT = new Predicate<File>() {
        @Override
        public boolean apply(@Nullable File input) {
            return input != null && input.getName().endsWith(".thrift");
        }
    };
}
