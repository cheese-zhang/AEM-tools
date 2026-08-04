package com.github.aemtoolkit.inspection

import com.github.aemtoolkit.util.AemConfigValidator
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

/**
 * Reports structural issues in Dispatcher, CND, and Felix configuration files.
 */
class AemConfigInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor = object : PsiElementVisitor() {
        override fun visitFile(file: PsiFile) {
            AemConfigValidator.validate(file.name, file.text).forEach { message ->
                holder.registerProblem(file, message)
            }
        }
    }
}
