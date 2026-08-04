package com.github.aemtoolkit.reference

import com.github.aemtoolkit.util.HtlUtil
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute

/**
 * Supplies quick documentation for HTL block statements.
 */
class HtlDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val context = originalElement ?: element ?: return null
        if (!HtlUtil.isHtlFile(context.containingFile)) return null
        val attribute = sequenceOf(element, originalElement)
            .filterIsInstance<XmlAttribute>()
            .firstOrNull()
            ?: PsiTreeUtil.getParentOfType(context, XmlAttribute::class.java, false)
            ?: return null
        val statement = HtlUtil.statementName(attribute)
        val documentation = HtlUtil.blockStatements[statement] ?: return null
        return "<div class='definition'><b>${StringUtil.escapeXmlEntities(statement)}</b></div>" +
            "<div class='content'>${StringUtil.escapeXmlEntities(documentation)}</div>"
    }
}
