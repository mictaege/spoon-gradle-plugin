package com.github.mictaege.spoon_gradle_plugin

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileType
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import spoon.Launcher

import java.lang.reflect.Method
import java.util.function.Function

import static java.io.File.pathSeparator

abstract class SpoonTask extends DefaultTask {

	private Logger log = Logging.getLogger("spoon")

	@Incremental
	@InputDirectory
	abstract DirectoryProperty getSrcDir()
	@OutputDirectory
	File outDir
	// should be an @Input, but needs to be serializable
	Function<File, Boolean> fileFilter
	@Input
	String[] processors = []
	@InputFiles
	FileCollection classpath
	@Input
	int compliance

	int unspoonedFilesCopied
	int unspoonedFilesTargetDeleted
	int spoonedFilesTargetDeleted
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
		//TODO measure performance in comparison to older implementation - use old impl if !inputChanges.incremental?
		//old impl:
		//
		//project.copy {
		//            from(srcDir.path) {
		//                include '**/*.java'
		//                exclude { f ->
		//                    !f.isDirectory() && fileFilter.apply(f.file)
		//                }
		//            }
		//            into outDir.path
		//        }
		//
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
		List<String> params = new LinkedList<>()

		addParam(params, '--input', getSrcDir().get().asFile.absolutePath)
		addParam(params, '--output', outDir.absolutePath)
		addParam(params, '--compliance', '' + compliance)
		if (processors.size() != 0) {
			addParam(params, '--processors', processors.join(pathSeparator))
		}
		addParam(params, '--cpmode', 'noclasspath')
		addParam(params, '--level', "OFF")
		addParam(params, '--output-type', "classes")
		addKey(params, '--lines')
		addKey(params, '--disable-model-self-checks')
		addKey(params, '--disable-comments')

		def typesToSpoon = findTypesToSpoon(inputChanges)
		addParam(params, '--generate-files', typesToSpoon)

		def launcher = new Launcher()
		String[] args = params.toArray(new String[params.size()])
		logEnv(args)
		launcher.setArgs(args)
		launcher.environment.setInputClassLoader(extendedClassloader())
		launcher.run()
	}

	private ClassLoader extendedClassloader() {
		URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader()
		Class sysclass = URLClassLoader.class
		classpath.forEach { f ->
			if (f.exists()) {
				try {
					Method method = sysclass.getDeclaredMethod("addURL", URL.class)
					method.setAccessible(true)
					method.invoke(sysloader, f.toURI().toURL())
				} catch (Throwable t) {
					throw new IOException("Error, could not add URL to system classloader", t)
				}
			}
		}
		return sysloader
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

	private String findTypesToSpoon(InputChanges inputChanges) {
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
					JavaParser.parse(file)
							.findAll(TypeDeclaration.class)
							.forEach { n ->
								typeList.add(JavaParserFacade.get(solver).getTypeDeclaration(n).getQualifiedName())
							}
					spoonedFilesSentToSpoon++
				}
			}
		}
		return typeList.join(":")
	}

//	public void debugChange(FileChange change) {
//		log.debug("""change: 
//File:       ${change.file}
//FileType:   ${change.fileType}
//ChangeType: ${change.changeType}
//""")
//	}
}
