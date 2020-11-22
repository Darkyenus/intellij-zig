package org.ziglang.editing

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ProcessingContext
import icons.ZigIcons
import org.ziglang.ZigBundle
import org.ziglang.ZigFileType
import org.ziglang.builtinFunctions
import org.ziglang.psi.ZigTypes

class ZigCompletionProvider(private val list: List<LookupElement>)
	: CompletionProvider<CompletionParameters>() {
	override fun addCompletions(
			parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) =
			list.forEach(result::addElement)
}

class ZigCompletionContributor : CompletionContributor() {
	private companion object CompletionHolder {
		private val KEYWORD_LITERALS = listOf(
				"unreachable",
				"undefined",
				"error",
				"this",
				"suspend",
				"packed",
				"null",
				"true",
				"false"
		).map {
			LookupElementBuilder.create(it)
					.withIcon(ZigIcons.ZIG_BIG_ICON)
					.withTypeText(ZigBundle.message("zig.completions.keywords"))
					.bold()
		}
		private val EXPR_KEYWORDS = listOf(
				"for",
				"while",
				"if",
				"switch",
				"union",
				"struct",
				"enum",
				"return",
				"try"
		).map {
			LookupElementBuilder.create("$it ")
					.withPresentableText(it)
					.withIcon(ZigIcons.ZIG_BIG_ICON)
					.withTypeText(ZigBundle.message("zig.completions.keywords"))
					.bold()
		}
		private val TOP_KEYWORDS = listOf(
				"const",
				"var",
				"fn",
				"comptime",
				"inline",
				"export",
				"pub"
		).map {
			LookupElementBuilder.create("$it ")
					.withPresentableText(it)
					.withIcon(ZigIcons.ZIG_BIG_ICON)
					.withTypeText(ZigBundle.message("zig.completions.keywords"))
					.bold()
		}
		private val BUILTIN_FUNCTIONS = builtinFunctions.map {
			LookupElementBuilder.create(it)
					.withIcon(ZigIcons.ZIG_BIG_ICON)
					.withTypeText(ZigBundle.message("zig.completions.built-in"))
					.withItemTextUnderlined(true)
					.withInsertHandler { context, _ ->
						context.document.insertString(context.editor.caretModel.offset, "()")
						context.editor.caretModel.moveCaretRelatively(1, 0, false, false, false)
					}
		}
	}

	init {
		extend(CompletionType.BASIC,
				psiElement(ZigTypes.IDENTIFIER)
						.andNot(psiElement().afterLeaf(".", "@")),
				ZigCompletionProvider(KEYWORD_LITERALS))
		extend(CompletionType.BASIC,
				psiElement(ZigTypes.IDENTIFIER)
						.andNot(psiElement().afterLeaf(".", "@"))
						.andNot(psiElement().beforeLeaf(psiElement(ZigTypes.SEMICOLON))),
				ZigCompletionProvider(TOP_KEYWORDS))
		extend(CompletionType.BASIC,
				psiElement(ZigTypes.IDENTIFIER)
						.andNot(psiElement().afterLeaf(".", "@")),
				ZigCompletionProvider(EXPR_KEYWORDS))
		extend(CompletionType.BASIC,
				psiElement(ZigTypes.BUILTIN_IDENTIFIER)
						.andNot(psiElement().afterLeaf(".")),
				ZigCompletionProvider(BUILTIN_FUNCTIONS))
	}
}

class ZigTypedHandlerDelegate : TypedHandlerDelegate() {
	override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
		if (file.fileType == ZigFileType) {
			return if (charTyped in "@.(") {
				Result.CONTINUE
			} else {
				Result.STOP
			}
		}
		return super.checkAutoPopup(charTyped, project, editor, file)
	}
}

