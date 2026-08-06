package com.github.aemtoolkit.gutter

import com.github.aemtoolkit.osgi.OsgiConfigurationService
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlTokenType

/** Navigates serialized OSGi configurations back to their Java service. */
class OsgiXmlLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        if (element.node.elementType != XmlTokenType.XML_NAME) return
        val tag = element.parent as? XmlTag ?: return
        if (tag.parentTag != null) return
        val service = OsgiConfigurationService.getInstance(element.project)
        val configuration = service.findByFile(element.containingFile.virtualFile) ?: return
        val targets = service.findClasses(configuration)
        if (targets.isEmpty()) return
        result.add(
            NavigationGutterIconBuilder.create(AllIcons.Nodes.Class)
                .setTargets(targets)
                .setTooltipText("Navigate to OSGi service '${configuration.pid}'")
                .setPopupTitle("OSGi service for ${configuration.file.name}")
                .createLineMarkerInfo(element),
        )
    }
}
