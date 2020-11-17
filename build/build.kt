@file:Suppress("unused")
@file:BuildDependencyPlugin("wemi-plugin-intellij")
import wemi.compile.KotlinCompilerVersion
import wemi.util.FileSet
import wemiplugin.intellij.IntelliJ
import wemiplugin.intellij.IntelliJIDE
import wemiplugin.intellij.IntelliJPluginLayer

val zigPlugin by project(Archetypes.JavaKotlinProject, IntelliJPluginLayer) {
	projectGroup set { "org.ziglang" }
	projectName set { "intellij-zig" }
	projectVersion set { "0.1.3" }

	sources set { FileSet(projectRoot.get() / "src") }
	resources set { FileSet(projectRoot.get() / "res") }
	testSources set { FileSet(projectRoot.get() / "test") }
	testResources set { FileSet(projectRoot.get() / "testData") }

	libraryDependencies add { kotlinDependency("stdlib-jdk8") }
	libraryDependencies add { kotlinDependency("test-junit").copy(scope = ScopeTest) }
	libraryDependencies add { dependency("junit", "junit", "4.12", scope = ScopeTest) }
	// com.github.JetBrains/Grammar-Kit/2019.1
	// org.jetbrains.intellij.deps.jflex/jflex/1.7.0-1

	libraryDependencies add { dependency("org.eclipse.mylyn.github", "org.eclipse.egit.github.core", "2.1.5", exclusions = listOf(
			DependencyExclusion(group = " com.google.code.gson", name = "gson")
	)) }

	generateLexer(path("grammar/zig-lexer.flex"))
	generateParser(path("grammar/zig-grammar.bnf"))

	IntelliJ.intellijIdeDependency set { IntelliJIDE.External(version = "201.8743.12") }
	IntelliJ.intelliJPluginXmlFiles add { LocatedPath(Keys.projectRoot.get() / "src/main/plugin.xml") }
	Keys.automaticKotlinStdlib set { false }
	Keys.kotlinVersion set { KotlinCompilerVersion.Version1_3_72 }
}

/*
tasks.withType<PatchPluginXmlTask> {
	changeNotes(file("res/META-INF/change-notes.html").readText())
	pluginDescription(file("res/META-INF/description.html").readText())
	version(pluginVersion)
	pluginId(packageName)
}
*/