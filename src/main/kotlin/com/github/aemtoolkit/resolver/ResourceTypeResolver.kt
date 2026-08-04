package com.github.aemtoolkit.resolver

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker

/**
 * Resolves AEM resource types using registered [AemComponentProvider] extensions.
 */
@Service(Service.Level.PROJECT)
class ResourceTypeResolver(private val project: Project) {
    private val components: CachedValue<List<AemComponent>> =
        CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result.create(
                    loadComponents(),
                    PsiModificationTracker.MODIFICATION_COUNT,
                )
            },
            false,
        )

    /** Returns the component matching [resourceType]. */
    fun resolve(resourceType: String): AemComponent? =
        allComponents().firstOrNull { it.resourceType == normalize(resourceType) }

    /** Returns every component visible through registered providers. */
    fun allComponents(): List<AemComponent> = components.value

    private fun loadComponents(): List<AemComponent> =
        AemComponentProvider.EP_NAME.extensionList
            .flatMap { it.getComponents(project) }
            .distinctBy(AemComponent::resourceType)
            .sortedBy(AemComponent::resourceType)

    private fun normalize(resourceType: String): String =
        resourceType
            .removePrefix("/apps/")
            .removePrefix("/libs/")
            .trim('/')

    companion object {
        /** Returns the project-level resolver service. */
        fun getInstance(project: Project): ResourceTypeResolver = project.service()
    }
}
