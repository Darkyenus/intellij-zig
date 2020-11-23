package org.ziglang.execution

import com.intellij.execution.filters.ConsoleFilterProviderEx
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.execution.filters.UrlFilter
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import java.util.regex.Pattern

class ZigConsoleFilter(private val project: Project) : Filter {
	private companion object {
		private val ERROR_FILE_LOCATION = Pattern.compile("(.+\\.zig):([0-9]+):([0-9]+):")
	}

	override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
		val startIndex = entireLength - line.length
		if (startIndex == 0) return null
		val matcher = ERROR_FILE_LOCATION.matcher(line)
		if (!matcher.lookingAt()) return null
		val resultFilePath = matcher.group(1)
		val resultFile = findFileByPath(resultFilePath) ?: return null
		val lineNumber = matcher.group(2).toIntOrNull() ?: return null
		val columnNumber = matcher.group(3).toIntOrNull() ?: return null
		return Filter.Result(
				startIndex,
				startIndex + resultFilePath.length,
				OpenFileHyperlinkInfo(
						project,
						resultFile,
						(lineNumber - 1).coerceAtLeast(0),
						columnNumber
				)
		)
	}

	private fun findFileByPath(path:String): VirtualFile? {
		LocalFileSystem.getInstance().findFileByPath(path)?.let { return it }

		for (module in ModuleManager.getInstance(project).modules) {
			for (contentRoot in module.rootManager.contentRoots) {
				val found = contentRoot.fileSystem.findFileByPath(path)
				if (found != null) {
					return found
				}
			}
		}

		return null
	}
}

class ZigConsoleFilterProvider : ConsoleFilterProviderEx {
	override fun getDefaultFilters(project: Project, scope: GlobalSearchScope) = getDefaultFilters(project)
	override fun getDefaultFilters(project: Project) = arrayOf(ZigConsoleFilter(project), UrlFilter())
}
