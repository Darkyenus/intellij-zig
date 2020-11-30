package org.ziglang.project.ui

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.platform.ProjectGeneratorPeer
import com.intellij.ui.components.labels.LinkLabel
import org.ziglang.ZigBundle
import org.ziglang.initExeComboBox
import org.ziglang.project.ZigSettings
import org.ziglang.project.validateZigExe
import org.ziglang.project.versionOf
import org.ziglang.project.zigSettings
import java.awt.event.ItemListener

class ZigProjectGeneratorPeerImpl : ZigProjectGeneratorPeer() {
	private var settings = ZigSettings()
	private val listeners = ArrayList<ProjectGeneratorPeer.SettingsListener>()

	init {
		initExeComboBox(executablePath) {
			executablePath.comboBox.selectedItem = settings.exePath
			version.text = versionOf(it.comboBox.selectedItem as? String ?: return@initExeComboBox)
		}
		installPathField.addBrowseFolderListener(TextBrowseFolderListener(
				FileChooserDescriptorFactory.createSingleFolderDescriptor()))
		zigWebsite.asLink()
		iceZigRelease.asLink()
		setupLater.addChangeListener {
			executablePath.isEnabled = !setupLater.isSelected
		}
		val listener = ItemListener {
			val selected = executablePath.comboBox.selectedItem as? String ?: return@ItemListener
			version.text = versionOf(selected)
		}
		listener.itemStateChanged(null)
		executablePath.comboBox.addItemListener(listener)
	}

	override fun addSettingsListener(listener: ProjectGeneratorPeer.SettingsListener) {
		listeners += listener
	}

	override fun getSettings() = settings
	override fun getComponent() = mainPanel
	override fun buildUI(settingsStep: SettingsStep) = settingsStep.addExpertPanel(component)

	override fun validate(): ValidationInfo? {
		if (setupLater.isSelected) return null
		val selected = executablePath.comboBox.selectedItem as? String
		if (selected != null) {
			if (!validateZigExe(selected)) {
				// usefulText.isVisible = true
				return ValidationInfo(ZigBundle.message("zig.project.invalid-exe"))
			}
			listeners.forEach { it.stateChanged(true) }
			settings = ZigSettings(selected)
		}
		return null
	}

	override fun isBackgroundJobRunning() = false
}

class ZigConfigurableImpl(private val project: Project) : ZigConfigurable() {

	init {
		initExeComboBox(executablePath) {
			executablePath.comboBox.selectedItem = project.zigSettings.exePath
			version.text = versionOf(it.comboBox.selectedItem as? String ?: return@initExeComboBox)
		}
		zigWebsite.asLink()
		iceZigRelease.asLink()
		installPathField.text = ""
		version.text = project.zigSettings.exeInfo?.version ?: "Invalid"
		installPathField.addBrowseFolderListener(TextBrowseFolderListener(
				FileChooserDescriptorFactory.createSingleFolderDescriptor()))
	}

	override fun createComponent() = mainPanel
	override fun getDisplayName() = ZigBundle.message("zig.name")
	override fun isModified() = executablePath.comboBox.selectedItem != project.zigSettings.exePath

	@Throws(ConfigurationException::class)
	override fun apply() {
		val selected = executablePath.comboBox.selectedItem as? String
		if (selected != null) {
			if (!validateZigExe(selected))
				throw ConfigurationException(ZigBundle.message("zig.project.invalid-exe"))
			project.zigSettings = ZigSettings(selected)
		}
	}
}

private fun LinkLabel<Any>.asLink() {
	setListener({ _, _ ->
		BrowserLauncher.instance.browse(text)
	}, null)
}

