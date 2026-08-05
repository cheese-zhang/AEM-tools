package com.github.aemtoolkit.caconfig

import com.github.aemtoolkit.resolver.AemRepositoryPath
import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMethod
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile

/**
 * Indexes Java CAConfig definitions and FileVault configuration resources.
 */
@Service(Service.Level.PROJECT)
class CaConfigService(private val project: Project) {
    private val index: CachedValue<CaConfigIndex> =
        CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result.create(
                    buildIndex(),
                    PsiModificationTracker.MODIFICATION_COUNT,
                )
            },
            false,
        )

    /** Returns every project CAConfig definition. */
    fun definitions(): List<CaConfigDefinition> = index.value.definitions

    /** Returns every project-local `/conf` CAConfig resource. */
    fun resources(): List<CaConfigResource> = index.value.resources

    /** Returns every project `sling:configRef` declaration. */
    fun references(): List<CaConfigReference> = index.value.references

    /** Finds a definition by explicit config name or Java qualified name. */
    fun findDefinition(name: String): CaConfigDefinition? =
        definitions().firstOrNull { it.name == name || it.qualifiedName == name }

    /** Finds resources representing [configName]. */
    fun findResources(configName: String): List<CaConfigResource> =
        resources().filter { it.configName == configName }

    /** Returns config roots ordered from the closest path to its ancestors. */
    fun effectiveResources(configRoot: String, configName: String): List<CaConfigResource> {
        val normalizedRoot = normalizeRepositoryPath(configRoot)
        return findResources(configName)
            .filter { resource ->
                normalizedRoot == resource.contextPath ||
                    normalizedRoot.startsWith("${resource.contextPath}/") ||
                    resource.contextPath in FALLBACK_ROOTS
            }
            .sortedBy { resource ->
                FALLBACK_ROOTS.indexOf(resource.contextPath)
                    .takeIf { it >= 0 }
                    ?.plus(10_000)
                    ?: (10_000 - resource.contextPath.length)
            }
    }

    private fun buildIndex(): CaConfigIndex {
        val definitions = loadDefinitions()
        return CaConfigIndex(
            definitions = definitions,
            resources = loadResources(definitions),
            references = loadReferences(),
        )
    }

    private fun loadDefinitions(): List<CaConfigDefinition> {
        val scope = GlobalSearchScope.projectScope(project)
        val manager = PsiManager.getInstance(project)
        return FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope)
            .asSequence()
            .mapNotNull { manager.findFile(it) as? PsiJavaFile }
            .flatMap { file ->
                PsiTreeUtil.findChildrenOfType(file, PsiClass::class.java).asSequence()
            }
            .mapNotNull(::toDefinition)
            .distinctBy(CaConfigDefinition::name)
            .sortedBy(CaConfigDefinition::name)
            .toList()
    }

    private fun toDefinition(type: PsiClass): CaConfigDefinition? {
        val annotation = type.annotations.firstOrNull(::isConfigurationAnnotation)
            ?: return null
        val qualifiedName = type.qualifiedName ?: return null
        return CaConfigDefinition(
            name = annotation.stringAttribute("name")
                ?.takeIf(String::isNotBlank)
                ?: type.name
                ?: return null,
            qualifiedName = qualifiedName,
            label = annotation.stringAttribute("label")?.takeIf(String::isNotBlank),
            description = annotation.stringAttribute("description")?.takeIf(String::isNotBlank),
            collection = annotation.booleanAttribute("collection") ?: false,
            declaration = type,
            properties = type.methods.mapNotNull { method ->
                val propertyAnnotation = method.annotations.firstOrNull(::isPropertyAnnotation)
                CaConfigProperty(
                    name = method.name,
                    type = method.returnType?.canonicalText ?: return@mapNotNull null,
                    label = propertyAnnotation?.stringAttribute("label")
                        ?.takeIf(String::isNotBlank),
                    description = propertyAnnotation?.stringAttribute("description")
                        ?.takeIf(String::isNotBlank),
                    order = propertyAnnotation?.intAttribute("order") ?: 0,
                    defaultValue = (method as? PsiAnnotationMethod)?.defaultValue?.text,
                    declaration = method,
                )
            },
        )
    }

    private fun loadResources(
        definitions: List<CaConfigDefinition>,
    ): List<CaConfigResource> {
        val manager = PsiManager.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)
        return FilenameIndex.getVirtualFilesByName(AemXmlUtil.CONTENT_XML, scope)
            .mapNotNull { file ->
                val path = file.path.replace('\\', '/')
                val marker = "/jcr_root/"
                val markerIndex = path.indexOf(marker)
                if (markerIndex < 0) return@mapNotNull null
                val repositoryPath = ("/" + path.substring(markerIndex + marker.length)
                    .removeSuffix("/${AemXmlUtil.CONTENT_XML}")
                    .trim('/'))
                    .replace("/_sling_configs/", "/sling:configs/")
                val configMarker = "/sling:configs/"
                val configIndex = repositoryPath.indexOf(configMarker)
                if (configIndex < 0 || CONFIG_STORAGE_ROOTS.none(repositoryPath::startsWith)) {
                    return@mapNotNull null
                }
                val relativeConfigPath = repositoryPath.substring(configIndex + configMarker.length)
                val configName = matchConfigName(relativeConfigPath, definitions)
                val xml = manager.findFile(file) as? XmlFile ?: return@mapNotNull null
                CaConfigResource(
                    configName = configName,
                    contextPath = repositoryPath.substring(0, configIndex),
                    repositoryPath = repositoryPath,
                    file = file,
                    properties = xml.rootTag?.attributes
                        ?.filterNot { it.isNamespaceDeclaration }
                        ?.mapNotNull { attribute ->
                            attribute.value?.let { attribute.name to it }
                        }
                        ?.toMap()
                        .orEmpty(),
                )
            }
            .sortedWith(compareBy(CaConfigResource::configName, CaConfigResource::contextPath))
    }

    private fun matchConfigName(
        relativePath: String,
        definitions: List<CaConfigDefinition>,
    ): String =
        definitions
            .asSequence()
            .flatMap { sequenceOf(it.name, it.qualifiedName) }
            .distinct()
            .filter { candidate ->
                relativePath == candidate || relativePath.startsWith("$candidate/")
            }
            .maxByOrNull(String::length)
            ?: relativePath.substringBefore('/')

    private fun loadReferences(): List<CaConfigReference> {
        val manager = PsiManager.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)
        return FilenameIndex.getVirtualFilesByName(AemXmlUtil.CONTENT_XML, scope)
            .mapNotNull { file ->
                val xml = manager.findFile(file) as? XmlFile ?: return@mapNotNull null
                val configRef = xml.rootTag?.getAttributeValue(CONFIG_REF) ?: return@mapNotNull null
                val repositoryPath = AemRepositoryPath.fromFilePath(file.path)
                    ?.removeSuffix("/${AemXmlUtil.CONTENT_XML}")
                    ?: return@mapNotNull null
                CaConfigReference(
                    repositoryPath = repositoryPath,
                    configRoot = normalizeRepositoryPath(configRef),
                    file = file,
                )
            }
            .sortedBy(CaConfigReference::repositoryPath)
    }

    private fun PsiAnnotation.stringAttribute(name: String): String? {
        val value = findDeclaredAttributeValue(name) ?: return null
        return JavaPsiFacade.getInstance(project)
            .constantEvaluationHelper
            .computeConstantExpression(value) as? String
    }

    private fun PsiAnnotation.booleanAttribute(name: String): Boolean? {
        val value = findDeclaredAttributeValue(name) ?: return null
        return JavaPsiFacade.getInstance(project)
            .constantEvaluationHelper
            .computeConstantExpression(value) as? Boolean
    }

    private fun PsiAnnotation.intAttribute(name: String): Int? {
        val value = findDeclaredAttributeValue(name) ?: return null
        return JavaPsiFacade.getInstance(project)
            .constantEvaluationHelper
            .computeConstantExpression(value) as? Int
    }

    private fun isConfigurationAnnotation(annotation: PsiAnnotation): Boolean =
        annotation.qualifiedName == CONFIGURATION_ANNOTATION ||
            annotation.nameReferenceElement?.referenceName == "Configuration"

    private fun isPropertyAnnotation(annotation: PsiAnnotation): Boolean =
        annotation.qualifiedName == PROPERTY_ANNOTATION ||
            annotation.nameReferenceElement?.referenceName == "Property"

    private fun normalizeRepositoryPath(path: String): String =
        AemRepositoryPath.normalize(path)

    companion object {
        const val CONFIG_REF = "sling:configRef"
        const val CONFIGURATION_ANNOTATION =
            "org.apache.sling.caconfig.annotation.Configuration"
        const val PROPERTY_ANNOTATION =
            "org.apache.sling.caconfig.annotation.Property"
        private val CONFIG_STORAGE_ROOTS = listOf("/conf/", "/apps/conf/", "/libs/conf/")
        private val FALLBACK_ROOTS = listOf("/conf/global", "/apps/conf", "/libs/conf")

        /** Returns the project CAConfig service. */
        fun getInstance(project: Project): CaConfigService = project.service()
    }
}

private data class CaConfigIndex(
    val definitions: List<CaConfigDefinition>,
    val resources: List<CaConfigResource>,
    val references: List<CaConfigReference>,
)
