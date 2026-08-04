package com.github.aemtoolkit.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Validates FileVault `filter.xml` roots and include/exclude patterns.
 */
class AemFilterInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor {
        val seenRoots = mutableSetOf<String>()
        return object : XmlElementVisitor() {
            override fun visitXmlTag(tag: XmlTag) {
                val file = tag.containingFile as? XmlFile ?: return
                if (file.name != "filter.xml") return
                when (tag.localName) {
                    "filter" -> checkFilter(tag, seenRoots, holder)
                    "include", "exclude" -> checkPattern(tag, holder)
                }
            }
        }
    }

    private fun checkFilter(
        tag: XmlTag,
        seenRoots: MutableSet<String>,
        holder: ProblemsHolder,
    ) {
        val root = tag.getAttribute("root")
        val value = root?.value
        if (value.isNullOrBlank()) {
            holder.registerProblem(root ?: tag, "FileVault filter is missing a root path")
        } else {
            if (!value.startsWith('/')) {
                holder.registerProblem(root.valueElement ?: root, "Filter root must be absolute")
            }
            if (!seenRoots.add(value)) {
                holder.registerProblem(root.valueElement ?: root, "Duplicate filter root '$value'")
            }
        }

        val mode = tag.getAttribute("mode") ?: return
        if (mode.value !in setOf("replace", "merge", "update")) {
            holder.registerProblem(
                mode.valueElement ?: mode,
                "Unknown filter mode '${mode.value}'",
            )
        }
    }

    private fun checkPattern(tag: XmlTag, holder: ProblemsHolder) {
        val pattern = tag.getAttribute("pattern") ?: return
        try {
            Pattern.compile(pattern.value.orEmpty())
        } catch (_: PatternSyntaxException) {
            holder.registerProblem(
                pattern.valueElement ?: pattern,
                "Invalid FileVault regular expression",
            )
        }
    }
}
