package com.github.mictaege.spoon_gradle_plugin

import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.util.function.Function

class SpoonCopyTask extends DefaultTask {

    private Logger log = Logging.getLogger("spoon")

    @InputFiles
    File srcDir
    @OutputDirectory
    File outDir
    Function<File, Boolean> fileFilter


    @TaskAction
    void run() {
        project.copy {
            from (srcDir.path) {
                include '**/*.java'
                exclude {f ->
                    !f.isDirectory() && fileFilter.apply(f.file)
                }
            }
            into outDir.path
        }
    }

}