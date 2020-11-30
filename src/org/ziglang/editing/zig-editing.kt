package org.ziglang.editing

import com.intellij.lang.ASTNode
import com.intellij.lang.BracePair
import com.intellij.lang.Commenter
import com.intellij.lang.PairedBraceMatcher
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.tree.IElementType
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.Tokenizer
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import org.ziglang.ZigBundle
import org.ziglang.ZigLanguage
import org.ziglang.ZigTokenType
import org.ziglang.newZigLexer
import org.ziglang.psi.ZigBlock
import org.ziglang.psi.ZigBlockExpr
import org.ziglang.psi.ZigErrorSetDecl
import org.ziglang.psi.ZigGlobalFnDeclaration
import org.ziglang.psi.ZigGlobalFnPrototype
import org.ziglang.psi.ZigGlobalVarDeclaration
import org.ziglang.psi.ZigStringLiteral
import org.ziglang.psi.ZigSymbol
import org.ziglang.psi.ZigTestDecl
import org.ziglang.psi.ZigTopLevelComptime
import org.ziglang.psi.ZigTypes
import org.ziglang.psi.ZigVariableDeclarationStatement

class ZigCommenter : Commenter {
	override fun getCommentedBlockCommentPrefix(): String? = blockCommentPrefix
	override fun getCommentedBlockCommentSuffix(): String? = blockCommentSuffix
	override fun getBlockCommentPrefix(): String? = null
	override fun getBlockCommentSuffix(): String? = null
	override fun getLineCommentPrefix() = "//"
}

class ZigBraceMatcher : PairedBraceMatcher {
	private companion object PairHolder {
		private val PAIRS = arrayOf(
				BracePair(ZigTypes.LBRACE,   ZigTypes.RBRACE, false),
				BracePair(ZigTypes.LBRACKET, ZigTypes.RBRACKET, false),
				BracePair(ZigTypes.LPAREN,   ZigTypes.RPAREN, false)
		)
	}

	override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int) = openingBraceOffset
	override fun getPairs() = PAIRS
	override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?) = true
}

object ZigNameValidator : InputValidatorEx {
	override fun canClose(inputString: String?) = checkInput(inputString)
	override fun checkInput(inputString: String?) = inputString?.run {
			all {
				it.isLetterOrDigit() || it == '_'
			} && firstOrNull()?.isDigit() != true
		} == true

	override fun getErrorText(inputString: String?) =
			ZigBundle.message("zig.actions.new-file.invalid", inputString.orEmpty())
}

class ZigSpellcheckerStrategy : SpellcheckingStrategy() {
	override fun getTokenizer(element: PsiElement): Tokenizer<PsiElement> = when (element) {
		is PsiComment -> TEXT_TOKENIZER
		is ZigSymbol -> if (element.isDeclaration) TEXT_TOKENIZER else EMPTY_TOKENIZER
		is ZigStringLiteral -> super.getTokenizer(element).takeIf { it != EMPTY_TOKENIZER } ?: TEXT_TOKENIZER
		else -> EMPTY_TOKENIZER
	}
}

class ZigFolderBuilder : FoldingBuilderEx(), DumbAware {

	class ZigFoldingDescriptor(element: PsiElement, private val holder: String)
		: FoldingDescriptor(element.node, element.textRange) {
		override fun getPlaceholderText() = holder
	}

	override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<ZigFoldingDescriptor> {
		return SyntaxTraverser.psiTraverser(root)
				.mapNotNull { element ->
					when {
						element.textLength <= 100 -> null // Do not fold elements that are too small (+ attempting to fold 0-length elements crashes)
						element is ZigBlock -> ZigFoldingDescriptor(element, "{…}")
						element is ZigErrorSetDecl -> ZigFoldingDescriptor(element, "error …")
						else -> null
					}
				}.toTypedArray()
	}

	override fun isCollapsedByDefault(node: ASTNode) = true
	override fun getPlaceholderText(node: ASTNode) = "…"
}

fun cutText(it: String, textMax: Int) = if (it.length <= textMax) it else "${it.take(textMax)}…"

class ZigBreadcrumbsProvider : BreadcrumbsProvider {

	override fun getLanguages() = arrayOf(ZigLanguage)

	override fun getElementInfo(element: PsiElement): String {
		return cutText(when (element) {
			is ZigGlobalFnDeclaration -> (element.name ?: "???") + "()"
			is ZigGlobalFnPrototype -> (element.name ?: "???") + "()"
			is ZigTestDecl -> element.testName?.text ?: "???"
			is ZigTopLevelComptime -> "comptime"
			is ZigBlock -> element.firstChild.text
			is ZigBlockExpr -> "{…}"
			is ZigGlobalVarDeclaration -> element.varDecl.name
			is ZigVariableDeclarationStatement -> element.varDecl.name
			else -> null
		}.orEmpty(), 16)
	}

	override fun acceptElement(element: PsiElement): Boolean {
		return element is ZigGlobalFnDeclaration ||
				element is ZigGlobalFnPrototype ||
				element is ZigTestDecl ||
				element is ZigTopLevelComptime ||
				element is ZigBlock ||
				element is ZigBlockExpr ||
				element is ZigGlobalVarDeclaration ||
				element is ZigVariableDeclarationStatement
	}
}

class ZigFindUsagesProvider : FindUsagesProvider {
	override fun canFindUsagesFor(element: PsiElement) = element is PsiNameIdentifierOwner
	override fun getHelpId(psiElement: PsiElement): String? = null
	override fun getType(element: PsiElement) = ""
	override fun getDescriptiveName(element: PsiElement) = (element as? PsiNamedElement)?.name ?: ""
	override fun getNodeText(element: PsiElement, useFullName: Boolean) = getDescriptiveName(element)
	override fun getWordsScanner() = DefaultWordsScanner(newZigLexer(), ZigTokenType.IDENTIFIERS, ZigTokenType.COMMENTS, ZigTokenType.STRINGS)
}

class ZigRefactoringSupportProvider : RefactoringSupportProvider() {
	override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?) = true
}
