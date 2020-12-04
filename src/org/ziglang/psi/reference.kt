package org.ziglang.psi

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.ArrayUtilRt
import org.ziglang.ZigTokenType.TokenHolder.IGNORABLE
import org.ziglang.psi.impl.ZigDeclaration
import org.ziglang.psi.impl.ZigDeclarationHolder
import org.ziglang.psi.impl.ZigSymbolType

/**
 * A base for references to anything.
 */
abstract class AbstractZigReference(protected val origin: ZigSymbol) : PsiReference {

	private val range = TextRange(0, element.textLength)
	protected var referenceTo: PsiElement? = null

	//region Origin info
	override fun getElement(): ZigSymbol = origin

	override fun getRangeInElement(): TextRange = range

	override fun getCanonicalText(): String = origin.text
	//endregion

	//region Resolution
	override fun isSoft(): Boolean = false

	override fun resolve(): PsiElement? {
		referenceTo?.let { return it }
		val ref = doResolve()
		referenceTo = ref
		return ref
	}

	/**
	 * @return [ZigSymbol] because only that generates references
	 */
	protected open fun doResolve(): ZigSymbol? = null

	//endregion

	//region Modification
	override fun handleElementRename(newElementName: String): PsiElement {
		return origin.setName(newElementName)
	}

	override fun bindToElement(element: PsiElement): PsiElement {
		referenceTo = element
		return element
	}

	override fun isReferenceTo(element: PsiElement): Boolean {
		return origin.manager.areElementsEquivalent(resolve(), element)
	}
	//endregion

	override fun equals(other: Any?): Boolean {
		return when {
			this === other -> true
			other !is AbstractZigReference -> false
			else -> origin == other.origin
		}
	}

	override fun hashCode(): Int = origin.hashCode()
}

/** A reference to a break label. */
class ZigLabelReference(symbol: ZigSymbol) : AbstractZigReference(symbol) {

	private inline fun <T : Any> walkVisibleLabelDeclarations(check: (ZigSymbol) -> T?):T? {
		// Looking for the label declaration, which will be a direct first child of some of our parents
		var container = origin.parent
		while (container != null) {
			var child = container.firstChild
			childSearch@while (child != null) {
				val type = child.elementType
				if (type != null && !IGNORABLE.contains(type)) {
					if (child is ZigBlockLabel) {
						val result = check(child.labelName)
						if (result != null) {
							return result
						}
					}
					break@childSearch
				}
				child = child.nextSibling
			}
			container = container.parent
		}
		return null
	}

	override fun doResolve(): ZigSymbol? {
		val labelName = origin.name ?: return null
		return walkVisibleLabelDeclarations { label ->
			if (label.textMatches(labelName)) {
				label
			} else null
		}
	}

	override fun getVariants(): Array<PsiElement> {
		val result = ArrayList<PsiElement>()
		walkVisibleLabelDeclarations { label ->
			result.add(label)
			null
		}
		return result.toTypedArray()
	}
}

/** Error reference which is also an error declaration.
 * That happens in code like:
 * ```
 * const FileOpenError = error {
 *      AccessDenied, // <- here
 *  };
 * ```
 * and
 * ```
 * error.AccessDenied
 * ```
 */
class ZigErrorReference(symbol: ZigSymbol) : AbstractZigReference(symbol), PsiPolyVariantReference {

	override fun isSoft(): Boolean = true // Error serves as both declaration and reference, so it is not a problem if it does not resolve to anything else

	override fun resolve(): PsiElement? {
		referenceTo?.let { return it }

		val multiResolve = multiResolve(false)
		var bestBet:PsiElement? = null
		for (result in multiResolve) {
			val element = result.element ?: continue
			if (result.isValidResult) {
				return element
			} else if (bestBet != null) {
				bestBet = element
			}
		}
		return bestBet
	}

	override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {// TODO(jp): Test
		referenceTo = null
		val errorName = origin.name ?: return ResolveResult.EMPTY_ARRAY
		val file = element.containingFile ?: return ResolveResult.EMPTY_ARRAY
		val result = ArrayList<ResolveResult>()
		PsiTreeUtil.processElements(file) { element ->
			if (element is ZigPrimaryErrorReferenceExpr) {
				val symbol = element.referencedErrorName
				if (symbol.textMatches(errorName)) {
					result.add(PsiElementResolveResult(symbol, element.hasNoError))
				}
			} else if (element is ZigErrorSetDecl) {
				for (symbol in element.symbolList) {
					if (symbol.textMatches(errorName)) {
						result.add(PsiElementResolveResult(symbol, symbol.hasNoError))
					}
				}
			}// TODO(jp): When encountering a file import, go there as well?
			false
		}
		return result.toTypedArray()
	}

	override fun isReferenceTo(element: PsiElement): Boolean {
		val errorName = origin.name ?: return false
		if (element !is ZigSymbol) return false
		val parent = element.parent ?: return false
		if (parent !is ZigPrimaryErrorReferenceExpr && parent !is ZigErrorSetDecl) return false
		return element.textMatches(errorName)
	}

	override fun getVariants(): Array<Any> {
		val file = element.containingFile ?: return ArrayUtilRt.EMPTY_OBJECT_ARRAY
		val result = ArrayList<ResolveResult>()
		PsiTreeUtil.processElements(file) { element ->
			if (element is ZigPrimaryErrorReferenceExpr) {
				result.add(PsiElementResolveResult(element, element.hasNoError))
			} else if (element is ZigErrorSetDecl) {
				for (symbol in element.symbolList) {
					result.add(PsiElementResolveResult(symbol, symbol.hasNoError))
				}
			}// TODO(jp): When encountering a file import, go there as well?
			false
		}
		return result.toTypedArray()
	}
}

/**
 * A reference to a value (function, variable, parameter, type, error, etc.).
 */
class ZigExprReference(
		origin: ZigSymbol,
		/** A container whose child is being referenced */
		internal val container: ZigTypeResolvable? = null
) : AbstractZigReference(origin) {

	override fun doResolve(): ZigSymbol? {
		val origin = origin
		val project = origin.project
		if (!origin.isValid || project.isDisposed) return null
		return ResolveCache.getInstance(project).resolveWithCaching(this, ExpressionReferenceResolver, false, false)
	}

	override fun getVariants(): Array<Any> {
		val candidateSet = ArrayList<LookupElementBuilder>(40)
		if (container == null) {
			walkVisibleDeclarations(origin) { declaration ->
				val declName = declaration.name
				val nameIdentifier = declaration.nameIdentifier
				val symbolType = nameIdentifier?.symbolType

				if (declName == null || symbolType == null) {
					return@walkVisibleDeclarations null
				}

				candidateSet.add(LookupElementBuilder
						.create(nameIdentifier)
						.withIcon(symbolType.icon)
						// tail text, it will not be completed by Enter Key press
						.withTailText(if (symbolType == ZigSymbolType.FnDeclaration) "()" else "", true)
						// the type of return value, show at right of popup
						.withTypeText("" /*TODO*/, true))
				null
			}
		} else {
			//TODO
			return ArrayUtilRt.EMPTY_OBJECT_ARRAY
		}

		return candidateSet.toTypedArray()
	}
}

private inline fun <T : Any> walkVisibleDeclarations(from:PsiElement, visit:(ZigDeclaration) -> T?):T? {
	var at = from
	do {
		val parent = at.parent ?: break
		if (parent is ZigContainerMembers) {
			// A top level container, we must walk the whole thing
			var child = parent.firstChild
			childSearch@while (child != null) {
				if (child !== at) {
					(child as? ZigDeclarationHolder)?.declaration()?.let(visit)?.let { return it }
				}
				child = child.nextSibling
			}
		} else {
			// Statement-level container, walk only previous siblings
			while (true) {
				at = at.prevSibling ?: break
				(at as? ZigDeclarationHolder)?.declaration()?.let(visit)?.let { return it }
			}
		}
		at = parent
	} while (true)
	return null
}

private val ExpressionReferenceResolver = ResolveCache.AbstractResolver<ZigExprReference, ZigSymbol> { ref, _ ->
	val container = ref.container
	val name = ref.element.name ?: return@AbstractResolver null
	if (container == null) {
		walkVisibleDeclarations(ref.element) { declaration ->
			val symbol = declaration.nameIdentifier
			if (symbol != null && symbol.textMatches(name)) {
				symbol
			} else null
		}
	} else {
		//TODO
		null
	}
}
