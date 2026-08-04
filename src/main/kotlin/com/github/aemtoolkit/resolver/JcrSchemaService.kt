package com.github.aemtoolkit.resolver

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker

/**
 * Aggregates JCR vocabulary from registered schema providers.
 */
@Service(Service.Level.PROJECT)
class JcrSchemaService(private val project: Project) {
    private val definitions: CachedValue<List<JcrDefinition>> =
        CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result.create(
                    loadDefinitions(),
                    PsiModificationTracker.MODIFICATION_COUNT,
                )
            },
            false,
        )

    /** Returns all unique definitions, ordered by name. */
    fun allDefinitions(): List<JcrDefinition> = definitions.value

    private fun loadDefinitions(): List<JcrDefinition> =
        JcrSchemaProvider.EP_NAME.extensionList
            .flatMap { it.getDefinitions(project) }
            .distinctBy { it.kind to it.name }
            .sortedBy(JcrDefinition::name)

    /** Finds a node type or property definition by exact name. */
    fun find(name: String): JcrDefinition? =
        allDefinitions().firstOrNull { it.name == name }

    /** Returns the project source declaring [name], when available. */
    fun findSource(name: String): PsiElement? =
        JcrSchemaProvider.EP_NAME.extensionList
            .firstNotNullOfOrNull { it.findSource(project, name) }

    companion object {
        /** Returns the project-level schema service. */
        fun getInstance(project: Project): JcrSchemaService = project.service()
    }
}
