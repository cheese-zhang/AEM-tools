package com.github.aemtoolkit.inspection

import com.github.aemtoolkit.resolver.AemRepositoryPath
import com.github.aemtoolkit.resolver.ResourceTypeResolver
import com.github.aemtoolkit.util.AemStyleIdValidator
import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

/**
 * Checks required AEM content properties and component-owned artifacts.
 */
class AemContentInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor = object : XmlElementVisitor() {
        override fun visitXmlTag(tag: XmlTag) {
            val file = tag.containingFile as? XmlFile ?: return
            if (!AemXmlUtil.isContentXml(file)) return

            checkStyleIds(tag, holder)
            if (tag != file.rootTag) return

            when (tag.getAttributeValue(AemXmlUtil.PRIMARY_TYPE)) {
                "cq:PageContent" -> checkPageContent(tag, holder)
                "cq:Component" -> checkComponent(tag, file, holder)
            }
        }
    }

    private fun checkPageContent(tag: XmlTag, holder: ProblemsHolder) {
        if (tag.getAttribute(AemXmlUtil.RESOURCE_TYPE) == null) {
            holder.registerProblem(
                tag,
                "AEM page content is missing sling:resourceType",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }
        if (tag.getAttribute(AemXmlUtil.TEMPLATE) == null) {
            holder.registerProblem(
                tag,
                "AEM page content is missing cq:template",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }
    }

    private fun checkStyleIds(tag: XmlTag, holder: ProblemsHolder) {
        val attribute = tag.getAttribute(AemXmlUtil.STYLE_IDS) ?: return
        val invalid = AemStyleIdValidator.invalidIds(attribute.value.orEmpty())
        if (invalid.isNotEmpty()) {
            holder.registerProblem(
                attribute.valueElement ?: attribute,
                "Invalid AEM Style ID: ${invalid.joinToString()}",
            )
        }
    }

    private fun checkComponent(
        tag: XmlTag,
        file: XmlFile,
        holder: ProblemsHolder,
    ) {
        val repositoryPath = AemRepositoryPath.fromFilePath(file.virtualFile.path)
            ?.removeSuffix("/${AemXmlUtil.CONTENT_XML}")
            ?.removePrefix("/apps/")
            ?: return
        val component = ResourceTypeResolver.getInstance(file.project).resolve(repositoryPath)
            ?: return

        if (component.dialog == null) {
            holder.registerProblem(tag, "AEM component is missing _cq_dialog")
        }
        if (component.htl == null) {
            holder.registerProblem(tag, "AEM component is missing an HTL file")
        }
        if (component.clientlibs.isEmpty()) {
            holder.registerProblem(
                tag,
                "AEM component is missing a client library",
                ProblemHighlightType.WEAK_WARNING,
            )
        }
    }
}
