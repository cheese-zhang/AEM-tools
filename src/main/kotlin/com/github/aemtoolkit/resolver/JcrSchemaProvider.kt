package com.github.aemtoolkit.resolver

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

/**
 * Extension point for JCR vocabulary supplied by CND or external schemas.
 */
interface JcrSchemaProvider {
    /** Returns definitions available to [project]. */
    fun getDefinitions(project: Project): Collection<JcrDefinition>

    /** Returns the source declaring [name], when the provider has one. */
    fun findSource(project: Project, name: String): PsiElement? = null

    companion object {
        val EP_NAME: ExtensionPointName<JcrSchemaProvider> =
            ExtensionPointName.create("com.github.aemtoolkit.jcrSchemaProvider")
    }
}
