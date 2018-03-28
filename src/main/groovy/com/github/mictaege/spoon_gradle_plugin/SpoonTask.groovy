package com.github.mictaege.spoon_gradle_plugin

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.WorkResult
import spoon.Launcher

import java.util.function.Function

import static java.io.File.pathSeparator

class SpoonTask extends DefaultTask {

    private Logger log = Logging.getLogger("spoon")

    File srcDir
    File outDir
    @InputFiles
    FileTree javaSrcDir
    @OutputDirectory
    File javaOutDir
    Function<File, Boolean> fileFilter
    String[] processors = []
    FileCollection classpath
    int compliance

    @TaskAction
    void run() {
        try {
            prepareSpoon()
            runSpoon()
        } catch (final Exception e) {
            throw new TaskExecutionException(this, e)
        }
    }

    private WorkResult prepareSpoon() {
        project.copy {
            from(srcDir.path) {
                include '**/*.java'
                exclude { f ->
                    !f.isDirectory() && fileFilter.apply(f.file)
                }
            }
            into outDir.path
        }
    }

    private void runSpoon() {
        List<String> params = new LinkedList<>()

        addParam(params, '--input', srcDir.getAbsolutePath())
        addParam(params, '--output', outDir.getAbsolutePath())
        addParam(params, '--compliance', '' + compliance)
        if (processors.size() != 0) {
            addParam(params, '--processors', processors.join(pathSeparator))
        }
        if (!classpath.asPath.empty) {
            addParam(params, '--source-classpath', classpath.asPath)
        }
        addKey(params, '--noclasspath')
        addParam(params, '--level', "OFF")
        addParam(params, '--output-type', "classes")
        addKey(params, '--lines')
        addKey(params, '--disable-model-self-checks')

        addParam(params, '--generate-files', findTypesToSpoon())

        def launcher = new Launcher()
        String[] args = params.toArray(new String[params.size()])
        logEnv(args)
        launcher.setArgs(args)
        launcher.run()
    }

    private logEnv(String[] args) {
        log.debug "Spoon arguments:"
        for (final String arg : args) {
            log.debug "\t$arg"
        }
    }

    private static void addParam(params, key, value) {
        addKey(params, key)
        params.add(value)
    }

    private static void addKey(params, key) {
        params.add(key)
    }

    private String findTypesToSpoon() {
        FileTree srcTree = project.fileTree(srcDir)
        srcTree.exclude { f ->
            !f.isDirectory() && !f.file.name.endsWith(".java")
        }
        srcTree.exclude { f ->
            !f.isDirectory() && !fileFilter.apply(f.file)
        }
        List<String> typeList = new ArrayList<>()
        def solver = new JavaParserTypeSolver(srcDir)
        srcTree.visit { f ->
            if (!f.isDirectory()) {
                JavaParser.parse(f.file)
                        .findAll(TypeDeclaration.class)
                        .forEach { n ->
                    typeList.add(JavaParserFacade.get(solver).getTypeDeclaration(n).getQualifiedName())
                }
            }
        }
        typeList.join(":")
    }

}
