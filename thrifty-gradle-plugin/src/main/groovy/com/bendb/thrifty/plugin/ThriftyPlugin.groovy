package com.bendb.thrifty.plugin

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class ThriftyPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        if (project.getPlugins().hasPlugin("com.android.application")) {
            applyAndroid(project, (DomainObjectCollection<BaseVariant>) project.android.applicationVariants);
        } else if (project.getPlugins().hasPlugin("com.android.library")) {
            applyAndroid(project, (DomainObjectCollection<BaseVariant>) project.android.libraryVariants);
        } else if (project.plugins.hasPlugin("org.gradle.java")) {
            applyJava(project)
        } else {
            throw new IllegalArgumentException("thrifty plugin requires the Android or Java plugins to be configured");
        }
    }

    private static def applyAndroid(Project project, DomainObjectCollection<BaseVariant> variants) {
        project.android.sourceSets.all { ss ->
            ss.extensions.create('thrifty', ThriftyExtension, project, ss.name)
        }

        variants.each { variant ->
            def name = variant.name.capitalize()
            def slug = "generateThrift${name}Sources"
            def task = createTask(project, variant.name, variant.dirName, variant.sourceSets)
            variant.registerJavaGeneratingTask(task, task.outputDir)
        }
    }

    private static def applyJava(Project project) {
        project.sourceSets.each { ss ->
            ss.extensions.create('thrifty', ThriftyExtension, project, ss.name)

            def setName = (String) ss.name
            def taskName = "main".equals(setName) ? "" : setName
            def thriftyTask = createTask(project, taskName, setName, [ss])

            def classesTaskName = taskName.isEmpty() ? "classes" : "${taskName}Classes"
            def classesTask = project.tasks.findByName(classesTaskName)
            classesTask.mustRunAfter thriftyTask

            def javaTask = (JavaCompile) project.tasks.findByName("compile${taskName.capitalize()}Java")
            javaTask.source += project.fileTree(thriftyTask.outputDir)
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
}
