package com.github.aemtoolkit.resolver

import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile

/**
 * Discovers AEM components through IntelliJ file indexes.
 */
class IndexedComponentProvider : AemComponentProvider {
    override fun getComponents(project: Project): Collection<AemComponent> {
        val scope = GlobalSearchScope.projectScope(project)
        val psiManager = PsiManager.getInstance(project)
        val models = findSlingModels(project, scope)

        return FilenameIndex.getVirtualFilesByName(AemXmlUtil.CONTENT_XML, scope)
            .mapNotNull { contentXml -> toComponent(contentXml, psiManager, models) }
            .distinctBy(AemComponent::resourceType)
            .sortedBy(AemComponent::resourceType)
    }

    private fun toComponent(
        contentXml: VirtualFile,
        psiManager: PsiManager,
        models: Collection<VirtualFile>,
    ): AemComponent? {
        val normalizedPath = contentXml.path.replace('\\', '/')
        val repositoryRoot = repositoryRoot(normalizedPath) ?: return null
        val markerIndex = normalizedPath.indexOf(repositoryRoot.marker)

        val xmlFile = psiManager.findFile(contentXml) as? XmlFile ?: return null
        val primaryType = xmlFile.rootTag?.getAttributeValue(AemXmlUtil.PRIMARY_TYPE)
        if (primaryType != "cq:Component") return null

        val directory = contentXml.parent
        val resourceType = normalizedPath
            .substring(markerIndex + repositoryRoot.marker.length)
            .substringBeforeLast("/${AemXmlUtil.CONTENT_XML}")
            .trim('/')
        if (resourceType.isEmpty()) return null

        return AemComponent(
            name = directory.name,
            resourceType = resourceType,
            componentPath = "/${repositoryRoot.name}/$resourceType",
            directory = directory,
            dialog = findDialog(directory),
            htl = directory.children.firstOrNull { !it.isDirectory && it.extension == "html" },
            slingModel = models.firstOrNull { model ->
                model.inputStream.bufferedReader().use { reader ->
                    val text = reader.readText()
                    "@Model" in text && resourceType in text
                }
            },
            clientlibs = findClientlibs(directory),
        )
    }

    private fun repositoryRoot(path: String): RepositoryRoot? =
        listOf("apps", "libs")
            .map { RepositoryRoot(it, "/src/main/content/jcr_root/$it/") }
            .firstOrNull { path.contains(it.marker) }

    private fun findDialog(directory: VirtualFile): VirtualFile? {
        val dialog = directory.findChild("_cq_dialog") ?: return null
        return if (dialog.isDirectory) {
            dialog.findChild(AemXmlUtil.CONTENT_XML) ?: dialog
        } else {
            dialog
        }
    }

    private fun findSlingModels(
        project: Project,
        scope: GlobalSearchScope,
    ): Collection<VirtualFile> =
        FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope)
            .filter { it.isValid && it.fileSystem.protocol == "file" && project.basePath != null }

    private fun findClientlibs(directory: VirtualFile): List<VirtualFile> =
        directory.children
            .filter { child ->
                child.isDirectory &&
                    (child.name.contains("clientlib", ignoreCase = true) ||
                        child.findChild("js.txt") != null ||
                        child.findChild("css.txt") != null)
            }
            .sortedBy(VirtualFile::getName)

    private data class RepositoryRoot(
        val name: String,
        val marker: String,
    )
}
