package com.github.aemtoolkit.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField

/**
 * Validates common Sling Models annotation mistakes without requiring AEM dependencies.
 */
class SlingModelInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor = object : JavaElementVisitor() {
        override fun visitClass(aClass: PsiClass) {
            val model = aClass.getAnnotation(MODEL) ?: return
            if (model.findDeclaredAttributeValue("adaptables") == null) {
                holder.registerProblem(model, "Sling Model must declare at least one adaptable")
            }
            checkResourceTypeAdaptable(model, holder)
        }

        override fun visitField(field: PsiField) {
            val injectionAnnotations = field.annotations.filter { annotation ->
                annotation.qualifiedName in INJECTION_ANNOTATIONS
            }
            if (injectionAnnotations.size > 1) {
                holder.registerProblem(
                    field.nameIdentifier ?: field,
                    "Field has multiple Sling Models injector annotations",
                )
            }
            field.getAnnotation(GENERIC_INJECT)?.let { annotation ->
                holder.registerProblem(
                    annotation,
                    "Prefer a source-specific Sling Models injector annotation",
                    ProblemHighlightType.WEAK_WARNING,
                )
            }
            if (injectionAnnotations.isNotEmpty() && field.hasModifierProperty("final")) {
                holder.registerProblem(
                    field.nameIdentifier ?: field,
                    "Injected Sling Model field cannot be final",
                )
            }
        }
    }

    private fun checkResourceTypeAdaptable(
        model: PsiAnnotation,
        holder: ProblemsHolder,
    ) {
        if (model.findDeclaredAttributeValue("resourceType") == null) return
        val adaptables = model.findDeclaredAttributeValue("adaptables")?.text.orEmpty()
        if ("Resource" !in adaptables && "SlingHttpServletRequest" !in adaptables) {
            holder.registerProblem(
                model,
                "A resourceType model should adapt from Resource or SlingHttpServletRequest",
            )
        }
    }

    private companion object {
        const val MODEL = "org.apache.sling.models.annotations.Model"
        const val GENERIC_INJECT = "javax.inject.Inject"
        val INJECTION_ANNOTATIONS = setOf(
            GENERIC_INJECT,
            "jakarta.inject.Inject",
            "org.apache.sling.models.annotations.injectorspecific.ValueMapValue",
            "org.apache.sling.models.annotations.injectorspecific.ChildResource",
            "org.apache.sling.models.annotations.injectorspecific.Self",
            "org.apache.sling.models.annotations.injectorspecific.SlingObject",
            "org.apache.sling.models.annotations.injectorspecific.ScriptVariable",
            "org.apache.sling.models.annotations.injectorspecific.OSGiService",
            "org.apache.sling.models.annotations.injectorspecific.RequestAttribute",
        )
    }
}
