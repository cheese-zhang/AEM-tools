package com.github.aemtoolkit.osgi

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.FileTypeIndex
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.xml.XmlFile

/** Correlates OSGi configuration files with Java DS and Felix SCR services. */
@Service(Service.Level.PROJECT)
class OsgiConfigurationService(private val project: Project) {
    private val index: CachedValue<List<OsgiConfiguration>> =
        CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result.create(
                    loadConfigurations(),
                    PsiModificationTracker.MODIFICATION_COUNT,
                    ProjectRootModificationTracker.getInstance(project),
                )
            },
            false,
        )

    fun all(): List<OsgiConfiguration> = index.value

    fun findByPid(pid: String): List<OsgiConfiguration> =
        all().filter { it.pid == pid }

    fun findForClass(type: PsiClass): List<OsgiConfiguration> =
        servicePids(type).flatMap(::findByPid).distinctBy { it.file }

    fun findClasses(configuration: OsgiConfiguration): List<PsiClass> {
        val facade = JavaPsiFacade.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)
        val exact = listOfNotNull(facade.findClass(configuration.pid, scope))
        val manager = PsiManager.getInstance(project)
        val explicit = FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope)
            .asSequence()
            .mapNotNull { manager.findFile(it) as? PsiJavaFile }
            .flatMap { PsiTreeUtil.findChildrenOfType(it, PsiClass::class.java).asSequence() }
            .filter { configuration.pid in servicePids(it) }
            .toList()
        return (exact + explicit)
            .filter { configuration.pid in servicePids(it) }
            .distinctBy(PsiClass::getQualifiedName)
    }

    fun findByFile(file: VirtualFile): OsgiConfiguration? =
        all().firstOrNull { it.file == file }

    /** Finds serialized XML attributes for [propertyName] across a service's configs. */
    fun findPropertyTargets(type: PsiClass, propertyName: String): List<XmlAttribute> =
        findForClass(type).mapNotNull { configuration ->
            val xml = PsiManager.getInstance(project).findFile(configuration.file) as? XmlFile
                ?: return@mapNotNull null
            xml.rootTag?.getAttribute(propertyName)
        }

    /** Returns explicit configuration PIDs, falling back to the implementation class. */
    fun servicePids(type: PsiClass): List<String> {
        val annotations = type.modifierList?.annotations.orEmpty()
        if (annotations.none(::isOsgiServiceAnnotation)) return emptyList()
        val explicit = annotations
            .filter(::isOsgiServiceAnnotation)
            .flatMap { annotation ->
                literalStrings(annotation, "configurationPid") +
                    literalStrings(annotation, "name")
            }
            .filterNot { it.isBlank() || it == "$" }
        return (explicit + listOfNotNull(type.qualifiedName)).distinct()
    }

    private fun loadConfigurations(): List<OsgiConfiguration> {
        val result = mutableListOf<OsgiConfiguration>()
        ProjectFileIndex.getInstance(project).iterateContent { file ->
            parse(file)?.let(result::add)
            true
        }
        return result.sortedWith(
            compareBy<OsgiConfiguration>({ it.pid }, { it.file.path }),
        )
    }

    private fun parse(file: VirtualFile): OsgiConfiguration? {
        if (file.isDirectory || !isConfigPath(file.path)) return null
        if (!isSupportedConfig(file.name)) return null
        if (file.name.endsWith(".xml", true) && !isOsgiXml(file)) return null
        val pid = pidFromFileName(file.name) ?: return null
        return OsgiConfiguration(pid, runModes(file), file)
    }

    private fun isOsgiXml(file: VirtualFile): Boolean {
        val xml = PsiManager.getInstance(project).findFile(file) as? XmlFile ?: return false
        return xml.rootTag?.getAttributeValue("jcr:primaryType") == "sling:OsgiConfig"
    }

    private fun literalStrings(annotation: PsiAnnotation, attribute: String): List<String> =
        when (val value = annotation.findAttributeValue(attribute)) {
            is PsiLiteralExpression -> listOfNotNull(value.value as? String)
            is PsiArrayInitializerMemberValue ->
                value.initializers.mapNotNull { (it as? PsiLiteralExpression)?.value as? String }
            else -> emptyList()
        }

    companion object {
        private val ANNOTATIONS = setOf(
            "org.osgi.service.component.annotations.Component",
            "org.apache.felix.scr.annotations.Component",
            "org.apache.felix.scr.annotations.Service",
        )

        fun pidFromFileName(fileName: String): String? {
            val base = when {
                fileName.endsWith(".cfg.json", true) ->
                    fileName.dropLast(".cfg.json".length).substringBefore('~')
                fileName.endsWith(".config", true) ->
                    fileName.dropLast(".config".length).substringBefore('~').substringBefore('-')
                fileName.endsWith(".xml", true) ->
                    fileName.dropLast(".xml".length).substringBefore('~').substringBefore('-')
                else -> return null
            }
            return base.takeIf(String::isNotBlank)
        }

        fun runModes(file: VirtualFile): List<String> =
            generateSequence(file.parent) { it.parent }
                .map(VirtualFile::getName)
                .firstOrNull { it == "config" || it.startsWith("config.") }
                ?.substringAfter("config", "")
                ?.trimStart('.')
                ?.split('.')
                ?.filter(String::isNotBlank)
                .orEmpty()

        private fun isConfigPath(path: String): Boolean =
            path.replace('\\', '/').split('/').any {
                it == "config" || it.startsWith("config.")
            }

        private fun isSupportedConfig(fileName: String): Boolean =
            fileName.endsWith(".xml", true) ||
                fileName.endsWith(".config", true) ||
                fileName.endsWith(".cfg.json", true)

        private fun isOsgiServiceAnnotation(annotation: PsiAnnotation): Boolean =
            annotation.qualifiedName in ANNOTATIONS ||
                annotation.nameReferenceElement?.referenceName in setOf("Component", "Service")

        fun getInstance(project: Project): OsgiConfigurationService = project.service()
    }
}
