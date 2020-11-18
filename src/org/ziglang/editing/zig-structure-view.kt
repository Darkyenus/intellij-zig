package org.ziglang.editing

import com.intellij.ide.structureView.*
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.pom.Navigatable
import com.intellij.psi.*
import icons.ZigIcons
import org.ziglang.ZigFile
import org.ziglang.psi.*

class ZigStructureViewModel(root: PsiFile, editor: Editor?) :
		StructureViewModelBase(root, editor, ZigStructureViewElement(root)),
		StructureViewModel.ElementInfoProvider {
	init {
		withSuitableClasses(
				ZigGlobalFnDeclaration::class.java,
				ZigGlobalFnPrototype::class.java,
				ZigGlobalVarDeclaration::class.java,
				ZigGlobalUsingNamespace::class.java,
				ZigTestDecl::class.java,
				ZigTopLevelComptime::class.java
		)
	}

	override fun shouldEnterElement(element: Any?) = true
	override fun isAlwaysShowsPlus(element: StructureViewTreeElement?) = false
	override fun isAlwaysLeaf(element: StructureViewTreeElement?) = element is ZigGlobalFnDeclaration
}

class ZigStructureViewElement(private val root: NavigatablePsiElement) :
		StructureViewTreeElement, ItemPresentation, SortableTreeElement, Navigatable by root {
	override fun getLocationString() = ""
	override fun getIcon(open: Boolean) = when (root) {
		is ZigFile -> ZigIcons.ZIG_FILE
		is ZigGlobalFnDeclaration -> ZigIcons.ZIG_FUN
		is ZigGlobalVarDeclaration -> ZigIcons.ZIG_VAR
		else -> ZigIcons.ZIG_BIG_ICON
	}

	// Return the displayed string, 60 should be the upper limit of characters
	override fun getPresentableText() = cutText(root.presentText(), 60)

	override fun getPresentation() = this
	override fun getValue() = root
	override fun getAlphaSortKey() = (root as? PsiNamedElement)?.name.orEmpty()
	override fun getChildren() = root
			.children
			.filter { it.treeViewTokens }
			.map { ZigStructureViewElement(it as NavigatablePsiElement) }
			.toTypedArray()
}

class ZigStructureViewFactory : PsiStructureViewFactory {
	override fun getStructureViewBuilder(file: PsiFile) = object : TreeBasedStructureViewBuilder() {
		override fun isRootNodeShown() = true
		override fun createStructureViewModel(editor: Editor?) = ZigStructureViewModel(file, editor)
	}
}