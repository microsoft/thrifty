package com.bendb.thrifty;

import com.bendb.thrifty.parser.ThriftFileElement;
import com.bendb.thrifty.parser.ThriftParser;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import okio.Okio;
import okio.Source;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final List<Path> includePaths = new ArrayList<>();

    public Loader addThriftFile(String file) {
        Preconditions.checkNotNull(file, "file");
        thriftFiles.add(file);
        return this;
    }

    public Loader addIncludePath(Path path) {
        Preconditions.checkNotNull(path, "path");
        Preconditions.checkArgument(Files.isDirectory(path), "path must be a directory");
        includePaths.add(path);
        return this;
    }

    private Set<Program> loadFromDisk() throws IOException {
        final Deque<String> filesToLoad = new ArrayDeque<>(thriftFiles);
        if (filesToLoad.isEmpty()) {
            for (final Path path : includePaths) {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(
                            Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.getFileName().toString().endsWith(".thrift")) {
                            filesToLoad.add(path.relativize(file).toString());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }

        Map<String, ThriftFileElement> loadedFiles = new LinkedHashMap<>();
        while (!filesToLoad.isEmpty()) {
            String path = filesToLoad.removeFirst();
            if (loadedFiles.containsKey(path)) {
                continue;
            }

            ThriftFileElement element = null;
            for (Path base : includePaths) {
                Source src = source(path, base);
                if (src == null) {
                    continue;
                }

                try {
                    Location location = Location.get(base.toString(), path);
                    String data = Okio.buffer(src).readUtf8();
                    element = ThriftParser.parse(location, data);
                } catch (IOException e) {
                    throw new IOException("Failed to load " + path + " from " + base, e);
                } finally {
                    src.close();
                }

                if (element == null) {
                    throw new FileNotFoundException(
                            "Failed to locate " + path + " in " + includePaths);
                }
            }
        }

        return Sets.newHashSet();
    }

    private static Source source(String thrift, Path directory) throws IOException {
        Path resolved = directory.resolve(thrift);
        if (Files.exists(resolved)) {
            return Okio.source(resolved);
        }
        return null;
    }
}
