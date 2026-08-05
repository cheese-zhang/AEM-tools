package com.github.aemtoolkit.inspection

import com.github.aemtoolkit.acs.AcsCommonsService
import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

/** Validates supported ACS AEM Commons authoring conventions. */
class AcsCommonsInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor = object : XmlElementVisitor() {
        override fun visitXmlTag(tag: XmlTag) {
            val file = tag.containingFile as? XmlFile ?: return
            if (!AemXmlUtil.isContentXml(file)) return
            val service = AcsCommonsService.getInstance(file.project)
            val filePath = file.virtualFile.path.replace('\\', '/')

            if (tag.getAttributeValue(AemXmlUtil.RESOURCE_TYPE) ==
                AcsCommonsService.GENERIC_LIST_DATASOURCE_RESOURCE_TYPE
            ) {
                val path = tag.getAttribute("path") ?: return
                if (service.findGenericList(path.value.orEmpty()) == null) {
                    holder.registerProblem(
                        path.valueElement ?: path,
                        "Cannot resolve ACS Generic List",
                    )
                }
            }

            if (filePath.contains("/settings/redirects/")) {
                val source = tag.getAttribute("source")
                val target = tag.getAttribute("target")
                if (source == null && target == null) return
                if (source?.value.isNullOrBlank()) {
                    holder.registerProblem(tag, "ACS redirect rule is missing source")
                }
                if (target?.value.isNullOrBlank()) {
                    holder.registerProblem(tag, "ACS redirect rule is missing target")
                }
                tag.getAttribute("statusCode")?.let { status ->
                    val code = status.value.orEmpty().removePrefix("{Long}").toIntOrNull()
                    if (code !in setOf(301, 302, 303, 307, 308)) {
                        holder.registerProblem(
                            status.valueElement ?: status,
                            "Unsupported HTTP redirect status",
                        )
                    }
                }
            }

            if (!file.virtualFile.name.startsWith(AcsCommonsService.NAMED_TRANSFORM_PID)) {
                return
            }
            val transforms = tag.getAttribute("transforms") ?: return
            parseArray(transforms.value.orEmpty()).forEach { transform ->
                val type = transform.substringBefore(':')
                if (type !in TRANSFORM_TYPES) {
                    holder.registerProblem(
                        transforms.valueElement ?: transforms,
                        "Unknown ACS image transform type '$type'",
                    )
                }
            }
        }
    }

    private fun parseArray(value: String): List<String> =
        value.removePrefix("[").removeSuffix("]")
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)

    private companion object {
        val TRANSFORM_TYPES = setOf(
            "resize",
            "bounded-resize",
            "rotate",
            "crop",
            "greyscale",
            "adjust",
            "multiply",
            "rgb-shift",
            "quality",
            "scale",
            "letter-pillar-box",
            "sharpen",
        )
    }
}
