@file:Suppress("unused")
@file:BuildDependencyPlugin("wemi-plugin-intellij")
import wemi.compile.KotlinCompilerVersion
import wemi.util.FileSet
import wemi.util.name
import wemiplugin.intellij.IntelliJ
import wemiplugin.intellij.IntelliJIDE
import wemiplugin.intellij.IntelliJPluginLayer
import wemiplugin.intellij.utils.Patch
import java.nio.file.Path

val zigPlugin by project(Archetypes.JavaKotlinProject, IntelliJPluginLayer) {
	projectGroup set { "org.ziglang" }
	projectName set { "intellij-zig" }
	projectVersion set { "0.1.3" }

	extend(Archetypes.JavaKotlinProject) {
		sources set { FileSet(projectRoot.get() / "src") }
		resources set { FileSet(projectRoot.get() / "res") }
		testSources set { FileSet(projectRoot.get() / "test") }
		testResources set { FileSet(projectRoot.get() / "testData") }
	}

	libraryDependencies add { kotlinDependency("stdlib-jdk8") }
	libraryDependencies add { kotlinDependency("test-junit").copy(scope = ScopeTest) }
	libraryDependencies add { dependency("junit", "junit", "4.12", scope = ScopeTest) }
	// com.github.JetBrains/Grammar-Kit/2019.1
	// org.jetbrains.intellij.deps.jflex/jflex/1.7.0-1

	libraryDependencies add { dependency("org.eclipse.mylyn.github", "org.eclipse.egit.github.core", "2.1.5", exclusions = listOf(
			DependencyExclusion(group = " com.google.code.gson", name = "gson")
	)) }

	grammarKitClasspath set DefaultGrammarKitClasspath
	/*grammarKitClasspath modify {
		val cp = it.filterNot { it.name.startsWith("Grammar-Kit-") }.toMutableList()
		cp.add(path("Grammar-Kit-2020.3.1-SNAPSHOT.jar"))
		cp
	}*/
	generateLexer(path("grammar/zig-lexer.flex"))
	generateParser(path("grammar/zig-grammar.bnf"))

	IntelliJ.intellijIdeDependency set { IntelliJIDE.External(version = "201.8743.12") }
	IntelliJ.intelliJPluginXmlFiles add { LocatedPath(path("build/plugin.xml")) }
	IntelliJ.intelliJPluginXmlPatches add { pluginXmlDescriptionPatch(path("build/description.html")) }
	IntelliJ.intelliJPluginXmlPatches add { pluginXmlChangeNotesPatch(path("build/change-notes.html")) }

	Keys.automaticKotlinStdlib set { false }
	Keys.kotlinVersion set { KotlinCompilerVersion.Version1_3_72 }
}

// TODO(jp): Included in Wemi 0.16
fun pluginXmlDescriptionPatch(description: Path):Patch = Patch("description", content = String(Files.readAllBytes(description), Charsets.UTF_8))
fun pluginXmlChangeNotesPatch(changeNotes: Path):Patch = Patch("change-notes", content = String(Files.readAllBytes(changeNotes), Charsets.UTF_8))


/*
tasks.withType<PatchPluginXmlTask> {
	changeNotes(file("res/META-INF/change-notes.html").readText())
	pluginDescription(file("res/META-INF/description.html").readText())
	version(pluginVersion)
	pluginId(packageName)
}
*/