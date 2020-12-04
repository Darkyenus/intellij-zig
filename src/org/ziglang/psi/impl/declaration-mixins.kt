package org.ziglang.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.ziglang.ZigTokenType
import org.ziglang.psi.ZigGlobalFnDeclaration
import org.ziglang.psi.ZigGlobalFnPrototype
import org.ziglang.psi.ZigParamDecl
import org.ziglang.psi.ZigSymbol
import org.ziglang.psi.ZigTypes
import org.ziglang.psi.ZigVarDecl

/** Something that serves as a declaration, with a name. */
interface ZigDeclaration : ZigDeclarationHolder, PsiNameIdentifierOwner {

	override fun declaration(): ZigDeclaration = this

	override fun getNameIdentifier():ZigSymbol?
}

/** An element which, while not declaration itself, serves as one. (For example wraps it with modifiers.) */
interface ZigDeclarationHolder : PsiElement {
	/** Get the declaration that this holder holds. */
	fun declaration():ZigDeclaration?
}

/** Base class for all named declarations. */
abstract class ZigAbstractDeclaration(node: ASTNode) : ASTWrapperPsiElement(node), ZigDeclaration {

	override fun getNameIdentifier():ZigSymbol? = PsiTreeUtil.findChildOfType(this, ZigSymbol::class.java)

	override fun setName(name: String) = also { nameIdentifier?.replace(ZigTokenType.fromText(name, project)) }

	override fun getName() = nameIdentifier?.text
}

/** Variable declaration base. */
abstract class ZigVariableDeclarationMixin(node: ASTNode) : ZigAbstractDeclaration(node), ZigVarDecl, ZigVariableDeclarationMixinI {

	override fun getNameIdentifier():ZigSymbol? = variableName

	override val isConst: Boolean
		get() = node.findChildByType(ZigTypes.CONST_KEYWORD) != null
}
interface ZigVariableDeclarationMixinI : PsiNameIdentifierOwner {
	val isConst: Boolean
}

/** Function parameter declaration base. */
abstract class ZigParamDeclarationMixin(node: ASTNode) : ZigAbstractDeclaration(node), ZigParamDecl, ZigParamDeclarationMixinI {
	override fun getNameIdentifier(): ZigSymbol? = parameterName
}
interface ZigParamDeclarationMixinI : PsiNameIdentifierOwner

/** Function declaration base. (= function with body) */
abstract class ZigFnDeclarationMixin(node: ASTNode) : ZigAbstractDeclaration(node), ZigGlobalFnDeclaration, ZigFnDeclarationMixinI {

	override fun getNameIdentifier(): ZigSymbol? = functionPrototype.functionName
}
interface ZigFnDeclarationMixinI : PsiNameIdentifierOwner

/** Function prototype declaration base. (= function without body) */
abstract class ZigFnPrototypeMixin(node:ASTNode) : ZigAbstractDeclaration(node), ZigGlobalFnPrototype, ZigFnPrototypeMixinI {
	override fun getNameIdentifier(): ZigSymbol? = functionPrototype.functionName
}
interface ZigFnPrototypeMixinI : PsiNameIdentifierOwner