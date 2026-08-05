package com.github.aemtoolkit.acs

import com.github.aemtoolkit.resolver.AemRepositoryPath
import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

/** Indexes supported ACS AEM Commons repository and OSGi conventions. */
@Service(Service.Level.PROJECT)
class AcsCommonsService(private val project: Project) {
    private val index: CachedValue<AcsCommonsIndex> =
        CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result.create(
                    buildIndex(),
                    PsiModificationTracker.MODIFICATION_COUNT,
                )
            },
            false,
        )

    fun genericLists(): List<AcsGenericList> = index.value.genericLists

    fun namedImageTransforms(): List<AcsNamedImageTransform> =
        index.value.namedImageTransforms

    fun redirectRules(): List<AcsRedirectRule> = index.value.redirectRules

    fun sharedProperties(): List<AcsSharedProperties> = index.value.sharedProperties

    fun findGenericList(path: String): AcsGenericList? {
        val normalized = path
            .substringBefore(".list.json")
            .replace("/_jcr_content", "")
            .trimEnd('/')
        return genericLists().firstOrNull {
            normalized == it.repositoryPath || normalized.startsWith("${it.repositoryPath}/")
        }
    }

    fun findNamedImageTransform(name: String): AcsNamedImageTransform? =
        namedImageTransforms().firstOrNull { it.name == name }

    private fun buildIndex(): AcsCommonsIndex {
        val scope = GlobalSearchScope.projectScope(project)
        val manager = PsiManager.getInstance(project)
        val contentFiles = FilenameIndex.getVirtualFilesByName(AemXmlUtil.CONTENT_XML, scope)
            .mapNotNull { file ->
                val xml = manager.findFile(file) as? XmlFile ?: return@mapNotNull null
                val path = AemRepositoryPath.fromFilePath(file.path)
                    ?.removeSuffix("/${AemXmlUtil.CONTENT_XML}")
                    ?: return@mapNotNull null
                Triple(file, path, xml)
            }
        return AcsCommonsIndex(
            genericLists = contentFiles.mapNotNull { (file, path, xml) ->
                toGenericList(file, path, xml)
            }.distinctBy(AcsGenericList::repositoryPath),
            namedImageTransforms = loadNamedImageTransforms(scope, manager),
            redirectRules = contentFiles.flatMap { (file, path, xml) ->
                toRedirectRules(file, path, xml)
            },
            sharedProperties = contentFiles.mapNotNull { (file, path, _) ->
                toSharedProperties(file, path)
            },
        )
    }

    private fun toGenericList(
        file: VirtualFile,
        path: String,
        xml: XmlFile,
    ): AcsGenericList? {
        val marker = "$GENERIC_LIST_ROOT/"
        val markerIndex = path.indexOf(marker)
        if (markerIndex < 0) return null
        val name = path.substring(markerIndex + marker.length).substringBefore('/')
        val repositoryPath = "$GENERIC_LIST_ROOT/$name"
        val items = xml.rootTag?.let(::descendants).orEmpty()
            .mapNotNull { tag ->
                val title = tag.getAttributeValue("jcr:title") ?: return@mapNotNull null
                val value = tag.getAttributeValue("value") ?: return@mapNotNull null
                AcsGenericListItem(title, value)
            }
            .toList()
        return AcsGenericList(name, repositoryPath, file, items)
    }

    private fun loadNamedImageTransforms(
        scope: GlobalSearchScope,
        manager: PsiManager,
    ): List<AcsNamedImageTransform> =
        FilenameIndex.getAllFilenames(project)
            .asSequence()
            .filter { it.startsWith(NAMED_TRANSFORM_PID) }
            .flatMap { FilenameIndex.getVirtualFilesByName(it, scope).asSequence() }
            .mapNotNull { file ->
                val psiFile = manager.findFile(file) ?: return@mapNotNull null
                val values = when (psiFile) {
                    is XmlFile -> {
                        val root = psiFile.rootTag ?: return@mapNotNull null
                        root.getAttributeValue("name") to
                            parseArray(root.getAttributeValue("transforms").orEmpty())
                    }
                    else -> parseJsonTransform(psiFile)
                }
                val name = values.first ?: return@mapNotNull null
                val transforms = values.second
                AcsNamedImageTransform(name, transforms, file)
            }
            .sortedBy(AcsNamedImageTransform::name)
            .toList()

    private fun toRedirectRules(
        file: VirtualFile,
        path: String,
        xml: XmlFile,
    ): List<AcsRedirectRule> {
        if (!path.contains("/settings/redirects")) return emptyList()
        return xml.rootTag?.let(::descendants).orEmpty().mapNotNull { tag ->
            val source = tag.getAttributeValue("source") ?: return@mapNotNull null
            val target = tag.getAttributeValue("target") ?: return@mapNotNull null
            AcsRedirectRule(
                source = source,
                target = target,
                statusCode = tag.getAttributeValue("statusCode")
                    ?.removePrefix("{Long}")
                    ?.toIntOrNull(),
                repositoryPath = "$path/${tag.name}".replace("//", "/"),
                file = file,
            )
        }.toList()
    }

    private fun toSharedProperties(
        file: VirtualFile,
        path: String,
    ): AcsSharedProperties? {
        val sharedMarker = "/shared-component-properties/"
        return when {
            path.endsWith("/global-component-properties") ->
                AcsSharedProperties(
                    AcsSharedProperties.Scope.GLOBAL,
                    null,
                    path,
                    file,
                )
            path.contains(sharedMarker) ->
                AcsSharedProperties(
                    AcsSharedProperties.Scope.SHARED,
                    path.substringAfter(sharedMarker),
                    path,
                    file,
                )
            else -> null
        }
    }

    private fun descendants(root: XmlTag): Sequence<XmlTag> =
        sequenceOf(root) + root.subTags.asSequence().flatMap(::descendants)

    private fun parseArray(value: String): List<String> =
        value.removePrefix("[").removeSuffix("]")
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)

    private fun parseJsonTransform(file: PsiFile): Pair<String?, List<String>> {
        val text = file.text
        val name = JSON_NAME.find(text)?.groupValues?.get(1)
        val transforms = JSON_TRANSFORMS.find(text)?.groupValues?.get(1)
            ?.let { body ->
                JSON_STRING.findAll(body).map { it.groupValues[1] }.toList()
            }
            .orEmpty()
        return name to transforms
    }

    companion object {
        const val GENERIC_LIST_ROOT = "/etc/acs-commons/lists"
        const val GENERIC_LIST_RESOURCE_TYPE =
            "acs-commons/components/utilities/genericlist"
        const val GENERIC_LIST_DATASOURCE_RESOURCE_TYPE =
            "acs-commons/components/utilities/genericlist/datasource"
        const val NAMED_TRANSFORM_PID =
            "com.adobe.acs.commons.images.impl.NamedImageTransformerImpl"
        private val JSON_NAME = Regex(""""name"\s*:\s*"([^"]+)"""")
        private val JSON_TRANSFORMS = Regex(
            """"transforms"\s*:\s*\[(.*?)]""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val JSON_STRING = Regex(""""([^"]+)"""")

        fun getInstance(project: Project): AcsCommonsService = project.service()
    }
}

private data class AcsCommonsIndex(
    val genericLists: List<AcsGenericList>,
    val namedImageTransforms: List<AcsNamedImageTransform>,
    val redirectRules: List<AcsRedirectRule>,
    val sharedProperties: List<AcsSharedProperties>,
)
