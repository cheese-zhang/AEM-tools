package com.github.aemtoolkit.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

/**
 * Navigates a local `data-sly-call` to its `data-sly-template` declaration.
 */
class HtlTemplateReference(
    element: XmlAttributeValue,
    range: TextRange,
    private val templateName: String,
) : PsiReferenceBase<XmlAttributeValue>(element, range, true) {
    override fun resolve(): PsiElement? {
        val file = element.containingFile as? XmlFile ?: return null
        return file.rootTag?.let(::findTemplate)
    }

    private fun findTemplate(tag: XmlTag): XmlAttribute? {
        tag.getAttribute("data-sly-template.$templateName")?.let { return it }
        return tag.subTags.firstNotNullOfOrNull(::findTemplate)
    }
}
