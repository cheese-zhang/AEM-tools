package com.github.aemtoolkit.clientlib

import com.github.aemtoolkit.resolver.AemRepositoryPath
import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.xml.XmlFile
import com.intellij.lang.xml.XMLLanguage

/** Indexes project-local AEM client libraries through FileVault XML PSI. */
@Service(Service.Level.PROJECT)
class AemClientLibraryService(private val project: Project) {
    private val index: CachedValue<List<AemClientLibrary>> =
        CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result.create(
                    loadClientLibraries(),
                    PsiModificationTracker.getInstance(project).forLanguage(XMLLanguage.INSTANCE),
                )
            },
            false,
        )

    /** Returns all client libraries in the current project. */
    fun all(): List<AemClientLibrary> = index.value

    /** Returns all declared categories in stable alphabetical order. */
    fun categories(): List<String> =
        all().flatMap(AemClientLibrary::categories).distinct().sorted()

    /** Finds every client library declaring [category]. */
    fun findByCategory(category: String): List<AemClientLibrary> =
        all().filter { category in it.categories }

    /** Finds the client library represented by [file]. */
    fun findByFile(file: com.intellij.openapi.vfs.VirtualFile): AemClientLibrary? =
        all().firstOrNull { it.contentXml == file || it.directory == file }

    private fun loadClientLibraries(): List<AemClientLibrary> {
        val scope = GlobalSearchScope.projectScope(project)
        val manager = PsiManager.getInstance(project)
        return FilenameIndex.getVirtualFilesByName(AemXmlUtil.CONTENT_XML, scope)
            .mapNotNull { file ->
                val xml = manager.findFile(file) as? XmlFile ?: return@mapNotNull null
                val root = xml.rootTag ?: return@mapNotNull null
                if (root.getAttributeValue(AemXmlUtil.PRIMARY_TYPE) != CLIENTLIB_TYPE) {
                    return@mapNotNull null
                }
                val repositoryPath = AemRepositoryPath.fromFilePath(file.path)
                    ?.removeSuffix("/${AemXmlUtil.CONTENT_XML}")
                    ?: return@mapNotNull null
                AemClientLibrary(
                    repositoryPath = repositoryPath,
                    directory = file.parent,
                    contentXml = file,
                    categories = parseValues(root.getAttributeValue(CATEGORIES)),
                    dependencies = parseValues(root.getAttributeValue(DEPENDENCIES)),
                    embeds = parseValues(root.getAttributeValue(EMBED)),
                )
            }
            .sortedBy(AemClientLibrary::repositoryPath)
    }

    companion object {
        const val CLIENTLIB_TYPE = "cq:ClientLibraryFolder"
        const val CATEGORIES = "categories"
        const val DEPENDENCIES = "dependencies"
        const val EMBED = "embed"

        /** Parses FileVault scalar and array property syntax. */
        fun parseValues(rawValue: String?): List<String> {
            val value = rawValue.orEmpty()
                .replaceFirst(Regex("""^\{[^}]+}"""), "")
                .trim()
            val content = value.removePrefix("[").removeSuffix("]")
            return content.split(',')
                .map { it.trim().trim('"', '\'') }
                .filter(String::isNotEmpty)
        }

        fun getInstance(project: Project): AemClientLibraryService = project.service()
    }
}
