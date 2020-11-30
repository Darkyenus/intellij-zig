package org.ziglang.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveState
import com.intellij.psi.TokenType
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.parentOfTypes
import org.ziglang.ZigTokenType
import org.ziglang.psi.NULL_ZIG_TYPE_RESOLVABLE
import org.ziglang.psi.ZigAsmInputItem
import org.ziglang.psi.ZigAsmOutputItem
import org.ziglang.psi.ZigBlockLabel
import org.ziglang.psi.ZigBreakLabel
import org.ziglang.psi.ZigContainerField
import org.ziglang.psi.ZigCurlySuffixExpr
import org.ziglang.psi.ZigErrorReference
import org.ziglang.psi.ZigErrorSetDecl
import org.ziglang.psi.ZigExprReference
import org.ziglang.psi.ZigFieldInit
import org.ziglang.psi.ZigFnProto
import org.ziglang.psi.ZigLabelReference
import org.ziglang.psi.ZigParamDecl
import org.ziglang.psi.ZigPayload
import org.ziglang.psi.ZigPrimaryAnonymousStructExpr
import org.ziglang.psi.ZigPrimaryEnumReferenceExpr
import org.ziglang.psi.ZigPrimaryErrorReferenceExpr
import org.ziglang.psi.ZigPrimaryReferenceExpr
import org.ziglang.psi.ZigPtrIndexPayload
import org.ziglang.psi.ZigPtrPayload
import org.ziglang.psi.ZigSuffixOp
import org.ziglang.psi.ZigSymbol
import org.ziglang.psi.ZigTypeExpr
import org.ziglang.psi.ZigTypes
import org.ziglang.psi.ZigVarDecl
import org.ziglang.psi.prevSiblingTypeIgnoring

/**
 * Handles symbol references.
 */
abstract class ZigSymbolMixin(node: ASTNode) : ASTWrapperPsiElement(node), ZigSymbol {

	override fun getNameIdentifier() = this
	override fun setName(name: String): PsiElement = replace(ZigTokenType.fromText(name, project))
	override fun getName(): String = text

	/**
	 * Covers all possible symbol types, based on where they occur in the grammar.
	 */
	enum class Type (val isDeclaration:Boolean = false, val isBreakLabel:Boolean = false) {
		/** under fnProto as function name declaration */
		FnDeclaration(isDeclaration = true),
		/** under varDecl as variable name declaration */
		VarDeclaration(isDeclaration = true),
		/** under containerField as field name declaration (can also refer to enum constant) */
		FieldDeclaration(isDeclaration = true),
		/** under primaryReferenceExpr as a reference to anything, pretty much */
		Expression,
		/** under primaryEnumReferenceExpr as a reference to enum constant of implicit enum type */
		EnumReference,
		/** under primaryErrorReferenceExpr as a reference to an error constant,
		 * but it also serves as a declaration of that error. */
		ErrorReference(isDeclaration = true),
		/** under errorIdentifierList as an error constant declaration (may appear multiple times) */
		ErrorDeclaration(isDeclaration = true),
		/** under asmOutputItem, asmInputItem as ??? (experimental zig feature) */
		Assembly,
		/** under breakLabel as a label name reference to blockLabel */
		BreakLabelReference(isBreakLabel = true),
		/** under blockLabel as a label name declaration */
		BreakLabelDeclaration(isDeclaration = true, isBreakLabel = true),
		/** under fieldInit as a field reference */
		FieldReference,
		/** under paramDecl as a function parameter declaration */
		ParameterDeclaration(isDeclaration = true),
		/** under payload, ptrPayload and ptrIndexPayload as a payload name declaration */
		PayloadDeclaration(isDeclaration = true),
		/** under ptrIndexPayload as a payload index name declaration */
		PayloadIndexDeclaration(isDeclaration = true),
		/** under suffixOp as reference to a field or constant name */
		FieldOrConstantReference,

		/** This reference is invalid - the AST is not complete enough to determine what it is. */
		Invalid
	}

	private var typeCache:Type? = null
	private var referenceCache: PsiReference? = null

	/** Type of the reference, based on its appearance in the AST. */
	val type:Type
		get() {
			typeCache?.let { return it }

			// Determine type
			val type = when (val parent: PsiElement? = this.parent) {
				is ZigFnProto -> Type.FnDeclaration
				is ZigVarDecl -> Type.VarDeclaration
				is ZigContainerField -> Type.FieldDeclaration
				is ZigPrimaryReferenceExpr -> Type.Expression
				is ZigPrimaryEnumReferenceExpr -> Type.EnumReference
				is ZigPrimaryErrorReferenceExpr -> Type.ErrorReference
				is ZigErrorSetDecl -> Type.ErrorDeclaration
				is ZigAsmOutputItem, is ZigAsmInputItem -> Type.Assembly
				is ZigBreakLabel -> Type.BreakLabelReference
				is ZigBlockLabel -> Type.BreakLabelDeclaration
				is ZigFieldInit -> Type.FieldReference
				is ZigParamDecl -> Type.ParameterDeclaration
				is ZigPayload, is ZigPtrPayload -> Type.PayloadDeclaration
				is ZigPtrIndexPayload -> when {
					parent.payloadName == this -> Type.PayloadDeclaration
					parent.payloadIndexName == this -> Type.PayloadIndexDeclaration
					else -> Type.Invalid
				}
				is ZigSuffixOp -> Type.FieldOrConstantReference
				else -> Type.Invalid
			}
			typeCache = type
			return type
		}

	/** For [ZigSymbolMixin], we cannot have a reference if it's a declaration. */
	override fun getReference(): PsiReference? {
		referenceCache?.let { return it }

		val type = type
		if (type.isDeclaration) {
			// Error declarations soft-refer to other error declarations of the same name
			if (type == Type.ErrorReference || type == Type.ErrorDeclaration) {
				val ref = ZigErrorReference(this)
				referenceCache = ref
				return ref
			}

			// Declaration does not refer to anything
			return null
		}

		val reference = when {
			type.isBreakLabel -> ZigLabelReference(this)
			type == Type.Expression -> {
				ZigExprReference(this)
			}
			type == Type.EnumReference -> {
				// This one is tricky, because it requires type deduction
				// TODO(jp): implement container resolution
				ZigExprReference(this, NULL_ZIG_TYPE_RESOLVABLE)// TODO(jp): Proper resolvable
			}
			type == Type.FieldReference -> {
				// Reference to a field of a newly constructed struct or something
				// If the struct is anonymous, this is actually the declaration of that field.
				val parent = parentOfTypes(ZigPrimaryAnonymousStructExpr::class, ZigCurlySuffixExpr::class)
				if (parent is ZigPrimaryAnonymousStructExpr) {
					// This is indeed an anonymous struct, it does not refer to anything
					null
				} else {
					val context = (parent as? ZigCurlySuffixExpr)?.expr as? ZigTypeExpr // TODO(jp): expr must be typeExpr, but it is erased, for some reason - I'd have to understand how extend works first...
					ZigExprReference(this, NULL_ZIG_TYPE_RESOLVABLE) // TODO(jp): Proper resolvable
				}
			}
			type == Type.FieldOrConstantReference -> {
				// We must check above to see what are we actually referring to
				ZigExprReference(this, NULL_ZIG_TYPE_RESOLVABLE) // TODO(jp): Proper resolvable
				null
			}
			else -> null
		}

		referenceCache = reference
		return reference
	}


	final override val isFunctionName: Boolean
		get() = parent is ZigFnProto && prevSiblingTypeIgnoring(
				ZigTypes.FN_KEYWORD,
				TokenType.WHITE_SPACE,
				ZigTokenType.LINE_COMMENT) != null

	final override val isParameter: Boolean
		get() = parent is ZigParamDecl

	final override val isVariableName: Boolean
		get() = parent is ZigVarDecl && prevSiblingTypeIgnoring(
				ZigTypes.CONST_KEYWORD,
				TokenType.WHITE_SPACE,
				ZigTokenType.LINE_COMMENT) ?: prevSiblingTypeIgnoring(
				ZigTypes.VAR_KEYWORD,
				TokenType.WHITE_SPACE,
				ZigTokenType.LINE_COMMENT) != null

	override val isDeclaration: Boolean
		get() = isFunctionName ||
				isParameter ||
				isVariableName


	override fun processDeclarations(processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement?, place: PsiElement): Boolean {
		return processor.execute(this, state)
	}

	override fun subtreeChanged() {
		typeCache = null
		referenceCache = null
		super.subtreeChanged()
	}
}

interface ZigSymbolMixinI : PsiNameIdentifierOwner {

	// TODO(jp): Reconsider these and re-implemented them correctly
	val isFunctionName: Boolean
	val isParameter: Boolean
	val isVariableName: Boolean
	val isDeclaration: Boolean
}