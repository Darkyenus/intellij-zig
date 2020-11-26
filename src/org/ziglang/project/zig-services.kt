package org.ziglang.project

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.xmlb.annotations.Transient
import org.ziglang.ZigBundle
import org.ziglang.executeCommand
import java.nio.file.Files
import java.nio.file.Paths

/**
 * A [light service](https://jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_services.html#light-services),
 * which contains project settings.
 */
@State(name = "ZigProjectSettings", storages = [Storage(file = "zigConfig.xml", roamingType = RoamingType.DISABLED)])
class ZigProjectService : PersistentStateComponent<ZigSettings> {

	var settings = ZigSettings()
		set(value) {
			field = value
			if (value.exePath != null) {
				zigGlobalSettings = zigGlobalSettings.withZigPath(value.exePath)
			}
		}

	override fun getState(): ZigSettings = settings

	override fun loadState(state: ZigSettings) {
		this.settings = state
	}
}

/** Immutable project-level settings. */
data class ZigSettings (
		val exePath: String? = zigGlobalSettings.allKnownZigExePaths.firstOrNull()
) {

	@field:Transient
	private var exeInfoLazy:ExeInfo? = null

	val exeInfo:ExeInfo?
		get() {
			var result = exeInfoLazy
			if (result == null && exePath != null) {
				// TODO(jp): Get this out of zig env command
				result = ExeInfo(
						versionOf(exePath),
						Paths.get(exePath).parent.resolve("lib/std").toAbsolutePath().normalize().toString())
				exeInfoLazy = result
			}
			return result
		}

	data class ExeInfo(val version:String, val stdDir:String)
}

var Project.zigSettings: ZigSettings
	get() = ServiceManager.getService(this, ZigProjectService::class.java)?.settings ?: ZigSettings()
	set(value) {
		val service = ServiceManager.getService(this, ZigProjectService::class.java)
		if (service != null) {
			service.settings = value
		}
	}


/**
 * A [light service](https://jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_services.html#light-services),
 * which contains system-wide settings.
 */
@State(name = "ZigGlobalSettings", storages = [Storage(file = "zigGlobalConfig.xml", roamingType = RoamingType.DISABLED)])
class ZigGlobalSettingsService : PersistentStateComponent<ZigGlobalSettings> {

	var settings : ZigGlobalSettings = ZigGlobalSettings().validate()

	override fun getState(): ZigGlobalSettings = settings

	override fun loadState(state: ZigGlobalSettings) {
		this.settings = state.validate()
	}
}

/** Immutable system-wide settings. */
data class ZigGlobalSettings(val allKnownZigExePaths: List<String> = defaultZigPaths()) {

	/** Return a copy with a new [path] added. */
	fun withZigPath(path:String):ZigGlobalSettings {
		if (path in allKnownZigExePaths || !validateZigExe(path)) {
			return this
		}
		return copy(allKnownZigExePaths = allKnownZigExePaths + path)
	}

	/** Return copy of self with only valid data inside. */
	fun validate(): ZigGlobalSettings {
		return copy(allKnownZigExePaths = allKnownZigExePaths.filter(::validateZigExe))
	}
}

var zigGlobalSettings: ZigGlobalSettings
	get() = ServiceManager.getService(ZigGlobalSettingsService::class.java)?.settings ?: ZigGlobalSettings()
	set(value) {
		val service = ServiceManager.getService(ZigGlobalSettingsService::class.java)
		if (service != null) {
			service.settings = value
		}
	}


fun defaultZigPaths():List<String> {
	val path = PathEnvironmentVariableUtil.findInPath("zig")?.absolutePath
			?: when {
				SystemInfo.isWindows -> "C:/Program Files/" //TODO
				SystemInfo.isMac -> "/usr/local/bin/zig" // Homebrew path
				else -> "/usr/bin/zig"
			}
	return listOf(path)
}

fun versionOf(path: String) = executeCommand(arrayOf(path, "version"))
		.first
		.firstOrNull()
		?: ZigBundle.message("zig.version.unknown")

fun validateZigExe(exePath: String) = Files.exists(Paths.get(exePath))