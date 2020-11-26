package org.ziglang.action

import com.google.common.io.Files
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.actions.AttributesDefaults
import com.intellij.ide.fileTemplates.ui.CreateFromTemplateDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import icons.ZigIcons
import org.ziglang.ZigBundle
import org.ziglang.ZigFileType
import org.ziglang.editing.ZigNameValidator
import org.ziglang.executeCommand
import org.ziglang.execution.ZigRunConfiguration
import org.ziglang.execution.ZigRunConfigurationType
import org.ziglang.project.validateZigExe
import org.ziglang.project.zigSettings
import org.ziglang.project.zigSettingsNullable
import org.ziglang.trimPath

class ZigTranslateFromCAction : AnAction(
		ZigBundle.message("zig.actions.c-translate.title"),
		ZigBundle.message("zig.actions.c-translate.description"),
		ZigIcons.ZIG_WEBSITE_ICON) {
	override fun actionPerformed(e: AnActionEvent) {

		val project = e.project ?: return
		val zigSettings = project.zigSettings.settings
		val cFile = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext) ?: return
		val zigFileName = "${cFile.nameWithoutExtension}.${ZigFileType.defaultExtension}"
		val (stdout, stderr) = ProgressManager.getInstance().run(object :
				Task.WithResult<Pair<List<String>, List<String>>, Exception>(
						project, "", false) {
			override fun compute(indicator: ProgressIndicator) = executeCommand(arrayOf(
					zigSettings.exePath,
					"translate-c",
					cFile.path
			), timeLimit = 10000L)
		})
		// TODO(jp): This crashes
		ApplicationManager.getApplication().runWriteAction {
			if (stderr.isNotEmpty() && stderr.all(String::isNotEmpty)) Messages.showErrorDialog(
					project,
					stderr.joinToString("\n"),
					ZigBundle.message("zig.actions.c-translate.error.title"))
			if (stdout.isNotEmpty() && stdout.all(String::isNotEmpty)) {
				val zigFile = cFile.parent.findOrCreateChildData(this, zigFileName)
				VfsUtil.saveText(zigFile, stdout.joinToString("\n"))
				OpenFileDescriptor(project, zigFile).navigate(true)
			}
		}
	}

	override fun update(e: AnActionEvent) {
		val zigExe = e.project?.zigSettingsNullable?.settings?.exePath ?: return
		val presentation = e.presentation
		presentation.isVisible = validateZigExe(zigExe)
		val file = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)
		val fileType = file?.fileType as? LanguageFileType
		if (file == null || (fileType != null && !fileType.name.toLowerCase().contains('c'))) {
			// If there is no file to work on or if we know the file type and it definitely is not C file (very rudimentary check)
			presentation.text = ZigBundle.message("zig.actions.c-translate.title")
			presentation.isEnabled = false
		} else {
			// There is a file to work with and it might be a C file
			presentation.text = ZigBundle.message("zig.actions.c-translate.title-file", file.name)
			presentation.isEnabled = true
		}
	}
}

class ZigBuildAction : ZigAction(
		ZigBundle.message("zig.actions.build.title"),
		ZigBundle.message("zig.actions.build.description")) {
	override fun actionPerformed(e: AnActionEvent) {
		val project = e.project ?: return
		val file = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext) ?: return
		val path = file.path.trimPath()
		val executor = ExecutorRegistry.getInstance().getExecutorById(DefaultRunExecutor.EXECUTOR_ID)!!
		val configuration = RunManager
				.getInstance(project)
				.let {
					it.createConfiguration(
							"build${Files.getNameWithoutExtension(path)}",
							ZigRunConfigurationType.configurationFactories[0]
					).apply {
						it.addConfiguration(this)
						(configuration as ZigRunConfiguration).apply {
							targetFile = path
							isBuildOnly = true
						}
					}

				}
		ProgramRunnerUtil.executeConfiguration(configuration, executor)
	}
}

// TODO(jp): Does not work, dunno why, all files are where they are supposed to be, probably something changed in IntelliJ?
class NewZigFile : CreateFileFromTemplateAction(
		ZigBundle.message("zig.actions.new-file.title"),
		ZigBundle.message("zig.actions.new-file.description"),
		ZigIcons.ZIG_FILE), DumbAware {
	companion object PropertyCreator {
		fun createProperties(project: Project, fileName: String) =
				FileTemplateManager.getInstance(project).defaultProperties.also { properties ->
					properties += "ZIG_VERSION" to project.zigSettings.settings.version
					properties += "NAME" to fileName
				}
	}

	override fun getActionName(dir: PsiDirectory, name: String, templateName: String) =
			ZigBundle.message("zig.actions.new-file.title")

	override fun buildDialog(project: Project?, dir: PsiDirectory?, builder: CreateFileFromTemplateDialog.Builder) {
		builder
				.setTitle(ZigBundle.message("zig.actions.new-file.title"))
				.setValidator(ZigNameValidator)
				.addKind("File", ZigIcons.ZIG_FILE, "Zig File")
				.addKind("Executable", ZigIcons.ZIG_FILE, "Zig Exe")
		//.addKind("Other", ZigIcons.ZIG_BIG_ICON, "Zig Other")		For test
	}

	override fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory) = try {
		val fileName = FileUtilRt.getNameWithoutExtension(name)
		val project = dir.project
		val properties = createProperties(project, fileName)
		CreateFromTemplateDialog(project, dir, template, AttributesDefaults(fileName).withFixedName(true), properties)
				.create()
				.containingFile
	} catch (e: Exception) {
		LOG.error("Error while creating new file", e)
		null
	}
}
