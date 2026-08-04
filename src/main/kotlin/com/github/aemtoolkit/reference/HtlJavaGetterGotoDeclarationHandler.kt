package com.github.aemtoolkit.reference

import com.github.aemtoolkit.resolver.HtlJavaModelResolver
import com.github.aemtoolkit.resolver.HtlJavaUsageResolver
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

/**
 * Makes Ctrl+Click on a JavaBean getter navigate to its HTL property usages.
 */
class HtlJavaGetterGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor,
    ): Array<PsiElement>? {
        val identifier = sourceElement as? PsiIdentifier ?: return null
        val method = identifier.parent as? PsiMethod ?: return null
        HtlJavaModelResolver.propertyName(method) ?: return null
        return HtlJavaUsageResolver.findUsages(method)
            .takeIf(List<PsiElement>::isNotEmpty)
            ?.toTypedArray()
    }

    override fun getActionText(context: com.intellij.openapi.actionSystem.DataContext): String =
        "Go to HTL Usage"
}
