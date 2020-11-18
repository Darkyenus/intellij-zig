package org.ziglang.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import org.ziglang.ZigTokenType

class ZigStringManipulator : AbstractElementManipulator<ZigStringLiteral>() {
	override fun handleContentChange(psi: ZigStringLiteral, range: TextRange, new: String): ZigStringLiteral {
		val after = ZigTokenType.fromText(new, psi.project) as? ZigStringLiteral ?: return psi
		psi.replace(after)
		return after
	}
}
