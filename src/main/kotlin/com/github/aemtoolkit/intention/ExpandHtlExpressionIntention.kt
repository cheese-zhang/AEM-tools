package com.github.aemtoolkit.intention

import com.github.aemtoolkit.util.HtlUtil
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute

/**
 * Expands a property shorthand to an explicit `properties` HTL expression.
 */
class ExpandHtlExpressionIntention : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String = "HTL"

    override fun getText(): String = "Qualify property with HTL properties object"

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        element: PsiElement,
    ): Boolean {
        if (!HtlUtil.isHtlFile(element.containingFile)) return false
        val value = PsiTreeUtil.getParentOfType(element, XmlAttribute::class.java, false)
            ?.value
            ?: return false
        return SIMPLE_EXPRESSION.matches(value)
    }

    override fun invoke(
        project: Project,
        editor: Editor?,
        element: PsiElement,
    ) {
        val attribute = PsiTreeUtil.getParentOfType(element, XmlAttribute::class.java, false)
            ?: return
        val property = SIMPLE_EXPRESSION.matchEntire(attribute.value.orEmpty())
            ?.groupValues
            ?.get(1)
            ?: return
        attribute.setValue("\${properties.$property}")
    }

    private companion object {
        val SIMPLE_EXPRESSION = Regex("""\$\{\s*([A-Za-z_]\w*)\s*}""")
    }
}
