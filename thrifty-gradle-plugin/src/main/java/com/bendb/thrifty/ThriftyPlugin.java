package com.bendb.thrifty;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ThriftyPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        if (project.getPlugins().hasPlugin("com.android.application")) {
            applyAndroid(project);
        } else if (project.getPlugins().hasPlugin("com.android.library")) {
            applyAndroid(project);
        } else {
            throw new IllegalArgumentException("thrifty plugin requires the Android plugin to be configured");
        }
    }

    private void applyAndroid(Project project) {

    }
}
