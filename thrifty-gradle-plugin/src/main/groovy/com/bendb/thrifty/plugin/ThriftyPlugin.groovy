package com.bendb.thrifty.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile

class ThriftyPlugin implements Plugin<Project> {
    Logger log

    @Override
    public void apply(Project project) {
        def hasAppPlugin = project.plugins.hasPlugin AppPlugin
        def hasLibraryPlugin = project.plugins.hasPlugin LibraryPlugin
        def hasJavaPlugin = project.plugins.hasPlugin JavaPlugin

        log = project.logger

        if (hasAppPlugin) {
            def plugin = project.getPlugins().getPlugin(AppPlugin)
            def ext = (AppExtension) plugin.extension
            applyAndroid(project, ext.applicationVariants);
        } else if (hasLibraryPlugin) {
            def plugin = project.plugins.getPlugin(LibraryPlugin)
            def ext = (LibraryExtension) plugin.extension
            applyAndroid(project, ext.libraryVariants);
        } else if (hasJavaPlugin) {
            applyJava(project)
        } else {
            throw new IllegalArgumentException("thrifty plugin requires the Android or Java plugins to be configured");
        }
    }

    private def applyAndroid(Project project, DomainObjectCollection<BaseVariant> variants) {
        project.android.sourceSets.all { SourceSet ss ->
            log.info("ss: ${ss}, name: ${ss.name}")
            defineExtension(ss, project)
        }

        variants.each { variant ->
            def task = createTask(project, variant.name, variant.dirName, variant.sourceSets)
            variant.registerJavaGeneratingTask(task, task.outputDir)
        }
    }

    private def applyJava(Project project) {
        project.sourceSets.each { SourceSet ss ->
            log.info("applyJava: ss=${ss}, ss.name=${ss.name}")
            defineExtension(ss, project)

            def setName = (String) ss.name
            def taskName = "main".equals(setName) ? "" : setName
            def thriftyTask = createTask(project, taskName, setName, [ss])

            def classesTaskName = taskName.isEmpty() ? "classes" : "${taskName}Classes"
            def classesTask = project.tasks.findByName(classesTaskName)
            classesTask.mustRunAfter thriftyTask

            def javaTask = (JavaCompile) project.tasks.findByName("compile${taskName.capitalize()}Java")
            javaTask.source thriftyTask.outputDir
            javaTask.dependsOn thriftyTask
        }
    }

    private static ThriftyTask createTask(Project project, String name, String dir, Collection<?> sourceSets) {
        def configurations = sourceSets.collect { ss ->
            (ThriftyExtension) ss.extensions['thrifty']
        }

        def task = project.tasks.create(
                "generate${name.capitalize()}ThriftSources",
                ThriftyTask)
        task.configurations = configurations
        task.outputDir = project.file("${project.buildDir}/generated/source/thrifty/${dir}")

        return task
    }

    private static defineExtension(SourceSet ss, Project project) {
        ss.extensions.create('thrifty', ThriftyExtension, project, ss.name)
    }
}
