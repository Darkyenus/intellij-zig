import org.slf4j.LoggerFactory
import wemi.BindingHolder
import wemi.Value
import wemi.WemiException
import wemi.dependency.JCenter
import wemi.dependency.Jitpack
import wemi.dependency.MavenCentral
import wemi.dependency.resolveDependencyArtifacts
import wemi.expiresWith
import wemi.generation.generateSources
import wemi.key
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

val grammarKitClasspath by key<List<Path>>("Classpath to use with Grammar-Kit operations generateLexer and generateParser")

val DefaultGrammarKitClasspath : Value<List<Path>> = {
	val classpath = resolveDependencyArtifacts(DefaultGrammarKitDependencies, DefaultGrammarKitRepositories, progressListener)
	if (classpath == null) {
		LOG.error("Failed to retrieve Grammar Kit dependencies ({} in {})", DefaultGrammarKitDependencies, DefaultGrammarKitRepositories)
		throw WemiException("Failed to retrieve Grammar Kit dependencies")
	}
	classpath
}

private val PackageRegex = Regex("^\\s*package\\s+((?:[a-zA-Z0-9_]+\\s*\\.?\\s*)+)\\s*;.*$")

fun BindingHolder.generateLexer(source:Path, skeleton:Path? = null) {
	generateSources("lexer-${source.name}") { genSourceRoot ->
		expiresWith(source)
		if (skeleton != null) {
			expiresWith(skeleton)
		}

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
		args.add("--quiet")
		args.add("--warn-unused")
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

		val procBuilder = prepareJavaProcess(
				Keys.javaHome.get().javaExecutable,
				Keys.projectRoot.get(),
				grammarKitClasspath.get(),
				"jflex.Main",
				emptyList(),
				args
		)

		LOG.debug("Running JFlex lexer generator")
		runForegroundProcess(procBuilder, separateOutputByNewlines = false)
	}
}

fun BindingHolder.generateParser(source:Path) {
	generateSources("parser-${source.name}") { genSourceRoot ->
		expiresWith(source)

		val classpath = grammarKitClasspath.get().toMutableList()

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

		LOG.debug("Running Grammar Kit parser generator")
		runForegroundProcess(prepareJavaProcess(
				Keys.javaHome.get().javaExecutable,
				Keys.projectRoot.get(),
				classpath,
				"org.intellij.grammar.Main",
				//listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"),
				emptyList(),
				listOf(genSourceRoot.absolutePath, source.absolutePath)
		), separateOutputByNewlines = false)
	}
}