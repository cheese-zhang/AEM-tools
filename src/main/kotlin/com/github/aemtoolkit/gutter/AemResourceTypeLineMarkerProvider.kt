package com.github.aemtoolkit.gutter

import com.github.aemtoolkit.resolver.AemResourceTypeTargetResolver
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
        if (AemResourceTypeTargetResolver.getInstance(element.project).resolve(value.value).isEmpty()) {
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
        val resolver = ResourceTypeResolver.getInstance(value.project)
        val component = resolver.resolve(value.value)
        val resourceDirectory = resolver.resolveDirectory(value.value)
        val renderConditions = AemResourceTypeTargetResolver.getInstance(value.project)
            .resolveRenderConditions(value.value)
        val group = DefaultActionGroup().apply {
            if (resourceDirectory != null) {
                add(
                    OpenArtifactAction(
                        if (component == null) "Open Resource" else "Open Component",
                        resourceDirectory,
                        value,
                    ),
                )
            }
            renderConditions.forEach { renderCondition ->
                add(
                    OpenPsiElementAction(
                        "Open ${renderCondition.name}",
                        renderCondition,
                    ),
                )
            }
            if (component != null) {
                add(OpenArtifactAction("Open Dialog", component.dialog, value))
                add(OpenArtifactAction("Open HTL", component.htl, value))
                add(OpenArtifactAction("Open Sling Model", component.slingModel, value))
                add(OpenArtifactAction("Open Clientlib", component.clientlibs.firstOrNull(), value))
            }
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

    private class OpenPsiElementAction(
        text: String,
        private val target: PsiElement,
    ) : AnAction(text) {
        override fun actionPerformed(event: AnActionEvent) {
            target.navigationElement
                .takeIf(PsiElement::isValid)
                ?.let { element ->
                    com.intellij.pom.Navigatable::class.java
                        .takeIf { it.isInstance(element) }
                        ?.cast(element)
                        ?.navigate(true)
                }
        }
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
            if (navigable.isDirectory) {
                com.intellij.ide.projectView.ProjectView.getInstance(context.project)
                    .select(null, navigable, true)
            } else {
                FileEditorManager.getInstance(context.project).openFile(navigable, true)
            }
        }
    }
}
