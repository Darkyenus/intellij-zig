package org.ziglang

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ComboboxWithBrowseButton
import org.ziglang.project.zigGlobalSettings
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.JComboBox

fun String.trimPath() = trimEnd('/', '!')

/**
 * @param command the cmd string
 * @param input stdin
 * @param timeLimit maximum execution time
 */
fun executeCommand(
		command: Array<String>, input: String? = null, timeLimit: Long = 1200L): Pair<List<String>, List<String>> {
	var processRef: Process? = null
	var output: List<String> = emptyList()
	var outputErr: List<String> = emptyList()
	val executor = Executors.newCachedThreadPool()
	val future = executor.submit {
		val process: Process = Runtime.getRuntime().exec(command)
		processRef = process
		process.outputStream.use {
			if (input != null) it.write(input.toByteArray())
			it.flush()
		}
		process.waitFor()
		output = process.inputStream.use { it.reader().useLines { it.toList() } }
		outputErr = process.errorStream.use { it.reader().useLines { it.toList() } }
		process.destroy()
	}
	try {
		future.get(timeLimit, TimeUnit.MILLISECONDS)
	} catch (ignored: Throwable) {
		// timeout? catch it and give up anyway
	} finally {
		processRef?.destroy()
	}
	return output to outputErr
}

fun TextRange.subRange(start: Int, end: Int) = TextRange(startOffset + start, startOffset + end + 1)

fun VirtualFile.findOrCreate(dir: String, module: Module): VirtualFile {
	return this.findChild(dir) ?: this.createChildDirectory(module, dir)
}

inline fun initExeComboBox(
		zigExeField: ComboboxWithBrowseButton,
		project: Project? = null,
		crossinline addListener: (ComboboxWithBrowseButton) -> Unit = {}) {
	zigExeField.addBrowseFolderListener(ZigBundle.message("zig.messages.run.select-compiler"),
			ZigBundle.message("zig.messages.run.select-compiler.description"),
			project,
			FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
			object : TextComponentAccessor<JComboBox<Any>> {
				override fun getText(component: JComboBox<Any>) = component.selectedItem as? String ?: ""
				override fun setText(component: JComboBox<Any>, text: String) {
					component.addItem(text)
					addListener(zigExeField)
				}
			})
	for (knownZigExePath in zigGlobalSettings.allKnownZigExePaths) {
		zigExeField.comboBox.addItem(knownZigExePath)
	}
}