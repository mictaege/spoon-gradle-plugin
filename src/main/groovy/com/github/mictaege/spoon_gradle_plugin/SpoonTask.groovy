package com.github.mictaege.spoon_gradle_plugin

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileType
import org.gradle.api.tasks.*
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import spoon.Launcher

import java.util.function.Function

import static spoon.OutputType.CLASSES
import static spoon.reflect.visitor.PrettyPrinterCreator.createPrettyPrinter

@CacheableTask
abstract class SpoonTask extends DefaultTask {

	@Incremental
	@InputDirectory
	@PathSensitive(PathSensitivity.RELATIVE)
	abstract DirectoryProperty getSrcDir()
	@OutputDirectory
	File outDir
	// should be an @Input, but needs to be serializable. It is code of the plugin which configures the task and as
	// such part of the build script classpath: changing it changes the implementation of the task, which is an input.
	@Internal
	Function<File, Boolean> fileFilter
	@Input
	String[] processors = []
	// the processors see the classpath through a class loader only, e.g. to resolve constants used in annotations
	@CompileClasspath
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
		} catch (final GradleException e) {
			throw e
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
				def targetFile = targetFileOf(file)
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

	// Not derived from the normalized path of the change: that one depends on the path sensitivity of srcDir.
	// Not private: it is called from closures, which are unable to see private methods of the decorated task class.
	protected File targetFileOf(File srcFile) {
		def relativePath = getSrcDir().get().asFile.toPath().relativize(srcFile.toPath())
		return outDir.toPath().resolve(relativePath).toFile()
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
		List<URL> cpUrls = new ArrayList<>()
		classpath.forEach { f ->
			if (f.exists()) {
				cpUrls.add(f.toURI().toURL())
			}
		}
		return URLClassLoader.newInstance(cpUrls.toArray(new URL[cpUrls.size()]), getClass().getClassLoader())
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
				targetFileOf(file).delete()
				spoonedFilesTargetDeleted++
			} else {
				def solver = new JavaParserTypeSolver(srcDirFile)
				if (!file.isDirectory()) {
					def parser = new JavaParser()
					parser.parserConfiguration.setLanguageLevel(ParserConfiguration.LanguageLevel.RAW)

					def result = parser.parse(file)
					if (!result.isSuccessful()) {
						// the types of the file are unknown, so they would be missing in the output (or a stale copy
						// would be kept) - and the incomplete output would end up in the build cache
						def problems = result.problems.collect { it.verboseMessage }.join(System.lineSeparator())
						throw new GradleException("Parsing Java file ${file.toPath()} failed!${System.lineSeparator()}${problems}")
					}
					result.ifSuccessful({
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
