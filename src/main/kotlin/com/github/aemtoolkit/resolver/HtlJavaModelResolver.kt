package com.github.aemtoolkit.resolver

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiJavaFile
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlAttributeValue
import java.beans.Introspector
import com.intellij.psi.xml.XmlTag

/**
 * Resolves `data-sly-use` Java classes and their HTL-visible properties.
 */
object HtlJavaModelResolver {
    private val className = Regex("""[A-Za-z_$][\w$]*(?:\.[\w$]+)+""")

    /** Returns Java-backed properties visible through [variable]. */
    fun properties(
        value: PsiElement,
        variable: String,
        propertyChain: List<String> = emptyList(),
    ): List<HtlJavaProperty> {
        val modelClass = resolveModelClass(value, variable) ?: return emptyList()
        val targetClass = resolvePropertyClass(modelClass, propertyChain) ?: return emptyList()
        return visibleProperties(targetClass)
            .distinctBy(HtlJavaProperty::name)
            .sortedBy(HtlJavaProperty::name)
            .toList()
    }

    /** Resolves [property] to the Java member backing it. */
    fun resolveProperty(
        value: PsiElement,
        variable: String,
        property: String,
        propertyChain: List<String> = emptyList(),
    ): PsiMember? =
        properties(value, variable, propertyChain)
            .firstOrNull { it.name == property }
            ?.member

    /** Returns true when [variable] has a `data-sly-use` declaration. */
    fun hasDeclaration(value: PsiElement, variable: String): Boolean {
        return findDeclaration(value, variable) != null
    }

    /** Finds the `data-sly-use` XML attribute declaring [variable]. */
    fun findDeclaration(value: PsiElement, variable: String) =
        (value.containingFile as? XmlFile)?.rootTag
            ?.let { findUseDeclaration(it, variable) }
            ?.parent as? com.intellij.psi.xml.XmlAttribute

    /** Returns the Java class name declared for [variable], if it is class-based. */
    fun declaredClassName(value: PsiElement, variable: String): String? {
        val xmlFile = value.containingFile as? XmlFile ?: return null
        val declaration = xmlFile.rootTag
            ?.let { findUseDeclaration(it, variable) }
            ?.value
            ?: return null
        val candidate = QUOTED_VALUE.find(declaration)?.groupValues?.get(1)
            ?: declaration.trim()
        return candidate.takeIf {
            className.matches(it) && !it.endsWith(".html", true)
        }
    }

    /** Resolves the Java class declared for [variable]. */
    fun resolveModelClass(value: PsiElement, variable: String): PsiClass? {
        val qualifiedName = declaredClassName(value, variable) ?: return null
        return JavaPsiFacade.getInstance(value.project).findClass(
            qualifiedName,
            GlobalSearchScope.projectScope(value.project),
        )
    }

    /** Returns project classes suitable for `data-sly-use` declarations. */
    fun availableModelClasses(project: Project): List<PsiClass> {
        val scope = GlobalSearchScope.projectScope(project)
        val manager = com.intellij.psi.PsiManager.getInstance(project)
        return FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope)
            .asSequence()
            .mapNotNull { manager.findFile(it) as? PsiJavaFile }
            .flatMap { PsiTreeUtil.findChildrenOfType(it, PsiClass::class.java).asSequence() }
            .filter(::isHtlModelClass)
            .distinctBy(PsiClass::getQualifiedName)
            .sortedBy { it.qualifiedName }
            .toList()
    }

    private fun findUseDeclaration(tag: XmlTag, variable: String): XmlAttributeValue? {
        tag.getAttribute("data-sly-use.$variable")?.valueElement?.let { return it }
        return tag.subTags.firstNotNullOfOrNull { findUseDeclaration(it, variable) }
    }

    private fun toProperty(method: PsiMethod): HtlJavaProperty? {
        if (!method.hasModifierProperty(PsiModifier.PUBLIC) ||
            method.parameterList.parametersCount != 0
        ) {
            return null
        }

        val propertyName = propertyName(method) ?: return null
        return HtlJavaProperty(propertyName, method)
    }

    private fun toProperty(field: PsiField): HtlJavaProperty? =
        field.takeIf { it.hasModifierProperty(PsiModifier.PUBLIC) }
            ?.let { HtlJavaProperty(it.name, it) }

    private fun visibleProperties(type: PsiClass): Sequence<HtlJavaProperty> =
        type.allMethods.asSequence().mapNotNull(::toProperty) +
            type.allFields.asSequence().mapNotNull(::toProperty)

    private fun resolvePropertyClass(
        root: PsiClass,
        propertyChain: List<String>,
    ): PsiClass? = propertyChain.fold(root as PsiClass?) { current, property ->
        val member = current?.let(::visibleProperties)
            ?.firstOrNull { it.name == property }
            ?.member
            ?: return null
        classForType(
            when (member) {
                is PsiMethod -> member.returnType
                is PsiField -> member.type
                else -> null
            },
        )
    }

    private fun classForType(type: com.intellij.psi.PsiType?): PsiClass? {
        return when (type) {
            is PsiArrayType -> classForType(type.componentType)
            is PsiClassType -> {
                val rawType = type.canonicalText.substringBefore('<')
                if (rawType in ITERABLE_TYPES ||
                    rawType.substringAfterLast('.') in ITERABLE_SHORT_NAMES
                ) {
                    return type.parameters.firstOrNull()?.let(::classForType)
                }
                val resolved = type.resolve() ?: return null
                if (resolved.qualifiedName in ITERABLE_TYPES ||
                    InheritanceUtil.isInheritor(resolved, "java.lang.Iterable")
                ) {
                    type.parameters.firstOrNull()?.let(::classForType)
                } else {
                    resolved
                }
            }
            else -> null
        }
    }

    private fun isHtlModelClass(type: PsiClass): Boolean {
        if (!type.hasModifierProperty(PsiModifier.PUBLIC) ||
            type.hasModifierProperty(PsiModifier.ABSTRACT) ||
            type.isInterface ||
            type.qualifiedName == null
        ) {
            return false
        }
        if (type.annotations.any { annotation ->
                annotation.qualifiedName == SLING_MODEL_ANNOTATION ||
                    annotation.nameReferenceElement?.referenceName == "Model"
            }
        ) {
            return true
        }
        return generateSequence(type.superClass) { it.superClass }
            .any { it.name in USE_CLASS_NAMES }
    }

    /** Returns the HTL property exposed by [method], or `null` for non-getters. */
    fun propertyName(method: PsiMethod): String? {
        val name = when {
            method.name.startsWith("get") && method.name.length > 3 ->
                method.name.substring(3)
            method.name.startsWith("is") && method.name.length > 2 ->
                method.name.substring(2)
            else -> return null
        }
        return Introspector.decapitalize(name)
    }

    private const val SLING_MODEL_ANNOTATION = "org.apache.sling.models.annotations.Model"
    private val QUOTED_VALUE = Regex("""['"]([^'"]+)['"]""")
    private val USE_CLASS_NAMES = setOf("WCMUse", "WCMUsePojo", "Use", "UsePojo")
    private val ITERABLE_TYPES = setOf(
        "java.lang.Iterable",
        "java.util.Collection",
        "java.util.List",
        "java.util.Set",
    )
    private val ITERABLE_SHORT_NAMES = ITERABLE_TYPES.mapTo(mutableSetOf()) {
        it.substringAfterLast('.')
    }
}

/** Java member exposed as an HTL property. */
data class HtlJavaProperty(
    val name: String,
    val member: PsiMember,
)
