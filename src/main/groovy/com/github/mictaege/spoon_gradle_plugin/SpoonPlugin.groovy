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
                def spoonExt = project.spoon.lazyExtensions == null ? project.spoon : project.spoon.lazyExtensions.get()
                def name = it.getDestinationDir().name
                def javaTaskSrcDir = it.getSource()
                def javaTaskOutDir = it.getDestinationDir()
                def compileSrcDir = project.file("${project.projectDir.absolutePath}/src/$name/java/")
                def spoonOutDir = project.file("${project.buildDir}/generated-sources/spoon/${name}")
                def compileClasspath = it.classpath
                if (!spoonExt.exclude.contains(name) && !it.source.empty) {

                    def spoonTask = project.task("spoon${name.capitalize()}", type: SpoonTask) {
                        srcDir = compileSrcDir
                        outDir = spoonOutDir
                        javaSrcDir = javaTaskSrcDir
                        javaOutDir = javaTaskOutDir
                        fileFilter = spoonExt.fileFilter
                        processors = spoonExt.processors
                        classpath = compileClasspath
                        compliance = spoonExt.compliance
                    }

                    it.source = spoonOutDir
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