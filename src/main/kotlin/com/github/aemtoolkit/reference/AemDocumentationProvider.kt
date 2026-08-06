package com.github.aemtoolkit.reference

import com.github.aemtoolkit.classicui.ClassicUiWidgetRepository
import com.github.aemtoolkit.resolver.AemPlatformResourceType
import com.github.aemtoolkit.resolver.AemResourceTypeTargetResolver
import com.github.aemtoolkit.resolver.JcrSchemaService
import com.github.aemtoolkit.resolver.ResourceTypeResolver
import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlAttribute

/**
 * Supplies hover documentation for resolved AEM resource types.
 */
class AemDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val classicUiDoc = classicUiDocumentation(element, originalElement)
        if (classicUiDoc != null) return classicUiDoc
        val schemaDoc = schemaDocumentation(element, originalElement)
        if (schemaDoc != null) return schemaDoc

        val value = sequenceOf(element, originalElement)
            .filterIsInstance<XmlAttributeValue>()
            .firstOrNull()
            ?: originalElement?.parent as? XmlAttributeValue
            ?: return null
        val attribute = AemXmlUtil.containingAttribute(value) ?: return null
        if (!AemXmlUtil.isResourceType(attribute)) return null

        val component = ResourceTypeResolver.getInstance(value.project).resolve(value.value)
        if (component == null) {
            val renderCondition = AemResourceTypeTargetResolver.getInstance(value.project)
                .resolveRenderConditions(value.value)
                .firstOrNull()
            if (renderCondition != null) {
                return "<div class='definition'><b>${StringUtil.escapeXmlEntities(value.value)}</b></div>" +
                    "<div class='content'><p>Java render condition</p>" +
                    "<p><b>Implementation:</b> " +
                    "${StringUtil.escapeXmlEntities(renderCondition.qualifiedName.orEmpty())}</p></div>"
            }

            val localDirectory = ResourceTypeResolver.getInstance(value.project)
                .resolveDirectory(value.value)
            if (localDirectory != null) {
                return "<div class='definition'><b>${StringUtil.escapeXmlEntities(value.value)}</b></div>" +
                    "<div class='content'><p>AEM script resource</p>" +
                    "<p><b>Local path:</b> " +
                    "${StringUtil.escapeXmlEntities(localDirectory.path)}</p></div>"
            }
            val repositoryPath = AemPlatformResourceType.repositoryPath(value.value) ?: return null
            return "<div class='definition'><b>${StringUtil.escapeXmlEntities(value.value)}</b></div>" +
                "<div class='content'><p>AEM platform component</p>" +
                "<p><b>Repository path:</b> ${StringUtil.escapeXmlEntities(repositoryPath)}</p>" +
                "<p>Navigation becomes available when this `/libs` source is present in the project.</p></div>"
        }
        return buildString {
            append("<div class='definition'><b>")
            append(StringUtil.escapeXmlEntities(component.name))
            append("</b></div><div class='content'>")
            append("<p><b>Component path:</b> ")
            append(StringUtil.escapeXmlEntities(component.componentPath))
            append("</p><table>")
            append(row("Dialog exists", component.dialog != null))
            append(row("HTL exists", component.htl != null))
            append(row("Sling Model exists", component.slingModel != null))
            append("</table></div>")
        }
    }

    private fun classicUiDocumentation(
        element: PsiElement?,
        originalElement: PsiElement?,
    ): String? {
        val context = originalElement ?: element ?: return null
        val attribute = sequenceOf(element, originalElement)
            .filterIsInstance<XmlAttribute>()
            .firstOrNull()
            ?: AemXmlUtil.containingAttribute(context)
            ?: return null
        val xtype = if (attribute.name == "xtype") {
            attribute.value
        } else {
            attribute.parent.getAttributeValue("xtype")
        } ?: return null
        val widget = ClassicUiWidgetRepository.find(xtype) ?: return null
        if (attribute.name == "xtype") {
            return "<div class='definition'><b>${StringUtil.escapeXmlEntities(xtype)}</b> " +
                "<i>Classic UI xtype</i></div><div class='content'>" +
                StringUtil.escapeXmlEntities(widget.description) +
                "</div>"
        }
        val field = widget.fields.firstOrNull { it.name == attribute.name } ?: return null
        return "<div class='definition'><b>${StringUtil.escapeXmlEntities(field.name)}</b> " +
            "<i>${StringUtil.escapeXmlEntities(xtype)}</i></div><div class='content'>" +
            StringUtil.escapeXmlEntities(field.description) +
            "</div>"
    }

    private fun row(label: String, exists: Boolean): String =
        "<tr><td>$label</td><td>${if (exists) "Yes" else "No"}</td></tr>"

    private fun schemaDocumentation(
        element: PsiElement?,
        originalElement: PsiElement?,
    ): String? {
        val context = originalElement ?: element ?: return null
        val attribute = sequenceOf(element, originalElement)
            .filterIsInstance<XmlAttribute>()
            .firstOrNull()
            ?: AemXmlUtil.containingAttribute(context)
            ?: return null
        val lookupName = if (attribute.name == AemXmlUtil.PRIMARY_TYPE) {
            attribute.value
        } else {
            attribute.name
        } ?: return null
        val definition = JcrSchemaService.getInstance(context.project).find(lookupName)
            ?: return null
        return "<div class='definition'><b>${StringUtil.escapeXmlEntities(definition.name)}</b>" +
            " <i>${definition.kind.name.lowercase().replace('_', ' ')}</i></div>" +
            "<div class='content'>${StringUtil.escapeXmlEntities(definition.description)}</div>"
    }
}
