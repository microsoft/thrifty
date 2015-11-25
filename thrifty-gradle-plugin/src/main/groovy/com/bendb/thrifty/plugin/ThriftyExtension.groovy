package com.bendb.thrifty.plugin

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection

public class ThriftyExtension {
    def ConfigurableFileCollection thriftFiles
    def List<File> searchPath
    def String listClass
    def String setClass
    def String mapClass

    public ThriftyExtension(Project project, String dirName) {
        thriftFiles = project.files()
        searchPath = []
        listClass = ArrayList.class.getCanonicalName()
        setClass = HashSet.class.getCanonicalName()
        mapClass = HashMap.class.getCanonicalName()
    }

    /**
     * Adds a Thrift file to the set of files to be compiled.
     *
     * <p>The {@param file} parameter is resolved as in {@link Project#files(Object...)}.
     *
     * @param file the .thrift file to be added.
     */
    public void thrift(Object file) {
        thriftFiles.from(file)
    }

    /**
     * Adds a directory to the search path, to be used in searching for included
     * Thrift files.
     *
     * @param directory
     */
    public void pathDir(File directory) {
        searchPath.add(directory)
    }

    /**
     * Set the concrete {@link java.util.List} implementation to use in generated code.
     *
     * <p>If unspecified, this value defaults to {@link java.util.ArrayList}.
     *
     * @param listClass The fully-qualified name of a concrete list implementation.
     */
    public void listClass(String listClass) {
        this.listClass = listClass
    }

    /**
     * Set the concrete {@link java.util.Set} implementation to use in generated code.
     *
     * <p>If unspecified, this value defaults to {@link java.util.HashSet}.
     *
     * @param setClass the fully-qualified name of a concrete set implementation.
     */
    public void setClass(String setClass) {
        this.setClass = setClass
    }

    /**
     * Set the concrete {@link java.util.Map} implementation to use in generated code.
     *
     * <p>If unspecified, this value defaults to {@link java.util.HashMap}.
     *
     * @param mapClass the fully-qualified name of a concrete map implementation.
     */
    public void mapClass(String mapClass) {
        this.mapClass = mapClass
    }
}
