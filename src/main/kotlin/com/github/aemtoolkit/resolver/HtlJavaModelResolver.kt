package com.github.aemtoolkit.resolver

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
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
    fun properties(value: PsiElement, variable: String): List<HtlJavaProperty> {
        val modelClass = resolveModelClass(value, variable) ?: return emptyList()
        return modelClass.allMethods
            .mapNotNull(::toProperty)
            .distinctBy(HtlJavaProperty::name)
            .sortedBy(HtlJavaProperty::name)
    }

    /** Resolves [property] to the Java member backing it. */
    fun resolveProperty(
        value: PsiElement,
        variable: String,
        property: String,
    ): PsiMember? =
        properties(value, variable).firstOrNull { it.name == property }?.member

    /** Returns true when [variable] has a `data-sly-use` declaration. */
    fun hasDeclaration(value: PsiElement, variable: String): Boolean {
        val xmlFile = value.containingFile as? XmlFile ?: return false
        return xmlFile.rootTag?.let { findUseDeclaration(it, variable) } != null
    }

    /** Returns the Java class name declared for [variable], if it is class-based. */
    fun declaredClassName(value: PsiElement, variable: String): String? {
        val xmlFile = value.containingFile as? XmlFile ?: return null
        val declaration = xmlFile.rootTag
            ?.let { findUseDeclaration(it, variable) }
            ?.value
            ?: return null
        return className.find(declaration)?.value
    }

    /** Resolves the Java class declared for [variable]. */
    fun resolveModelClass(value: PsiElement, variable: String): PsiClass? {
        val qualifiedName = declaredClassName(value, variable) ?: return null
        return JavaPsiFacade.getInstance(value.project).findClass(
            qualifiedName,
            GlobalSearchScope.projectScope(value.project),
        )
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
}

/** Java member exposed as an HTL property. */
data class HtlJavaProperty(
    val name: String,
    val member: PsiMember,
)
