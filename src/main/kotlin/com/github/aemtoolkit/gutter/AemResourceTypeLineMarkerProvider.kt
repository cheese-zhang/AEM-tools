package com.github.aemtoolkit.gutter

import com.github.aemtoolkit.resolver.ResourceTypeResolver
import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupFactory.ActionSelectionAid
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.ui.awt.RelativePoint
import java.awt.event.MouseEvent

/**
 * Adds component artifact actions beside `sling:resourceType`.
 */
class AemResourceTypeLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val value = element as? XmlAttributeValue ?: return null
        val attribute = AemXmlUtil.containingAttribute(value) ?: return null
        if (!AemXmlUtil.isResourceType(attribute)) return null
        if (ResourceTypeResolver.getInstance(element.project).resolve(value.value) == null) {
            return null
        }

        return LineMarkerInfo(
            value,
            value.textRange,
            AllIcons.Nodes.Module,
            { "Navigate to AEM component artifacts" },
            GutterIconNavigationHandler { event, target -> showActions(event, target) },
            GutterIconRenderer.Alignment.LEFT,
            { "AEM component navigation" },
        )
    }

    private fun showActions(event: MouseEvent, value: XmlAttributeValue) {
        val component = ResourceTypeResolver.getInstance(value.project).resolve(value.value)
            ?: return
        val group = DefaultActionGroup().apply {
            add(OpenArtifactAction("Open Component", component.directory, value))
            add(OpenArtifactAction("Open Dialog", component.dialog, value))
            add(OpenArtifactAction("Open HTL", component.htl, value))
            add(OpenArtifactAction("Open Sling Model", component.slingModel, value))
            add(OpenArtifactAction("Open Clientlib", component.clientlibs.firstOrNull(), value))
        }
        val dataContext = DataManager.getInstance().getDataContext(event.component)
        JBPopupFactory.getInstance()
            .createActionGroupPopup(
                "AEM Component",
                group,
                dataContext,
                ActionSelectionAid.SPEEDSEARCH,
                true,
            )
            .show(RelativePoint(event))
    }

    private class OpenArtifactAction(
        text: String,
        private val file: VirtualFile?,
        private val context: PsiElement,
    ) : AnAction(text) {
        override fun update(event: AnActionEvent) {
            event.presentation.isEnabled = file != null
        }

        override fun actionPerformed(event: AnActionEvent) {
            val target = file ?: return
            val navigable = if (target.isDirectory) {
                target.findChild(AemXmlUtil.CONTENT_XML) ?: target
            } else {
                target
            }
            if (!navigable.isDirectory) {
                FileEditorManager.getInstance(context.project).openFile(navigable, true)
            }
        }
    }
}
