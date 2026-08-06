package com.github.aemtoolkit.resolver

import com.intellij.ide.highlighter.HtmlFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope

/** Finds HTL templates that can be declared through `data-sly-use`. */
object HtlUseTemplateResolver {
    /** Returns local template paths, followed by the standard AEM ClientLib template. */
    fun candidates(project: Project, source: VirtualFile): List<HtlUseTemplate> {
        val scope = GlobalSearchScope.projectScope(project)
        val sourceDirectory = source.parent ?: return listOf(clientLibraryTemplate())
        val localTemplates = FileTypeIndex.getFiles(HtmlFileType.INSTANCE, scope)
            .asSequence()
            .filter { it != source }
            .mapNotNull { file ->
                val relativePath = VfsUtilCore.getRelativePath(file, sourceDirectory, '/')
                    ?: AemRepositoryPath.fromFilePath(file.path)
                    ?: return@mapNotNull null
                HtlUseTemplate(relativePath, file.path, file)
            }
        return (localTemplates + sequenceOf(clientLibraryTemplate()))
            .distinctBy(HtlUseTemplate::lookupString)
            .sortedBy(HtlUseTemplate::lookupString)
            .toList()
    }

    private fun clientLibraryTemplate() = HtlUseTemplate(
        lookupString = CLIENT_LIBRARY_TEMPLATE,
        location = "AEM platform HTL template",
        file = null,
    )

    const val CLIENT_LIBRARY_TEMPLATE = "/libs/granite/sightly/templates/clientlib.html"
}

/** An HTL template path offered to `data-sly-use` completion. */
data class HtlUseTemplate(
    val lookupString: String,
    val location: String,
    val file: VirtualFile?,
)
