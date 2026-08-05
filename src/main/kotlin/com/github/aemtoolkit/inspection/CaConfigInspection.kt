package com.github.aemtoolkit.inspection

import com.github.aemtoolkit.caconfig.CaConfigProperty
import com.github.aemtoolkit.caconfig.CaConfigService
import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

/** Validates project-local Sling Context-Aware Configuration resources. */
class CaConfigInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor = object : XmlElementVisitor() {
        override fun visitXmlTag(tag: XmlTag) {
            val file = tag.containingFile as? XmlFile ?: return
            if (!AemXmlUtil.isContentXml(file) || tag != file.rootTag) return

            val service = CaConfigService.getInstance(file.project)
            tag.getAttribute(CaConfigService.CONFIG_REF)?.let { attribute ->
                val root = attribute.value.orEmpty()
                if (service.resources().none { it.contextPath == root }) {
                    holder.registerProblem(
                        attribute.valueElement ?: attribute,
                        "No CAConfig resources were found below $root",
                        ProblemHighlightType.WEAK_WARNING,
                    )
                }
            }

            val resource = service.resources().firstOrNull { it.file == file.virtualFile }
                ?: return
            val definition = service.findDefinition(resource.configName) ?: return
            tag.attributes
                .filterNot { it.isNamespaceDeclaration || isTechnicalProperty(it.name) }
                .forEach { attribute ->
                    val property = definition.properties.firstOrNull { it.name == attribute.name }
                    if (property == null) {
                        holder.registerProblem(
                            attribute,
                            "Unknown property '${attribute.name}' for ${definition.name}",
                        )
                    } else if (!hasCompatibleType(attribute.value.orEmpty(), property)) {
                        holder.registerProblem(
                            attribute.valueElement ?: attribute,
                            "Value type does not match ${property.type}",
                        )
                    }
                }
        }
    }

    private fun isTechnicalProperty(name: String): Boolean =
        name.startsWith("jcr:") ||
            name.startsWith("sling:") ||
            name.startsWith("cq:")

    private fun hasCompatibleType(value: String, property: CaConfigProperty): Boolean {
        val declaredType = value.substringAfter('{', "").substringBefore('}', "")
        if (declaredType.isEmpty()) {
            return property.type == "java.lang.String" ||
                property.type == "String" ||
                property.type.endsWith("[]")
        }
        return when (declaredType.removeSuffix("[]")) {
            "Boolean" -> property.type == "boolean" ||
                property.type == "java.lang.Boolean" ||
                property.type == "boolean[]"
            "Long" -> property.type in setOf(
                "long",
                "java.lang.Long",
                "int",
                "java.lang.Integer",
                "long[]",
                "int[]",
            )
            "Double", "Decimal" -> property.type in setOf(
                "double",
                "java.lang.Double",
                "float",
                "java.lang.Float",
                "double[]",
                "float[]",
            )
            else -> true
        }
    }
}
