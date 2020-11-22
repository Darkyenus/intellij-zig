package org.ziglang.editing

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.ziglang.ZigBundle
import org.ziglang.ZigSyntaxHighlighter
import org.ziglang.builtinFunctions
import org.ziglang.psi.ZigExpr
import org.ziglang.psi.ZigIfPrefix
import org.ziglang.psi.ZigPrimaryBoolExpr
import org.ziglang.psi.ZigPrimaryBuiltinExpr
import org.ziglang.psi.ZigStringLiteral
import org.ziglang.psi.ZigSymbol
import org.ziglang.psi.ZigTestDecl
import org.ziglang.psi.impl.firstExprOrNull
import org.ziglang.subRange
import java.util.regex.Pattern
import kotlin.math.min

class ZigAnnotator : Annotator {
	companion object {
		private val escapeChars = mapOf(
				'\\' to 0,
				'n' to 0,
				't' to 0,
				'x' to 2,
				'u' to 4,
				'U' to 6
		)

		private val escapeRegex = Pattern.compile("""\\.""")
	}

	override fun annotate(element: PsiElement, holder: AnnotationHolder) {
		when (element) {
			is ZigSymbol -> symbol(element, holder)
			is ZigPrimaryBuiltinExpr -> macroExpr(element, holder)
			is ZigIfPrefix -> ifExpr(element.condition, holder)
			is ZigTestDecl -> ifExpr(element.firstExprOrNull(), holder)
			is ZigStringLiteral -> string(element, holder)
		}
	}

	@Suppress("RemoveRedundantBackticks", "LocalVariableName")
	private fun macroExpr(macroExpr: ZigPrimaryBuiltinExpr, holder: AnnotationHolder) {
		val builtin = macroExpr.builtin
		val builtinName = builtin.text.removePrefix("@")
		if (builtinName !in builtinFunctions) {
			holder.newAnnotation(HighlightSeverity.ERROR, ZigBundle.message("zig.lint.unknown-builtin-symbol"))
					.range(builtin)
					.highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
					.create()
			// registerFix(ZigRemoveElementIntention(`@`, ZigBundle.message("zig.lint.un-builtin")))
		}
	}

	private fun symbol(element: ZigSymbol, holder: AnnotationHolder) {
		when {
			element.isFunctionName ->
				holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
						.range(element)
						.textAttributes(ZigSyntaxHighlighter.FUNCTION_DECLARATION)
						.create()
		}
	}

	private fun ifExpr(condition: ZigExpr?, holder: AnnotationHolder) {
		when {
			condition is ZigPrimaryBoolExpr ->
				holder.newAnnotation(HighlightSeverity.WARNING, ZigBundle.message("zig.lint.const-condition", condition.text))
						.range(condition)
						.create()
		}
	}

	private fun string(element: ZigStringLiteral, holder: AnnotationHolder) {
		fun String.nextString(start: Int, length: Int) =
				substring(start, min(start + length, this.length))

		val matcher = escapeRegex.matcher(element.text)

		while (matcher.find()) {
			val start = matcher.start()
			val end = matcher.end() - 1
			val char = matcher.group()[1]

			if (char in escapeChars) {
				val nextCount = escapeChars[char] ?: return
				val accept = element.text.nextString(start + 2, nextCount).run {
					isEmpty() || all { it in "0123456789ABCDEFabcdef" }
				}

				if (accept) {
					holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
							.range(element.textRange.subRange(start, end + nextCount))
							.textAttributes(ZigSyntaxHighlighter.STRING_ESCAPE)
							.create()
				}
				continue
			}
			holder.newAnnotation(HighlightSeverity.ERROR, ZigBundle.message("zig.lint.illegal-escape"))
					.range(element.textRange.subRange(start, end))
					.textAttributes(ZigSyntaxHighlighter.STRING_ESCAPE_INVALID)
					.create()
		}
	}
}