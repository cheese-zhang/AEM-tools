package com.github.aemtoolkit.reference

import com.github.aemtoolkit.resolver.JcrSchemaService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.xml.XmlAttributeValue

/**
 * Navigates a custom `jcr:primaryType` value to its project CND declaration file.
 */
class JcrTypeReference(element: XmlAttributeValue) :
    PsiReferenceBase<XmlAttributeValue>(
        element,
        TextRange(1, element.textLength - 1),
        true,
    ) {
    override fun resolve(): PsiElement? =
        JcrSchemaService.getInstance(element.project).findSource(element.value)
}
