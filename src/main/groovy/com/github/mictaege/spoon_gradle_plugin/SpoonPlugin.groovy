package com.github.mictaege.spoon_gradle_plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.compile.JavaCompile

class SpoonPlugin implements Plugin<Project> {

    @Override
    void apply(final Project project) {

        ensureJava(project)

        project.extensions.create "spoon", SpoonExtension

        project.afterEvaluate({
            project.tasks.withType(JavaCompile) {
                def pathName = it.source.getAsPath()
                def name = it.getDestinationDir().name
                def compileClasspath = it.classpath
                if (!project.spoon.exclude.contains(name) && !it.source.empty) {
                    def spoonTask = project.task("spoon${name}".toLowerCase(), type: SpoonTask) {
                        buildOnlyOutdatedFiles = project.spoon.buildOnlyOutdatedFiles
                        srcFolders += pathName
                        outFolder = project.file("${project.buildDir}/generated-sources/spoon/${name}")
                        processors = project.spoon.processors
                        classpath = compileClasspath.filter {f -> f.exists()}
                        compliance = project.spoon.compliance
                    }
                    it.source = spoonTask.outFolder
                    it.dependsOn spoonTask
                }
            }
        })
    }

    private static void ensureJava(final Project project) {
        def hasJavaPlugin = project.plugins.hasPlugin JavaPlugin
        if (!hasJavaPlugin) {
            throw new IllegalStateException('The java plugin is required')
        }
    }

}