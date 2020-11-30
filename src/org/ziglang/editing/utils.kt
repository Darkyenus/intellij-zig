package org.ziglang.editing

import com.intellij.psi.PsiElement
import org.ziglang.ZigFile
import org.ziglang.psi.ZigGlobalFnDeclaration
import org.ziglang.psi.ZigGlobalFnPrototype
import org.ziglang.psi.ZigGlobalUsingNamespace
import org.ziglang.psi.ZigGlobalVarDeclaration
import org.ziglang.psi.ZigIfExpr
import org.ziglang.psi.ZigTestDecl

fun PsiElement.presentText(): String = when (this) {
	is ZigFile -> name
	is ZigIfExpr -> "if ${children.getOrNull(1)?.text ?: ""}"
	is ZigGlobalFnDeclaration -> "fn ${name ?: "???"}()"
	is ZigGlobalFnPrototype -> "fn ${name ?: "???"}()"
	is ZigGlobalVarDeclaration -> "${if (varDecl.isConst) "const" else "var"} ${varDecl.name}"
	is ZigGlobalUsingNamespace -> "usingnamespace ${expr?.text ?: "???"}"
	else -> text
}

val PsiElement.treeViewTokens
	get() = this is ZigFile ||
			this is ZigGlobalFnDeclaration ||
			this is ZigGlobalFnPrototype ||
			this is ZigGlobalVarDeclaration ||
			this is ZigGlobalUsingNamespace ||
			this is ZigTestDecl