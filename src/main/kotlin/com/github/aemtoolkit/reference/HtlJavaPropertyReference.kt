package com.github.aemtoolkit.reference

import com.github.aemtoolkit.resolver.HtlJavaModelResolver
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReferenceBase

/**
 * Navigates an HTL model property to its Java getter.
 */
class HtlJavaPropertyReference(
    element: PsiElement,
    range: TextRange,
    private val variable: String,
    private val property: String,
    private val propertyChain: List<String> = emptyList(),
) : PsiReferenceBase<PsiElement>(element, range, true) {
    override fun resolve(): PsiElement? =
        HtlJavaModelResolver.resolveProperty(
            element,
            variable,
            property,
            propertyChain,
        )

    override fun handleElementRename(newElementName: String): PsiElement {
        val replacement = when {
            resolve() !is PsiMethod -> newElementName
            newElementName.startsWith("get") && newElementName.length > 3 ->
                java.beans.Introspector.decapitalize(newElementName.substring(3))
            newElementName.startsWith("is") && newElementName.length > 2 ->
                java.beans.Introspector.decapitalize(newElementName.substring(2))
            else -> newElementName
        }
        return ElementManipulators.handleContentChange(element, rangeInElement, replacement)
    }
}
