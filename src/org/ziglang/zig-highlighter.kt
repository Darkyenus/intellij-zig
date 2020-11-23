package org.ziglang

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.options.colors.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import icons.ZigIcons
import org.ziglang.psi.ZigTypes

object ZigSyntaxHighlighter : SyntaxHighlighter {
	@JvmField val KEYWORDS = arrayOf(
			ZigTypes.ALIGN_KEYWORD,
			ZigTypes.ALLOWZERO_KEYWORD,
			ZigTypes.AND_KEYWORD,
			ZigTypes.ANYFRAME_KEYWORD,
			ZigTypes.ANYTYPE_KEYWORD,
			ZigTypes.ASM_KEYWORD,
			ZigTypes.ASYNC_KEYWORD,
			ZigTypes.AWAIT_KEYWORD,
			ZigTypes.BREAK_KEYWORD,
			ZigTypes.CALLCONV_KEYWORD,
			ZigTypes.CATCH_KEYWORD,
			ZigTypes.COMPTIME_KEYWORD,
			ZigTypes.CONST_KEYWORD,
			ZigTypes.CONTINUE_KEYWORD,
			ZigTypes.DEFER_KEYWORD,
			ZigTypes.ELSE_KEYWORD,
			ZigTypes.ENUM_KEYWORD,
			ZigTypes.ERRDEFER_KEYWORD,
			ZigTypes.ERROR_KEYWORD,
			ZigTypes.EXPORT_KEYWORD,
			ZigTypes.EXTERN_KEYWORD,
			ZigTypes.FALSE_KEYWORD,
			ZigTypes.FN_KEYWORD,
			ZigTypes.FOR_KEYWORD,
			ZigTypes.IF_KEYWORD,
			ZigTypes.INLINE_KEYWORD,
			ZigTypes.NOALIAS_KEYWORD,
			ZigTypes.NOSUSPEND_KEYWORD,
			ZigTypes.NOINLINE_KEYWORD,
			ZigTypes.NULL_KEYWORD,
			ZigTypes.OPAQUE_KEYWORD,
			ZigTypes.OR_KEYWORD,
			ZigTypes.ORELSE_KEYWORD,
			ZigTypes.PACKED_KEYWORD,
			ZigTypes.PUB_KEYWORD,
			ZigTypes.RESUME_KEYWORD,
			ZigTypes.RETURN_KEYWORD,
			ZigTypes.LINKSECTION_KEYWORD,
			ZigTypes.STRUCT_KEYWORD,
			ZigTypes.SUSPEND_KEYWORD,
			ZigTypes.SWITCH_KEYWORD,
			ZigTypes.TEST_KEYWORD,
			ZigTypes.THREADLOCAL_KEYWORD,
			ZigTypes.TRUE_KEYWORD,
			ZigTypes.TRY_KEYWORD,
			ZigTypes.UNDEFINED_KEYWORD,
			ZigTypes.UNION_KEYWORD,
			ZigTypes.UNREACHABLE_KEYWORD,
			ZigTypes.USINGNAMESPACE_KEYWORD,
			ZigTypes.VAR_KEYWORD,
			ZigTypes.VOLATILE_KEYWORD,
			ZigTypes.WHILE_KEYWORD
	)

	@JvmField val PARENS = arrayOf(
			ZigTypes.LPAREN,
			ZigTypes.RPAREN
	)

	@JvmField val BRACKETS = arrayOf(
			ZigTypes.LBRACKET,
			ZigTypes.RBRACKET
	)

	@JvmField val BRACES = arrayOf(
			ZigTypes.LBRACE,
			ZigTypes.RBRACE
	)

	@JvmField val OPERATORS = arrayOf(
			ZigTypes.AMPERSAND,
			ZigTypes.AMPERSANDEQUAL,
			ZigTypes.ASTERISK,
			ZigTypes.ASTERISK2,
			ZigTypes.ASTERISKEQUAL,
			ZigTypes.ASTERISKPERCENT,
			ZigTypes.ASTERISKPERCENTEQUAL,
			ZigTypes.CARET,
			ZigTypes.CARETEQUAL,
			ZigTypes.COLON,
			ZigTypes.COMMA,
			ZigTypes.DOT,
			ZigTypes.DOT2,
			ZigTypes.DOT3,
			ZigTypes.DOTASTERISK,
			ZigTypes.DOTQUESTIONMARK,
			ZigTypes.EQUAL,
			ZigTypes.EQUALEQUAL,
			ZigTypes.EQUALRARROW,
			ZigTypes.EXCLAMATIONMARK,
			ZigTypes.EXCLAMATIONMARKEQUAL,
			ZigTypes.LARROW,
			ZigTypes.LARROW2,
			ZigTypes.LARROW2EQUAL,
			ZigTypes.LARROWEQUAL,
			ZigTypes.MINUS,
			ZigTypes.MINUSEQUAL,
			ZigTypes.MINUSPERCENT,
			ZigTypes.MINUSPERCENTEQUAL,
			ZigTypes.MINUSRARROW,
			ZigTypes.PERCENT,
			ZigTypes.PERCENTEQUAL,
			ZigTypes.PIPE,
			ZigTypes.PIPE2,
			ZigTypes.PIPEEQUAL,
			ZigTypes.PLUS,
			ZigTypes.PLUS2,
			ZigTypes.PLUSEQUAL,
			ZigTypes.PLUSPERCENT,
			ZigTypes.PLUSPERCENTEQUAL,
			ZigTypes.LETTERC,
			ZigTypes.QUESTIONMARK,
			ZigTypes.RARROW,
			ZigTypes.RARROW2,
			ZigTypes.RARROW2EQUAL,
			ZigTypes.RARROWEQUAL,
			ZigTypes.SEMICOLON,
			ZigTypes.SLASH,
			ZigTypes.SLASHEQUAL,
			ZigTypes.TILDE
	)

	@JvmField val KEYWORD = TextAttributesKey.createTextAttributesKey("ZIG_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
	@JvmField val SYMBOL = TextAttributesKey.createTextAttributesKey("ZIG_SYMBOL", HighlighterColors.TEXT)
	@JvmField val NUMBER = TextAttributesKey.createTextAttributesKey("ZIG_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
	@JvmField val STRING = TextAttributesKey.createTextAttributesKey("ZIG_STRING", DefaultLanguageHighlighterColors.STRING)
	@JvmField val STRING_ESCAPE = TextAttributesKey.createTextAttributesKey("ZIG_STRING_ESCAPE", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)
	@JvmField val STRING_ESCAPE_INVALID = TextAttributesKey.createTextAttributesKey("ZIG_STRING_ESCAPE_INVALID", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE)
	@JvmField val LINE_COMMENT = TextAttributesKey.createTextAttributesKey("ZIG_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
	@JvmField val SEMICOLON = TextAttributesKey.createTextAttributesKey("ZIG_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
	@JvmField val UNDEFINED = TextAttributesKey.createTextAttributesKey("ZIG_UNDEFINED", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL)
	@JvmField val OPERATOR = TextAttributesKey.createTextAttributesKey("ZIG_OPERATORS", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL)
	@JvmField val PAREN = TextAttributesKey.createTextAttributesKey("ZIG_PARENS", DefaultLanguageHighlighterColors.PARENTHESES)
	@JvmField val BRACKET = TextAttributesKey.createTextAttributesKey("ZIG_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
	@JvmField val BRACE = TextAttributesKey.createTextAttributesKey("ZIG_BRACES", DefaultLanguageHighlighterColors.BRACES)
	@JvmField val FUNCTION_DECLARATION = TextAttributesKey.createTextAttributesKey("ZIG_FUNCTION_DECLARATION", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
	@JvmField val BUILTIN_FUNCTION_CALL = TextAttributesKey.createTextAttributesKey("ZIG_BUILTIN_FUNCTION_CALL", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL)

	@JvmField val KEYWORD_KEY = arrayOf(KEYWORD)
	@JvmField val STRING_KEY = arrayOf(STRING)
	@JvmField val NUMBER_KEY = arrayOf(NUMBER)
	@JvmField val SYMBOL_KEY = arrayOf(SYMBOL)
	@JvmField val LINE_COMMENT_KEY = arrayOf(LINE_COMMENT)
	@JvmField val SEMICOLON_KEY = arrayOf(SEMICOLON)
	@JvmField val UNDEFINED_KEY = arrayOf(UNDEFINED)
	@JvmField val OPERATOR_KEY = arrayOf(OPERATOR)
	@JvmField val PARENS_KEY = arrayOf(PAREN)
	@JvmField val BRACKETS_KEY = arrayOf(BRACKET)
	@JvmField val BRACES_KEY = arrayOf(BRACE)
	@JvmField val BUILTIN_FUNCTION_CALL_KEY = arrayOf(BUILTIN_FUNCTION_CALL)

	override fun getHighlightingLexer() = ZigLexerAdapter()
	override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> = when (tokenType) {
		ZigTypes.IDENTIFIER -> SYMBOL_KEY
		ZigTokenType.LINE_COMMENT -> LINE_COMMENT_KEY
		ZigTypes.SEMICOLON -> SEMICOLON_KEY
		ZigTypes.STRING_LITERAL_SINGLE, ZigTypes.LINE_STRING -> STRING_KEY
		ZigTypes.INTEGER_LITERAL,
		ZigTypes.FLOAT_LITERAL -> NUMBER_KEY
		ZigTypes.UNDEFINED_KEYWORD -> UNDEFINED_KEY
		ZigTypes.BUILTIN_IDENTIFIER -> BUILTIN_FUNCTION_CALL_KEY
		in PARENS -> PARENS_KEY
		in BRACKETS -> BRACKETS_KEY
		in BRACES -> BRACES_KEY
		in KEYWORDS -> KEYWORD_KEY
		in OPERATORS -> OPERATOR_KEY
		else -> emptyArray()
	}
}

class ZigSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
	override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) =
			ZigSyntaxHighlighter
}

class ZigColorSettingsPage : ColorSettingsPage {
	private companion object DescriptionHolder {
		private val DESCRIPTION = arrayOf(
				AttributesDescriptor(ZigBundle.message("zig.highlighter.settings.keyword"), ZigSyntaxHighlighter.KEYWORD),
				AttributesDescriptor(ZigBundle.message("zig.highlighter.settings.number"), ZigSyntaxHighlighter.NUMBER),
				AttributesDescriptor(ZigBundle.message("zig.highlighter.settings.string"), ZigSyntaxHighlighter.STRING),
				AttributesDescriptor(ZigBundle.message("zig.highlighter.settings.string-escape"), ZigSyntaxHighlighter.STRING_ESCAPE),
				AttributesDescriptor(ZigBundle.message("zig.highlighter.settings.string-escape-invalid"), ZigSyntaxHighlighter.STRING_ESCAPE_INVALID),
				AttributesDescriptor(ZigBundle.message("zig.highlighter.settings.comment"), ZigSyntaxHighlighter.LINE_COMMENT),
				AttributesDescriptor(ZigBundle.message("zig.highlighter.settings.operator.ordinary"), ZigSyntaxHighlighter.OPERATOR),
				AttributesDescriptor(ZigBundle.message("zig.highlighter.settings.operator.semicolon"), ZigSyntaxHighlighter.SEMICOLON),
				AttributesDescriptor(ZigBundle.message("zig.highlighter.settings.operator.parens"), ZigSyntaxHighlighter.PAREN),
				AttributesDescriptor(ZigBundle.message("zig.highlighter.settings.operator.brackets"), ZigSyntaxHighlighter.BRACKET),
				AttributesDescriptor(ZigBundle.message("zig.highlighter.settings.operator.braces"), ZigSyntaxHighlighter.BRACE),
				AttributesDescriptor(ZigBundle.message("zig.highlighter.settings.reference.ordinary"), ZigSyntaxHighlighter.SYMBOL),
				AttributesDescriptor(ZigBundle.message("zig.highlighter.settings.reference.builtin"), ZigSyntaxHighlighter.BUILTIN_FUNCTION_CALL)
		)

		private val ADDITIONAL_DESCRIPTIONS = mapOf(
				"functionName" to ZigSyntaxHighlighter.FUNCTION_DECLARATION,
				"builtinFunction" to ZigSyntaxHighlighter.BUILTIN_FUNCTION_CALL,
				"escape" to ZigSyntaxHighlighter.STRING_ESCAPE,
				"escapeInvalid" to ZigSyntaxHighlighter.STRING_ESCAPE_INVALID
		)
	}

	override fun getHighlighter() = ZigSyntaxHighlighter
	override fun getAdditionalHighlightingTagToDescriptorMap() = ADDITIONAL_DESCRIPTIONS
	override fun getIcon() = ZigIcons.ZIG_BIG_ICON
	override fun getAttributeDescriptors() = DESCRIPTION
	override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
	override fun getDisplayName() = ZigFileType.name
	// @Language("Zig")
	override fun getDemoText() =
			"""
		// Comment
		const std = @<builtinFunction>import</builtinFunction>("std");
		pub fn <functionName>main</functionName>() !void {
		    var aVar = []f64{1 + 2.0};
		    std.debug.warn("This is a<escape>\n</escape>new line<escapeInvalid>\g</escapeInvalid>");
		}
		""".trimIndent()
}
