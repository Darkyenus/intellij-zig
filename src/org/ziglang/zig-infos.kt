package org.ziglang

import com.intellij.AbstractBundle
import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import icons.ZigIcons
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import org.ziglang.psi.ZigGlobalVarDeclaration
import java.util.*

object ZigLanguage : Language("Zig", "text/zig") {
	override fun isCaseSensitive(): Boolean = false
}

object ZigFileType : LanguageFileType(ZigLanguage) {
	override fun getIcon() = ZigIcons.ZIG_FILE
	override fun getName() = "Zig"
	override fun getDefaultExtension() = "zig"
	override fun getDescription() = ZigBundle.message("zig.description")
}

class ZigFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ZigLanguage) {
	override fun getFileType() = ZigFileType
	override fun processDeclarations(processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement?, place: PsiElement): Boolean =
			children.all {
				((it as? ZigGlobalVarDeclaration)?.varDecl ?: it)
						.processDeclarations(processor, state, lastParent, place)
			}
}

class ZigContext : TemplateContextType("ZIG_CONTEXT_ID", "Zig") {
	override fun isInContext(file: PsiFile, offset: Int) = file.fileType == ZigFileType
}

object ZigBundle {
	@NonNls
	private const val BUNDLE = "org.ziglang.zig-bundle"
	private val bundle: ResourceBundle by lazy { ResourceBundle.getBundle(BUNDLE) }

	@JvmStatic
	fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
			AbstractBundle.message(bundle, key, *params)
}

