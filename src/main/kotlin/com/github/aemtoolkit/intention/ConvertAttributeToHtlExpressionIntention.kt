package com.github.aemtoolkit.intention

import com.github.aemtoolkit.util.HtlUtil
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute

/**
 * Wraps a literal HTML attribute value in an escaped HTL expression.
 */
class ConvertAttributeToHtlExpressionIntention : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String = "HTL"

    override fun getText(): String = "Convert HTML attribute to HTL expression"

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        element: PsiElement,
    ): Boolean {
        if (!HtlUtil.isHtlFile(element.containingFile)) return false
        val attribute = PsiTreeUtil.getParentOfType(element, XmlAttribute::class.java, false)
            ?: return false
        val value = attribute.value ?: return false
        return !attribute.name.startsWith("data-sly-") &&
            value.isNotBlank() &&
            "\${" !in value
    }

    override fun invoke(
        project: Project,
        editor: Editor?,
        element: PsiElement,
    ) {
        val attribute = PsiTreeUtil.getParentOfType(element, XmlAttribute::class.java, false)
            ?: return
        val literal = attribute.value
            ?.replace("\\", "\\\\")
            ?.replace("'", "\\'")
            ?: return
        attribute.setValue("\${'$literal'}")
    }
}
