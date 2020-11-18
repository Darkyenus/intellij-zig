package org.ziglang.editing

import com.intellij.psi.PsiElement
import org.ziglang.ZigFile
import org.ziglang.psi.*

fun PsiElement.presentText() = when (this) {
	is ZigFile -> name
	is ZigIfExpr -> "if ${children.getOrNull(1)?.text ?: ""}"
	is ZigGlobalFnDeclaration -> "fn ${functionPrototype.name}()"
	is ZigGlobalFnPrototype -> "fn ${functionPrototype.name}()"
	is ZigGlobalVarDeclaration -> "${if (varDecl.isConst) "const" else "var"} ${varDecl.name}"
	is ZigGlobalUsingNamespace -> "usingnamespace ${expr.text}"
	else -> text
}

val PsiElement.treeViewTokens
	get() = this is ZigFile ||
			this is ZigGlobalFnDeclaration ||
			this is ZigGlobalFnPrototype ||
			this is ZigGlobalVarDeclaration ||
			this is ZigGlobalUsingNamespace ||
			this is ZigTestDecl