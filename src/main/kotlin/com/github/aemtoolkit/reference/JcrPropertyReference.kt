package com.github.aemtoolkit.reference

import com.github.aemtoolkit.resolver.JcrSchemaService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.xml.XmlAttribute

/**
 * Navigates a project-defined JCR property to its CND source.
 */
class JcrPropertyReference(element: XmlAttribute) :
    PsiReferenceBase<XmlAttribute>(
        element,
        TextRange(0, element.name.length),
        true,
    ) {
    override fun resolve(): PsiElement? =
        JcrSchemaService.getInstance(element.project).findSource(element.name)
}
