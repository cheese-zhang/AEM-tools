package com.github.aemtoolkit.inspection

import com.github.aemtoolkit.util.HtlExpressionValidator
import com.github.aemtoolkit.util.HtlUtil
import com.github.aemtoolkit.resolver.HtlJavaModelResolver
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.xml.XmlAttribute
import com.intellij.openapi.util.TextRange

/**
 * Validates HTL block statements and embedded expressions.
 */
class HtlInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor = object : XmlElementVisitor() {
        override fun visitXmlAttribute(attribute: XmlAttribute) {
            if (!HtlUtil.isHtlFile(attribute.containingFile)) return
            val statement = HtlUtil.statementName(attribute)
            if (attribute.name.startsWith("data-sly-") &&
                statement !in HtlUtil.blockStatements
            ) {
                holder.registerProblem(attribute.nameElement, "Unknown HTL block statement")
                return
            }

            val value = attribute.value.orEmpty()
            if (statement in HtlUtil.blockStatements &&
                statement != "data-sly-unwrap" &&
                value.isBlank()
            ) {
                holder.registerProblem(attribute, "HTL block statement requires a value")
            }
            HtlExpressionValidator.validate(value)?.let { error ->
                holder.registerProblem(attribute.valueElement ?: attribute, error)
            }
            checkJavaModel(attribute, holder)
        }
    }

    private fun checkJavaModel(attribute: XmlAttribute, holder: ProblemsHolder) {
        val valueElement = attribute.valueElement ?: return
        if (attribute.name.startsWith("data-sly-use.")) {
            val variable = attribute.name.substringAfter("data-sly-use.")
            val className = HtlJavaModelResolver.declaredClassName(valueElement, variable)
            if (className != null &&
                HtlJavaModelResolver.resolveModelClass(valueElement, variable) == null
            ) {
                holder.registerProblem(
                    valueElement,
                    "Cannot resolve HTL use-class '$className'",
                )
            }
        }

        MODEL_PROPERTY.findAll(valueElement.value).forEach { match ->
            val variable = match.groupValues[1]
            val property = match.groupValues[2]
            if (!HtlJavaModelResolver.hasDeclaration(valueElement, variable)) return@forEach
            if (HtlJavaModelResolver.resolveProperty(valueElement, variable, property) != null) {
                return@forEach
            }
            val range = match.groups[2]!!.range
            holder.registerProblem(
                valueElement,
                TextRange(range.first + 1, range.last + 2),
                "Cannot resolve '$property' on HTL model '$variable'",
            )
        }
    }

    private companion object {
        val MODEL_PROPERTY = Regex("""\$\{\s*([A-Za-z_]\w*)\.([A-Za-z_]\w*)""")
    }
}
