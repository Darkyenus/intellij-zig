package org.ziglang.psi

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult
import com.intellij.psi.ResolveState
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.PsiTreeUtil
import icons.ZigIcons
import javax.swing.Icon

/**
 * A base for references to anything.
 */
abstract class AbstractZigReference(protected val origin: ZigSymbol) : PsiReference {

	private val range = TextRange(0, element.textLength)
	protected var referenceTo: PsiElement? = null

	//region Origin info
	override fun getElement(): PsiElement = origin

	override fun getRangeInElement(): TextRange = range

	override fun getCanonicalText(): String = origin.text
	//endregion

	//region Resolution
	override fun isSoft(): Boolean = false
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
		return resolve() == element
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

	override fun resolve(): PsiElement? {
		TODO("not implemented")
	}

	override fun getVariants(): Array<Any> {
		return super.getVariants()
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

	override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
		// TODO(jp): Walk this file and all referenced files to find all error declarations of the same name
		TODO("not implemented")
	}
}

/**
 * A reference to a value (function, variable, parameter, type, error, etc.).
 */
class ZigExprReference (
		origin: ZigSymbol,
		/** A container whose child is being referenced */
		private val container:ZigTypeResolvable? = null
) : AbstractZigReference(origin) {

	override fun resolve(): PsiElement? {
		val origin = origin
		val project = origin.project
		if (!origin.isValid || project.isDisposed) return null
		return ResolveCache.getInstance(project).resolveWithCaching(this, symbolResolver, true, false)
	}

	private companion object ResolverHolder {
		private val symbolResolver = ResolveCache.AbstractResolver<ZigExprReference, PsiElement> { ref, incompleteCode ->
			val processor = SymbolResolveProcessor(ref, incompleteCode)
			val file = ref.element.containingFile ?: return@AbstractResolver null
			treeWalkUp(processor, ref.element, file)
			return@AbstractResolver processor.candidateSet.firstOrNull()?.element
		}
	}

	override fun getVariants(): Array<Any> {
		val variantsProcessor = CompletionProcessor(this, true)
		treeWalkUp(variantsProcessor, element, element.containingFile)
		return variantsProcessor.candidateSet.toTypedArray()
	}
}


abstract class ResolveProcessor<ResolveResult>(private val place: PsiElement) : PsiScopeProcessor {
	abstract val candidateSet: ArrayList<ResolveResult>
	override fun handleEvent(event: PsiScopeProcessor.Event, o: Any?) = Unit
	override fun <T : Any?> getHint(hintKey: Key<T>): T? = null
	protected val PsiElement.hasNoError get() = (this as? StubBasedPsiElement<*>)?.stub != null || !PsiTreeUtil.hasErrorElements(this)
	fun isInScope(element: PsiElement) = when {
		element !is ZigSymbol -> false
		element.isVariableName -> PsiTreeUtil.isAncestor(
				PsiTreeUtil.getParentOfType(element, ZigBlock::class.java)
						?: element.parent.parent?.parent, place, true)
		element.isParameter -> PsiTreeUtil.isAncestor(PsiTreeUtil.getParentOfType(
				element, ZigGlobalFnDeclaration::class.java), place, true)
		element.isFunctionName -> PsiTreeUtil.isAncestor(PsiTreeUtil.getParentOfType(
				element, ZigGlobalFnDeclaration::class.java)?.parent, place, true)
		else -> false
	}
}

class SymbolResolveProcessor(
		@JvmField protected val name: String,
		place: PsiElement,
		private val incompleteCode: Boolean) :
		ResolveProcessor<PsiElementResolveResult>(place) {

	constructor(ref: ZigExprReference, incompleteCode: Boolean) : this(ref.canonicalText, ref.element, incompleteCode)

	override val candidateSet = ArrayList<PsiElementResolveResult>(3)

	protected open fun accessible(element: PsiElement) = name == element.text && isInScope(element)

	override fun execute(element: PsiElement, resolveState: ResolveState): Boolean {
		return when {
			candidateSet.isNotEmpty() -> false
			element is ZigSymbol -> {
				val accessible = accessible(element)
				if (accessible)
					candidateSet += PsiElementResolveResult(element, element.hasNoError)
				!accessible
			}
			else -> true
		}
	}
}

class CompletionProcessor(place: PsiElement, private val incompleteCode: Boolean) :
		ResolveProcessor<LookupElementBuilder>(place) {

	constructor(ref: ZigExprReference, incompleteCode: Boolean) : this(ref.element, incompleteCode)

	override val candidateSet = ArrayList<LookupElementBuilder>(20)

	override fun execute(element: PsiElement, resolveState: ResolveState): Boolean {
		if (element is ZigSymbol) {
			val icon: Icon
			val value: String
			val tail: String
			val type: String

			when {
				element.isFunctionName -> {
					icon = ZigIcons.ZIG_FUN
					value = element.text
					tail = "()"
					type = ""
				}
				element.isVariableName || element.isParameter -> {
					icon = ZigIcons.ZIG_VAR
					value = element.text
					tail = ""
					type = ""
				}
				else -> return true
			}
			if (element.hasNoError && isInScope(element)) {
				candidateSet += LookupElementBuilder
						.create(value)
						.withIcon(icon)
						// tail text, it will not be completed by Enter Key press
						.withTailText(tail, true)
						// the type of return value, show at right of popup
						.withTypeText(type, true)
			}
		}
		return true
	}
}