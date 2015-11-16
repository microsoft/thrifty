package com.bendb.thrifty;

import com.bendb.thrifty.parser.IncludeElement;
import com.bendb.thrifty.parser.ThriftFileElement;
import com.bendb.thrifty.parser.ThriftParser;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import okio.Okio;
import okio.Source;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
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
    private final List<File> includePaths = new ArrayList<>();

    public Loader addThriftFile(String file) {
        Preconditions.checkNotNull(file, "file");
        thriftFiles.add(file);
        return this;
    }

    public Loader addIncludePath(File path) {
        Preconditions.checkNotNull(path, "path");
        Preconditions.checkArgument(path.isDirectory(), "path must be a directory");
        includePaths.add(path);
        return this;
    }

    private Set<Program> loadFromDisk() throws IOException {
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
            for (File base : includePaths) {
                File resolved = new File(base, path).getAbsoluteFile();
                if (!resolved.exists()) {
                    continue;
                }

                Source src = Okio.source(resolved);
                try {
                    Location location = Location.get(base.toString(), path);
                    String data = Okio.buffer(src).readUtf8();
                    element = ThriftParser.parse(location, data);
                    break;
                } catch (IOException e) {
                    throw new IOException("Failed to load " + path + " from " + base, e);
                } finally {
                    Closeables.close(src, true);
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

        return Sets.newHashSet();
    }

    private static final Predicate<File> IS_THRIFT = new Predicate<File>() {
        @Override
        public boolean apply(@Nullable File input) {
            return input != null && input.getName().endsWith(".thrift");
        }
    };
}
