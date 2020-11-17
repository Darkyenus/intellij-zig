import org.slf4j.LoggerFactory
import wemi.BindingHolder
import wemi.WemiException
import wemi.dependency.JCenter
import wemi.dependency.Jitpack
import wemi.dependency.MavenCentral
import wemi.dependency.resolveDependencyArtifacts
import wemi.generation.generateSources
import wemi.run.prepareJavaProcess
import wemi.run.runForegroundProcess
import wemi.util.absolutePath
import wemi.util.name
import wemiplugin.intellij.IntelliJ

private val LOG = LoggerFactory.getLogger("grammarKit")

val DefaultGrammarKitDependencies = listOf(
		dependency("org.jetbrains.intellij.deps.jflex", "jflex", "1.7.0-1", exclusions = listOf(
				DependencyExclusion(group = "org.jetbrains.plugins", name = "idea"),
				DependencyExclusion(group = "org.jetbrains.plugins", name = "ant")
		)),
		dependency("com.github.JetBrains", "Grammar-Kit", "2020.1" /* 2020.3 is broken */, exclusions = listOf(
				DependencyExclusion(group = "org.jetbrains.plugins", name = "idea"),
				DependencyExclusion(group = "org.jetbrains.plugins", name = "ant")
		))
)

val DefaultGrammarKitRepositories = listOf(
		MavenCentral,
		Jitpack,
		JCenter,
		Repository("intellij-third-party-dependencies", "https://jetbrains.bintray.com/intellij-third-party-dependencies")
)

private val PackageRegex = Regex("^\\s*package\\s+((?:[a-zA-Z0-9_]+\\s*\\.?\\s*)+)\\s*;.*$")

fun BindingHolder.generateLexer(source:Path, skeleton:Path? = null,
                                jflexDependency:List<Dependency> = DefaultGrammarKitDependencies,
                                jflexRepositories:List<Repository> = DefaultGrammarKitRepositories) {

	generateSources("lexer-${source.name}") { genSourceRoot ->
		// Figure out, to which package does the file belong to.
		var packageName = ""
		for (line in Files.lines(source)) {
			val match = PackageRegex.matchEntire(line)
			if (match == null) {
				val trimmedLine = line.trim()
				if (trimmedLine.startsWith("import") || trimmedLine.startsWith("%%")) {
					// There will be no package name after these
					break
				}
				continue
			}
			packageName = match.groupValues[1].replace(Regex("\\s+"), "")
			LOG.debug("Detected package name for {}: {}", source, packageName)
			break
		}

		val args = ArrayList<String>()
		if (skeleton != null) {
			args.add("--skel")
			args.add(skeleton.absolutePath)
		}
		args.add("-d")
		if (packageName.isEmpty()) {
			args.add(genSourceRoot.absolutePath)
		} else {
			val packagePath = genSourceRoot / packageName.replace('.', '/')
			Files.createDirectories(packagePath)
			args.add(packagePath.absolutePath)
		}
		args.add(source.absolutePath)

		val classpath = resolveDependencyArtifacts(jflexDependency, jflexRepositories, progressListener)
		if (classpath == null) {
			LOG.error("Failed to retrieve JFlex dependencies ({} in {})", jflexDependency, jflexRepositories)
			throw WemiException("Failed to retrieve JFlex dependencies")
		}
		LOG.debug("Lexer classpath: {}", classpath)

		val procBuilder = prepareJavaProcess(
				Keys.javaHome.get().javaExecutable,
				Keys.projectRoot.get(),
				classpath,
				"jflex.Main",
				emptyList(),
				args
		)
		runForegroundProcess(procBuilder)
	}
}

fun BindingHolder.generateParser(
		source:Path,
		grammarKitDependency:List<Dependency> = DefaultGrammarKitDependencies,
		grammarKitRepositories:List<Repository> = DefaultGrammarKitRepositories
) {

	generateSources("parser-${source.name}") { genSourceRoot ->
		val explicitClasspath = resolveDependencyArtifacts(grammarKitDependency, grammarKitRepositories, progressListener)
		if (explicitClasspath == null) {
			LOG.error("Failed to retrieve Grammar Kit dependencies ({} in {})", grammarKitDependency, grammarKitRepositories)
			throw WemiException("Failed to retrieve Grammar Kit dependencies")
		}
		val classpath = ArrayList<Path>(explicitClasspath)

		/*
		def requiredLibs = [
					"jdom", "trove4j", "junit", "guava", "asm-all", "automaton", "platform-api", "platform-impl",
					"util", "annotations", "picocontainer", "extensions", "idea", "openapi", "Grammar-Kit",
					"platform-util-ui", "platform-concurrency", "intellij-deps-fastutil",
					// CLion unlike IDEA contains `MockProjectEx` in `testFramework.jar` instead of `idea.jar`
					// so this jar should be in `requiredLibs` list to avoid `NoClassDefFoundError` exception
					// while parser generation with CLion distribution
					"testFramework"
			]
			classpath project.configurations.compileOnly.files.findAll({
				for(lib in requiredLibs){
					if(it.name.equalsIgnoreCase("${lib}.jar") || it.name.startsWith("${lib}-")){
						return true;
					}
				}
				return false;
			})*/

		classpath.addAll(IntelliJ.resolvedIntellijIdeDependency.get().jarFiles)

		LOG.debug("Parser classpath: {}", classpath)

		runForegroundProcess(prepareJavaProcess(
				Keys.javaHome.get().javaExecutable,
				Keys.projectRoot.get(),
				classpath,
				"org.intellij.grammar.Main",
				emptyList(),
				listOf(genSourceRoot.absolutePath, source.absolutePath)
		))
	}
}