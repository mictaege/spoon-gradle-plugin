package com.github.mictaege.spoon_gradle_plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin

class SpoonPlugin implements Plugin<Project> {

    @Override
    void apply(final Project project) {

        ensureJava(project)

        project.extensions.create "spoon", SpoonExtension

        project.afterEvaluate({
            def compileJava = project.getTasksByName("compileJava", true)
                    .stream()
                    .findFirst()
                    .orElseThrow({new IllegalStateException("Required task 'compileJava' is missing")})

            def spoonMainTask = project.task('spoonMain', type: SpoonTask) {
                buildOnlyOutdatedFiles = project.spoon.buildOnlyOutdatedFiles
                srcFolders = mainInputs(project)
                outFolder = project.file("${project.buildDir}/generated-sources/spoon/main")
                processors = project.spoon.processors
                classpath = compileJava.classpath.filter {f -> f.exists()}
                compliance = project.spoon.compliance
            }

            compileJava.source = spoonMainTask.outFolder
            compileJava.dependsOn spoonMainTask

            def compileTestJava = project.getTasksByName("compileTestJava", true)
                    .stream()
                    .findFirst()
                    .orElseThrow({new IllegalStateException("Required task 'compileTestJava' is missing")})

            def spoonTestTask = project.task('spoonTest', type: SpoonTask) {
                buildOnlyOutdatedFiles = project.spoon.buildOnlyOutdatedFiles
                srcFolders = testInputs(project)
                outFolder = project.file("${project.buildDir}/generated-sources/spoon/test")
                processors = project.spoon.processors
                classpath = compileTestJava.classpath.filter {f -> f.exists()}
                compliance = project.spoon.compliance
            }

            compileTestJava.source = spoonTestTask.outFolder
            compileTestJava.dependsOn spoonTestTask
        })
    }

    private static void ensureJava(final Project project) {
        def hasJavaPlugin = project.plugins.hasPlugin JavaPlugin
        if (!hasJavaPlugin) {
            throw new IllegalStateException('The java plugin is required')
        }
    }

    private static String[] mainInputs(final Project project) {
        def inputs = []
        project.sourceSets.main.java.srcDirs.each() {
            if (project.file(it).exists()) {
                inputs.add(it.getAbsolutePath())
            }
        }
        return inputs
    }

    private static String[] testInputs(final Project project) {
        def inputs = []
        project.sourceSets.test.java.srcDirs.each() {
            if (project.file(it).exists()) {
                inputs.add(it.getAbsolutePath())
            }
        }
        return inputs
    }

}