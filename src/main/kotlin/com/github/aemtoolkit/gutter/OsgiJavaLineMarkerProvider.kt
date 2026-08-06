package com.github.aemtoolkit.gutter

import com.github.aemtoolkit.osgi.OsgiConfigurationService
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiManager

/** Navigates OSGi service classes to all matching run-mode configurations. */
class OsgiJavaLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        val identifier = element as? PsiIdentifier ?: return
        val field = identifier.parent as? PsiField
        if (field != null) {
            collectPropertyMarker(identifier, field, result)
            return
        }
        val type = identifier.parent as? PsiClass ?: return
        val configurations = OsgiConfigurationService.getInstance(element.project)
            .findForClass(type)
        if (configurations.isEmpty()) return
        val manager = PsiManager.getInstance(element.project)
        val targets = configurations.mapNotNull { manager.findFile(it.file) }
        result.add(
            NavigationGutterIconBuilder.create(AllIcons.Nodes.ConfigFolder)
                .setTargets(targets)
                .setTooltipText("Navigate to OSGi configurations")
                .setPopupTitle("OSGi configurations for ${type.name}")
                .createLineMarkerInfo(identifier),
        )
    }

    private fun collectPropertyMarker(
        identifier: PsiIdentifier,
        field: PsiField,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        val annotation = field.modifierList?.annotations?.firstOrNull {
            it.qualifiedName == FELIX_PROPERTY ||
                it.nameReferenceElement?.referenceName == "Property"
        } ?: return
        val propertyName = (annotation.findAttributeValue("name") as? PsiLiteralExpression)
            ?.value as? String ?: field.name
        val type = field.containingClass ?: return
        val targets = OsgiConfigurationService.getInstance(field.project)
            .findPropertyTargets(type, propertyName)
        if (targets.isEmpty()) return
        val values = targets.mapNotNull { it.value }.distinct().joinToString(", ")
        result.add(
            NavigationGutterIconBuilder.create(AllIcons.Nodes.Property)
                .setTargets(targets)
                .setTooltipText("OSGi '$propertyName' configured as $values")
                .setPopupTitle("OSGi values for '$propertyName'")
                .createLineMarkerInfo(identifier),
        )
    }

    private companion object {
        const val FELIX_PROPERTY = "org.apache.felix.scr.annotations.Property"
    }
}
