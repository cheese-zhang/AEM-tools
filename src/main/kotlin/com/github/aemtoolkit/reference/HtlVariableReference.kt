package com.github.aemtoolkit.reference

import com.github.aemtoolkit.resolver.HtlJavaModelResolver
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

/** Resolves and renames an HTL expression variable declared by `data-sly-use`. */
class HtlVariableReference(
    element: PsiElement,
    range: TextRange,
    private val variable: String,
) : PsiReferenceBase<PsiElement>(element, range, false) {
    override fun resolve(): PsiElement? =
        HtlJavaModelResolver.findDeclaration(element, variable)

    override fun handleElementRename(newElementName: String): PsiElement =
        ElementManipulators.handleContentChange(
            element,
            rangeInElement,
            newElementName.substringAfter("data-sly-use."),
        )
}
