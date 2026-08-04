package com.github.aemtoolkit.gutter

import com.github.aemtoolkit.resolver.HtlJavaModelResolver
import com.github.aemtoolkit.resolver.HtlJavaUsageResolver
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

/**
 * Adds reverse navigation from Sling Model getters to their HTL usages.
 */
class HtlJavaGetterLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        val identifier = element as? PsiIdentifier ?: return
        val method = identifier.parent as? PsiMethod ?: return
        val property = HtlJavaModelResolver.propertyName(method) ?: return
        val usages = HtlJavaUsageResolver.findUsages(method)
        if (usages.isEmpty()) return

        result.add(
            NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementedMethod)
                .setTargets(usages)
                .setTooltipText("Navigate to HTL usages of '$property'")
                .setPopupTitle("HTL usages of '$property'")
                .createLineMarkerInfo(identifier),
        )
    }
}
