package org.ziglang

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.ziglang.psi.ZigTypes

fun newZigLexer(): Lexer = FlexAdapter(ZigLexer())

class ZigParserDefinition : ParserDefinition {
	override fun createParser(project: Project?): PsiParser = ZigParser()
	override fun createLexer(project: Project?): Lexer = newZigLexer()
	override fun createFile(viewProvider: FileViewProvider): ZigFile = ZigFile(viewProvider)
	override fun createElement(node: ASTNode?): PsiElement = ZigTypes.Factory.createElement(node)
	override fun getStringLiteralElements(): TokenSet = ZigTokenType.STRINGS
	override fun getCommentTokens(): TokenSet = ZigTokenType.COMMENTS
	override fun getFileNodeType(): IFileElementType = ZIG_PSI_FILE_TYPE
	override fun getWhitespaceTokens(): TokenSet = TokenSet.WHITE_SPACE

	override fun spaceExistenceTypeBetweenTokens(left: ASTNode?, right: ASTNode?): ParserDefinition.SpaceRequirements {
		val leftType = left?.elementType ?: return ParserDefinition.SpaceRequirements.MAY
		val rightType = right?.elementType ?: return ParserDefinition.SpaceRequirements.MAY

		if (leftType in ZigTokenType.IDENTIFIERS && rightType in ZigTokenType.IDENTIFIERS) {
			return ParserDefinition.SpaceRequirements.MUST
		}
		return ParserDefinition.SpaceRequirements.MAY
	}

	private companion object FileHolder {
		private val ZIG_PSI_FILE_TYPE = IFileElementType(ZigLanguage)
	}
}

class ZigTokenType(debugName: String) : IElementType(debugName, ZigLanguage) {
	companion object TokenHolder {
		@JvmField val COMMENTS = TokenSet.create(ZigTypes.LINE_COMMENT)
		@JvmField val STRINGS = TokenSet.create(ZigTypes.STRING_LITERAL_SINGLE, ZigTypes.LINE_STRING)
		@JvmField val IDENTIFIERS = TokenSet.create(ZigTypes.IDENTIFIER, ZigTypes.SYMBOL)

		/** Token types which have no semantic meaning. */
		@JvmField val IGNORABLE = TokenSet.create(ZigTypes.LINE_COMMENT, TokenType.WHITE_SPACE, TokenType.BAD_CHARACTER)

		fun fromText(string: String, project: Project): PsiElement = PsiFileFactory
				.getInstance(project)
				.createFileFromText(ZigLanguage, string)
				.firstChild
				.let { (it as? PsiErrorElement)?.firstChild ?: it }
	}
}

class ZigElementType(debugName: String) : IElementType(debugName, ZigLanguage)
