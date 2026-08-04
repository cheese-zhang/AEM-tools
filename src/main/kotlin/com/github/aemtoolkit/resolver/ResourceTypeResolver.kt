package com.github.aemtoolkit.resolver

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
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
    private val resourceDirectories: CachedValue<Map<String, VirtualFile>> =
        CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result.create(
                    loadResourceDirectories(),
                    PsiModificationTracker.MODIFICATION_COUNT,
                )
            },
            false,
        )

    /** Returns the component matching [resourceType]. */
    fun resolve(resourceType: String): AemComponent? =
        allComponents().firstOrNull { it.resourceType == normalize(resourceType) }

    /**
     * Returns the local `/apps` or `/libs` directory represented by [resourceType].
     *
     * Unlike [resolve], this also supports script resources and render conditions
     * that are not declared as `cq:Component`.
     */
    fun resolveDirectory(resourceType: String): VirtualFile? {
        val normalized = normalize(resourceType)
        return resolve(normalized)?.directory ?: resourceDirectories.value[normalized]
    }

    /** Returns every component visible through registered providers. */
    fun allComponents(): List<AemComponent> = components.value

    private fun loadComponents(): List<AemComponent> =
        AemComponentProvider.EP_NAME.extensionList
            .flatMap { it.getComponents(project) }
            .distinctBy(AemComponent::resourceType)
            .sortedBy(AemComponent::resourceType)

    private fun loadResourceDirectories(): Map<String, VirtualFile> {
        val resources = linkedMapOf<String, VirtualFile>()
        ProjectFileIndex.getInstance(project).iterateContent { file ->
            if (file.isDirectory) {
                repositoryResourceType(file)?.let { resourceType ->
                    resources.putIfAbsent(resourceType, file)
                }
            }
            true
        }
        return resources
    }

    private fun repositoryResourceType(directory: VirtualFile): String? {
        val path = directory.path.replace('\\', '/')
        val marker = listOf(
            "/src/main/content/jcr_root/apps/",
            "/src/main/content/jcr_root/libs/",
        ).firstOrNull(path::contains) ?: return null
        return path.substringAfter(marker).trim('/').takeIf(String::isNotEmpty)
    }

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
