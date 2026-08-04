package com.github.aemtoolkit.inspection

import com.github.aemtoolkit.resolver.AemPlatformResourceType
import com.github.aemtoolkit.resolver.ResourceTypeResolver
import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.xml.XmlAttribute

/**
 * Reports `sling:resourceType` values that do not resolve to a component.
 */
class UnresolvedResourceTypeInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor = object : XmlElementVisitor() {
        override fun visitXmlAttribute(attribute: XmlAttribute) {
            if (!AemXmlUtil.isResourceType(attribute)) return
            val value = attribute.value ?: return
            if (AemPlatformResourceType.isExternal(value)) return
            if (ResourceTypeResolver.getInstance(attribute.project).resolve(value) == null) {
                holder.registerProblem(
                    attribute.valueElement ?: attribute,
                    "Cannot resolve AEM component '$value'",
                )
            }
        }
    }
}
