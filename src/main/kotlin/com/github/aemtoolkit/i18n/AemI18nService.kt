package com.github.aemtoolkit.i18n

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
import com.intellij.psi.xml.XmlTag
import com.intellij.lang.xml.XMLLanguage

/** Indexes AEM `sling:key` and `sling:message` translation nodes. */
@Service(Service.Level.PROJECT)
class AemI18nService(private val project: Project) {
    private val index: CachedValue<I18nIndex> =
        CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result.create(
                    loadEntries().let { entries ->
                        I18nIndex(entries, entries.groupBy(AemI18nEntry::key))
                    },
                    PsiModificationTracker.getInstance(project).forLanguage(XMLLanguage.INSTANCE),
                )
            },
            false,
        )

    fun all(): List<AemI18nEntry> = index.value.entries

    fun keys(): List<String> = index.value.byKey.keys.sorted()

    fun find(key: String): List<AemI18nEntry> = index.value.byKey[key].orEmpty()

    private fun loadEntries(): List<AemI18nEntry> {
        val scope = GlobalSearchScope.projectScope(project)
        val manager = PsiManager.getInstance(project)
        return FilenameIndex.getVirtualFilesByName(AemXmlUtil.CONTENT_XML, scope)
            .flatMap { file ->
                val xml = manager.findFile(file) as? XmlFile ?: return@flatMap emptyList()
                val language = xml.rootTag?.getAttributeValue(LANGUAGE)
                xml.rootTag?.let(::descendants).orEmpty().mapNotNull { tag ->
                    val key = tag.getAttributeValue(KEY) ?: return@mapNotNull null
                    val message = tag.getAttributeValue(MESSAGE) ?: return@mapNotNull null
                    AemI18nEntry(key, message, language, file)
                }.toList()
            }
            .distinctBy { Triple(it.key, it.language, it.file.path) }
            .sortedWith(compareBy(AemI18nEntry::key, AemI18nEntry::language))
    }

    private fun descendants(root: XmlTag): Sequence<XmlTag> =
        sequenceOf(root) + root.subTags.asSequence().flatMap(::descendants)

    private data class I18nIndex(
        val entries: List<AemI18nEntry>,
        val byKey: Map<String, List<AemI18nEntry>>,
    )

    companion object {
        const val KEY = "sling:key"
        const val MESSAGE = "sling:message"
        const val LANGUAGE = "jcr:language"

        fun getInstance(project: Project): AemI18nService = project.service()
    }
}
