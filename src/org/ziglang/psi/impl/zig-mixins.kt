package org.ziglang.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.ElementManipulators
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.PsiTreeUtil
import org.ziglang.psi.ZigBlock
import org.ziglang.psi.ZigFnCallArguments
import org.ziglang.psi.ZigPrimaryTypeExpr
import org.ziglang.psi.ZigStringLiteral
import org.ziglang.psi.ZigSuffixExpr
import org.ziglang.psi.ZigSuffixOp
import org.ziglang.psi.ZigTypes
import org.ziglang.psi.ZigVarDecl

abstract class ZigStringMixin(node: ASTNode) : ZigExprImpl(node), ZigStringLiteral {
	override fun isValidHost() = true
	override fun updateText(text: String): ZigStringMixin = ElementManipulators.handleContentChange(this, text)
	override fun createLiteralTextEscaper() = LiteralTextEscaper.createSimple(this)
}

abstract class ZigBlockMixin(node: ASTNode) : ASTWrapperPsiElement(node), ZigBlock {
	override fun processDeclarations(
			processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement?, place: PsiElement) =
			statementList.all {
				it.firstChild
						.let { (it as? ZigVarDecl) ?: it }
						.processDeclarations(processor, state, lastParent, place)
			}
}

abstract class ZigSuffixExprMixin(node: ASTNode) : ASTWrapperPsiElement(node), ZigSuffixExpr {

	override fun isAsync(): Boolean = findChildByType<PsiElement>(ZigTypes.ASYNC_KEYWORD) != null

	override fun getPrimaryExpression(): ZigPrimaryTypeExpr? = expr as? ZigPrimaryTypeExpr

	override fun getSuffixOperations(): List<PsiElement> {
		return PsiTreeUtil.getChildrenOfAnyType(this, ZigSuffixOp::class.java, ZigFnCallArguments::class.java)
	}

}
interface ZigSuffixExprMixinI {
	/** Whether or not is this expression async.
	 * Async suffix expressions can have only one [ZigFnCallArguments] and it must be at the end. */
	fun isAsync():Boolean
	/** Get primary expression on which the others */
	fun getPrimaryExpression(): ZigPrimaryTypeExpr?
	/** Get list of suffix operations - they can only be instances of [ZigSuffixOp] and [ZigFnCallArguments] */
	fun getSuffixOperations(): List<PsiElement>
}