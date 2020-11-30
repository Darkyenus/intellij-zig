package org.ziglang.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.PsiTreeUtil
import org.ziglang.ZigTokenType
import org.ziglang.psi.ZigGlobalFnDeclaration
import org.ziglang.psi.ZigGlobalFnPrototype
import org.ziglang.psi.ZigParamDecl
import org.ziglang.psi.ZigSymbol
import org.ziglang.psi.ZigTypes
import org.ziglang.psi.ZigVarDecl

/** Base class for all named declarations. */
abstract class ZigAbstractDeclaration(node: ASTNode) : ASTWrapperPsiElement(node), PsiNameIdentifierOwner {

	override fun getNameIdentifier():ZigSymbol? = PsiTreeUtil.findChildOfType(this, ZigSymbol::class.java)

	override fun setName(name: String) = also { nameIdentifier?.replace(ZigTokenType.fromText(name, project)) }

	override fun getName() = nameIdentifier?.text

	override fun processDeclarations(
			processor: PsiScopeProcessor, substitutor: ResolveState, lastParent: PsiElement?, place: PsiElement): Boolean {
		return nameIdentifier?.processDeclarations(processor, substitutor, lastParent, place) != false
	}
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

	override fun processDeclarations(processor: PsiScopeProcessor, substitutor: ResolveState, lastParent: PsiElement?, place: PsiElement): Boolean {
		return (functionPrototype.paramDeclList.all { it.processDeclarations(processor, substitutor, lastParent, place) }
				&& super.processDeclarations(processor, substitutor, lastParent, place))
	}
}
interface ZigFnDeclarationMixinI : PsiNameIdentifierOwner

/** Function prototype declaration base. (= function without body) */
abstract class ZigFnPrototypeMixin(node:ASTNode) : ZigAbstractDeclaration(node), ZigGlobalFnPrototype, ZigFnPrototypeMixinI {
	override fun getNameIdentifier(): ZigSymbol? = functionPrototype.functionName
}
interface ZigFnPrototypeMixinI : PsiNameIdentifierOwner