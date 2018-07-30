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
package com.microsoft.thrifty.compiler;

import com.microsoft.thrifty.compiler.spi.TypeProcessor;
import com.microsoft.thrifty.gen.ThriftyCodeGenerator;
import com.microsoft.thrifty.schema.FieldNamingPolicy;
import com.microsoft.thrifty.schema.LoadFailedException;
import com.microsoft.thrifty.schema.Loader;
import com.microsoft.thrifty.schema.Schema;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * A program that compiles Thrift IDL files into Java source code for use
 * with thrifty-runtime.
 *
 * <pre>{@code
 * java -jar thrifty-compiler.jar --out=/path/to/output
 *         [--path=dir/for/search/path]
 *         [--list-type=java.util.ArrayList]
 *         [--set-type=java.util.HashSet]
 *         [--map-type=java.util.HashMap]
 *         file1.thrift
 *         file2.thrift
 *         ...
 * }</pre>
 *
 * <p>{@code --out} is required, and specifies the directory to which generated
 * Java sources will be written.
 *
 * <p>{@code --path} can be given multiple times.  Each directory so specified
 * will be placed on the search path.  When resolving {@code include} statements
 * during thrift compilation, these directories will be searched for included files.
 *
 * <p>{@code --list-type} is optional.  When provided, the compiler will use the given
 * class name when instantiating list-typed values.  Defaults to {@link ArrayList}.
 *
 * <p>{@code --set-type} is optional.  When provided, the compiler will use the given
 * class name when instantiating set-typed values.  Defaults to {@link java.util.HashSet}.
 *
 * <p>{@code --map-type} is optional.  When provided, the compiler will use the given
 * class name when instantiating map-typed values.  Defaults to {@link java.util.HashMap}.
 * Android users will likely wish to substitute {@code android.support.v4.util.ArrayMap}.
 *
 * <p>If no .thrift files are given, then all .thrift files located on the search path
 * will be implicitly included; otherwise only the given files (and those included by them)
 * will be compiled.
 */
public class ThriftyCompiler {
    private static final String OUT_PREFIX = "--out=";
    private static final String PATH_PREFIX = "--path=";
    private static final String LIST_TYPE_PREFIX = "--list-type=";
    private static final String SET_TYPE_PREFIX = "--set-type=";
    private static final String MAP_TYPE_PREFIX = "--map-type=";
    private static final String NULLABILITY_ARG = "--use-android-annotations";
    private static final String PARCELABLE_ARG = "--parcelable";
    private static final String JAVA_NAMES_ARG = "--use-java-style-names";

    private Path outputDirectory;
    private List<Path> thriftFiles = new ArrayList<>();
    private List<Path> searchPath = new ArrayList<>();
    private String listTypeName;
    private String setTypeName;
    private String mapTypeName;
    private boolean emitNullabilityAnnotations = false;
    private boolean emitParcelable = false;
    private FieldNamingPolicy fieldNamingPolicy = FieldNamingPolicy.DEFAULT;

    public static void main(String[] args) {
        try {
            ThriftyCompiler compiler = withArgs(args);
            compiler.searchPath.add(0, Paths.get(System.getProperty("user.dir")));
            compiler.compile();
        } catch (Exception e) {
            System.err.println("Unhandled exception:");
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public static ThriftyCompiler withArgs(String[] args) {
        ThriftyCompiler compiler = new ThriftyCompiler();

        for (String arg : args) {
            if (arg.startsWith(OUT_PREFIX)) {
                String path = arg.substring(OUT_PREFIX.length());
                compiler.setOutputDirectory(Paths.get(path));
            } else if (arg.startsWith(PATH_PREFIX)) {
                String dirname = arg.substring(PATH_PREFIX.length());
                compiler.addSearchDirectory(dirname);
            } else if (arg.startsWith(LIST_TYPE_PREFIX)) {
                String typename = arg.substring(LIST_TYPE_PREFIX.length());
                compiler.setListType(typename);
            } else if (arg.startsWith(SET_TYPE_PREFIX)) {
                String typename = arg.substring(SET_TYPE_PREFIX.length());
                compiler.setSetType(typename);
            } else if (arg.startsWith(MAP_TYPE_PREFIX)) {
                String typename = arg.substring(MAP_TYPE_PREFIX.length());
                compiler.setMapType(typename);
            } else if (arg.trim().equals(NULLABILITY_ARG)) {
                compiler.emitNullabilityAnnotations = true;
            } else if (arg.trim().equals(PARCELABLE_ARG)) {
                compiler.emitParcelable = true;
            } else if (arg.trim().equals(JAVA_NAMES_ARG)) {
                compiler.fieldNamingPolicy = FieldNamingPolicy.JAVA;
            } else if (arg.startsWith("-")) {
                throw new IllegalArgumentException("Unrecognized argument: " + arg);
            } else {
                compiler.addThriftFile(arg);
            }
        }

        if (compiler.outputDirectory == null) {
            throw new IllegalArgumentException("Output path must be provided (missing --out=path)");
        }

        return compiler;
    }

    public ThriftyCompiler addThriftFile(String path) {
        thriftFiles.add(Paths.get(path));
        return this;
    }

    public ThriftyCompiler addSearchDirectory(String path) {
        searchPath.add(Paths.get(path));
        return this;
    }

    public ThriftyCompiler setListType(String typename) {
        listTypeName = typename;
        return this;
    }

    public ThriftyCompiler setSetType(String typename) {
        setTypeName = typename;
        return this;
    }

    public ThriftyCompiler setMapType(String typename) {
        mapTypeName = typename;
        return this;
    }

    public ThriftyCompiler setOutputDirectory(Path directory) {
        outputDirectory = directory;
        return this;
    }

    public void compile() throws IOException {
        Loader loader = new Loader();
        for (Path thriftFile : thriftFiles) {
            loader.addThriftFile(thriftFile);
        }

        for (Path dir : searchPath) {
            loader.addIncludePath(dir);
        }

        Schema schema;
        try {
            schema = loader.load();
        } catch (LoadFailedException e) {
            for (String report : e.errorReporter().formattedReports()) {
                System.out.println(report);
            }

            Runtime.getRuntime().exit(1);
            return;
        }

        ThriftyCodeGenerator gen = new ThriftyCodeGenerator(schema, fieldNamingPolicy);
        if (listTypeName != null) {
            gen = gen.withListType(listTypeName);
        }

        if (setTypeName != null) {
            gen = gen.withSetType(setTypeName);
        }

        if (mapTypeName != null) {
            gen = gen.withMapType(mapTypeName);
        }

        TypeProcessorService svc = TypeProcessorService.getInstance();
        TypeProcessor processor = svc.getJavaProcessor();
        if (processor != null) {
            gen = gen.usingTypeProcessor(processor);
        }

        gen.emitAndroidAnnotations(emitNullabilityAnnotations);
        gen.emitParcelable(emitParcelable);

        gen.generate(outputDirectory);
    }
}
