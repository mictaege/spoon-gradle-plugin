package com.github.mictaege.spoon_gradle_plugin

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileType
import org.gradle.api.tasks.*
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import spoon.Launcher

import java.lang.reflect.Method
import java.util.function.Function

import static spoon.OutputType.CLASSES
import static spoon.reflect.visitor.PrettyPrinterCreator.createPrettyPrinter

abstract class SpoonTask extends DefaultTask {

	@Incremental
	@InputDirectory
	abstract DirectoryProperty getSrcDir()
	@OutputDirectory
	File outDir
	// should be an @Input, but needs to be serializable
	@Internal
	Function<File, Boolean> fileFilter
	@Input
	String[] processors = []
	@InputFiles
	FileCollection classpath
	@Input
	int compliance

	@Internal
	int unspoonedFilesCopied
	@Internal
	int unspoonedFilesTargetDeleted
	@Internal
	int spoonedFilesTargetDeleted
	@Internal
	int spoonedFilesSentToSpoon

	@TaskAction
	void run(InputChanges inputChanges) {
		try {
			if (!inputChanges.incremental) {
				outDir.deleteDir() //remove all files in order to avoid orphan files from moved/deleted sources
			}

			prepareSpoon(inputChanges)
			runSpoon(inputChanges)
			println("""SpoonTask Statistics:
    incremental run:          ${inputChanges.incremental}
    files without spooning:   $unspoonedFilesCopied (just copied)
    unspooned source removed: $unspoonedFilesTargetDeleted (target files without spooning deleted)
    spooned files:            $spoonedFilesSentToSpoon
    spooned source removed:   $spoonedFilesTargetDeleted (target files with spooning deleted)"""
			)
		} catch (final Exception e) {
			throw new TaskExecutionException(this, e)
		}
	}

	private void prepareSpoon(InputChanges inputChanges) {
		//copy all "unspooned" files into outDir
		inputChanges.getFileChanges(getSrcDir()).each { change ->
			def file = change.file

			if (change.fileType == FileType.DIRECTORY) {
				return
			}
			if (fileFilter.apply(file)) {
				return
			}
			if (change.file.name.endsWith(".java")) {
				def srcDirAsFile = getSrcDir().get().asFile
				def targetFile = new File(outDir.absolutePath + change.normalizedPath.substring(srcDirAsFile.absolutePath.length()))
				if (change.changeType == ChangeType.REMOVED) {
					targetFile.delete()
					unspoonedFilesTargetDeleted++
				} else {
					targetFile.parentFile.mkdirs()
					com.google.common.io.Files.copy(file, targetFile)
					unspoonedFilesCopied++
				}
			}
		}
	}

	private void runSpoon(InputChanges inputChanges) {
		def launcher = new Launcher()
		launcher.modelBuilder.addInputSource(getSrcDir().get().asFile)
		launcher.environment.setSourceOutputDirectory(outDir)
		launcher.setOutputFilter(findTypesToSpoon(inputChanges))
		launcher.environment.setInputClassLoader(extendedClassloader())
		launcher.environment.setComplianceLevel(compliance)
		launcher.environment.setNoClasspath(true)
		launcher.environment.setLevel("OFF")
		launcher.environment.setOutputType(CLASSES)
		launcher.environment.setPreserveLineNumbers(true)
		launcher.environment.setCommentEnabled(false)
		launcher.environment.disableConsistencyChecks()
		launcher.environment.setShouldCompile(false)
		processors.each { String procType ->
			launcher.addProcessor(procType)
		}
		launcher.environment.setPrettyPrinterCreator {
			createPrettyPrinter(launcher.environment)
		}
		launcher.run()
	}

	private ClassLoader extendedClassloader() {
		URLClassLoader sysloader = (URLClassLoader) getClass().getClassLoader()
		Class sysclass = URLClassLoader.class
		Method method = sysclass.getDeclaredMethod("addURL", URL.class)
		method.setAccessible(true)
		classpath.forEach { f ->
			if (f.exists()) {
				try {
					method.invoke(sysloader, f.toURI().toURL())
				} catch (Throwable t) {
					throw new IOException("Error, could not add URL to system classloader", t)
				}
			}
		}
		return sysloader
	}

	private String[] findTypesToSpoon(InputChanges inputChanges) {
		List<String> typeList = new ArrayList<>()
		inputChanges.getFileChanges(getSrcDir()).each { change ->
			def file = change.file
			if (change.fileType == FileType.DIRECTORY) {
				return
			}
			if (!file.name.endsWith(".java")) {
				return
			}
			if (!fileFilter.apply(file)) {
				return
			}
			def srcDirFile = getSrcDir().get().asFile
			if (change.changeType == ChangeType.REMOVED) {
				new File(outDir.absolutePath + change.normalizedPath.substring(srcDirFile.absolutePath.length())).delete()
				spoonedFilesTargetDeleted++
			} else {
				def solver = new JavaParserTypeSolver(srcDirFile)
				if (!file.isDirectory()) {
					def parser = new JavaParser()
					parser.parse(file)
							.ifSuccessful({
								it.findAll(TypeDeclaration.class)
										.forEach { n ->
											typeList.add(JavaParserFacade.get(solver).getTypeDeclaration(n).getQualifiedName())
										}
							})
					spoonedFilesSentToSpoon++
				}
			}
		}
		return typeList.toArray(new String[typeList.size()])
	}
}
