package com.github.aemtoolkit.reference

import com.github.aemtoolkit.resolver.HtlJavaModelResolver
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

/**
 * Navigates an HTL model property to its Java getter.
 */
class HtlJavaPropertyReference(
    element: PsiElement,
    range: TextRange,
    private val variable: String,
    private val property: String,
) : PsiReferenceBase<PsiElement>(element, range, true) {
    override fun resolve(): PsiElement? =
        HtlJavaModelResolver.resolveProperty(element, variable, property)
}
