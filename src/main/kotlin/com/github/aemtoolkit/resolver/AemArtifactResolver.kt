package com.github.aemtoolkit.resolver

import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker

/**
 * Resolves repository paths and files related to AEM components.
 */
@Service(Service.Level.PROJECT)
class AemArtifactResolver(private val project: Project) {
    private val artifacts: CachedValue<List<AemRepositoryArtifact>> =
        CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result.create(
                    loadRepositoryArtifacts(),
                    PsiModificationTracker.MODIFICATION_COUNT,
                )
            },
            false,
        )

    /** Resolves an absolute JCR path to its source file or directory. */
    fun resolveRepositoryPath(path: String): VirtualFile? {
        val expected = AemRepositoryPath.normalize(path)
        val scope = GlobalSearchScope.projectScope(project)
        return FilenameIndex.getVirtualFilesByName(AemXmlUtil.CONTENT_XML, scope)
            .firstOrNull { contentXml ->
                val repositoryPath = AemRepositoryPath.fromFilePath(contentXml.path)
                    ?.removeSuffix("/${AemXmlUtil.CONTENT_XML}")
                repositoryPath == expected
            }
            ?.parent
    }

    /** Returns navigable artifacts owned by [resourceType]. */
    fun relatedArtifacts(resourceType: String): List<VirtualFile> {
        val component = ResourceTypeResolver.getInstance(project).resolve(resourceType)
            ?: return emptyList()
        return buildList {
            component.dialog?.let(::add)
            component.htl?.let(::add)
            component.slingModel?.let(::add)
            addAll(component.clientlibs)
        }.distinctBy(VirtualFile::getPath)
    }

    /** Returns repository artifacts grouped by their AEM role. */
    fun repositoryArtifacts(): List<AemRepositoryArtifact> = artifacts.value

    private fun loadRepositoryArtifacts(): List<AemRepositoryArtifact> {
        val scope = GlobalSearchScope.projectScope(project)
        val psiManager = PsiManager.getInstance(project)
        return FilenameIndex.getVirtualFilesByName(AemXmlUtil.CONTENT_XML, scope)
            .mapNotNull { contentXml ->
                val path = AemRepositoryPath.fromFilePath(contentXml.path)
                    ?.removeSuffix("/${AemXmlUtil.CONTENT_XML}")
                    ?: return@mapNotNull null
                val primaryType = (psiManager.findFile(contentXml) as? XmlFile)
                    ?.rootTag
                    ?.getAttributeValue(AemXmlUtil.PRIMARY_TYPE)
                val kind = when {
                    "/settings/wcm/templates/" in path -> AemRepositoryArtifactKind.TEMPLATE
                    "/settings/wcm/policies/" in path -> AemRepositoryArtifactKind.POLICY
                    primaryType == "cq:ClientLibraryFolder" -> AemRepositoryArtifactKind.CLIENTLIB
                    else -> return@mapNotNull null
                }
                AemRepositoryArtifact(
                    name = contentXml.parent.name,
                    repositoryPath = path,
                    directory = contentXml.parent,
                    kind = kind,
                )
            }
            .distinctBy { it.kind to it.repositoryPath }
            .sortedBy(AemRepositoryArtifact::repositoryPath)
    }

    companion object {
        /** Returns the project-level artifact resolver. */
        fun getInstance(project: Project): AemArtifactResolver = project.service()
    }
}
