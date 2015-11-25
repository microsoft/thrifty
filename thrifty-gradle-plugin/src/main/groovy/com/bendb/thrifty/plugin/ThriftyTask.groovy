package com.bendb.thrifty.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory

class ThriftyTask extends DefaultTask {

    @OutputDirectory
    def File outputDir

    def List<ThriftyExtension> configurations = []


}
