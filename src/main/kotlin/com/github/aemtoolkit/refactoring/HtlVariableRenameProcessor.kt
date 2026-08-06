package com.github.aemtoolkit.refactoring

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlAttribute
import com.intellij.refactoring.rename.RenamePsiElementProcessor

/** Keeps `data-sly-use` attribute names and HTL variable references aligned. */
class HtlVariableRenameProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean =
        element is XmlAttribute && element.name.startsWith(USE_PREFIX)

    override fun substituteElementToRename(
        element: PsiElement,
        editor: Editor?,
    ): PsiElement = element

    override fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<com.intellij.usageView.UsageInfo>,
        listener: com.intellij.refactoring.listeners.RefactoringElementListener?,
    ) {
        val attribute = element as XmlAttribute
        val variable = newName.substringAfter(USE_PREFIX)
        usages.forEach { usage ->
            usage.reference?.handleElementRename(variable)
        }
        attribute.setName("$USE_PREFIX$variable")
        listener?.elementRenamed(attribute)
    }

    private companion object {
        const val USE_PREFIX = "data-sly-use."
    }
}
