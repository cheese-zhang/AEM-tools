package com.github.aemtoolkit.resolver

import com.github.aemtoolkit.util.HtlUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlText
import com.intellij.psi.xml.XmlToken

/**
 * Locates HTL property expressions backed by a Java getter.
 */
object HtlJavaUsageResolver {
    /** Finds HTL attribute values that resolve to [method]. */
    fun findUsages(method: PsiMethod): List<PsiElement> {
        val modelClass = method.containingClass ?: return emptyList()
        val property = HtlJavaModelResolver.propertyName(method) ?: return emptyList()
        return usagesByProperty(modelClass)[property].orEmpty()
    }

    private fun usagesByProperty(modelClass: PsiClass): Map<String, List<PsiElement>> =
        CachedValuesManager.getCachedValue(modelClass) {
            CachedValueProvider.Result.create(
                collectUsages(modelClass),
                com.intellij.psi.util.PsiModificationTracker.MODIFICATION_COUNT,
            )
        }

    private fun collectUsages(modelClass: PsiClass): Map<String, List<PsiElement>> {
        val usages = linkedMapOf<String, MutableList<PsiElement>>()
        val project = modelClass.project
        val qualifiedName = modelClass.qualifiedName ?: return emptyMap()
        val properties = modelClass.allMethods
            .mapNotNull(HtlJavaModelResolver::propertyName)
            .toSet()
        if (properties.isEmpty()) return emptyMap()

        FilenameIndex.getAllFilesByExt(
            project,
            "html",
            GlobalSearchScope.projectScope(project),
        ).forEach { virtualFile ->
            val file = PsiManager.getInstance(project).findFile(virtualFile)
                ?.takeIf(HtlUtil::isHtlFile)
                ?: return@forEach
            val variables = PsiTreeUtil.findChildrenOfType(file, XmlAttribute::class.java)
                .asSequence()
                .filter { it.name.startsWith(USE_PREFIX) }
                .filter { attribute ->
                    attribute.valueElement?.let {
                        HtlJavaModelResolver.declaredClassName(it, attribute.name.removePrefix(USE_PREFIX))
                    } == qualifiedName
                }
                .map { it.name.removePrefix(USE_PREFIX) }
                .filter(String::isNotBlank)
                .toSet()
            if (variables.isEmpty()) return@forEach

            PsiTreeUtil.findChildrenOfAnyType(
                file,
                XmlAttributeValue::class.java,
                XmlToken::class.java,
            )
                .asSequence()
                .filter { it !is XmlToken || it.parent is XmlText }
                .forEach { element ->
                    variables.forEach { variable ->
                        properties.forEach { property ->
                            if (containsProperty(element.text, variable, property)) {
                                usages.getOrPut(property) { mutableListOf() }.add(element)
                            }
                        }
                    }
                }
        }
        return usages
    }

    private fun containsProperty(text: String, variable: String, property: String): Boolean =
        Regex(
            """\$\{[^}]*\b${Regex.escape(variable)}\s*\.\s*${Regex.escape(property)}\b""",
        ).containsMatchIn(text)

    private const val USE_PREFIX = "data-sly-use."
}
