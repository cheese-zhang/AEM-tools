package com.github.aemtoolkit.reference

import com.github.aemtoolkit.caconfig.CaConfigService
import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import com.intellij.util.ProcessingContext

/** Registers navigation between CAConfig XML and Java declarations. */
class CaConfigReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(XmlAttributeValue::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext,
                ): Array<PsiReference> {
                    val value = element as? XmlAttributeValue ?: return PsiReference.EMPTY_ARRAY
                    val attribute = value.parent as? XmlAttribute ?: return PsiReference.EMPTY_ARRAY
                    if (attribute.name != CaConfigService.CONFIG_REF) {
                        return PsiReference.EMPTY_ARRAY
                    }
                    return arrayOf(CaConfigRootReference(value))
                }
            },
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(XmlAttribute::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext,
                ): Array<PsiReference> {
                    val attribute = element as? XmlAttribute ?: return PsiReference.EMPTY_ARRAY
                    val file = attribute.containingFile as? XmlFile
                        ?: return PsiReference.EMPTY_ARRAY
                    if (!AemXmlUtil.isContentXml(file) || attribute.isNamespaceDeclaration) {
                        return PsiReference.EMPTY_ARRAY
                    }
                    val resource = CaConfigService.getInstance(element.project).resources()
                        .firstOrNull { it.file == file.virtualFile }
                        ?: return PsiReference.EMPTY_ARRAY
                    val definition = CaConfigService.getInstance(element.project)
                        .findDefinition(resource.configName)
                        ?: return PsiReference.EMPTY_ARRAY
                    if (definition.properties.none { it.name == attribute.name }) {
                        return PsiReference.EMPTY_ARRAY
                    }
                    return arrayOf(CaConfigPropertyReference(attribute, resource.configName))
                }
            },
        )
    }
}

private class CaConfigRootReference(element: XmlAttributeValue) :
    PsiReferenceBase<XmlAttributeValue>(
        element,
        TextRange(1, element.textLength - 1),
        true,
    ) {
    override fun resolve(): PsiElement? {
        val resource = CaConfigService.getInstance(element.project).resources()
            .firstOrNull { it.contextPath == element.value }
            ?: return null
        return com.intellij.psi.PsiManager.getInstance(element.project)
            .findFile(resource.file)
    }
}

private class CaConfigPropertyReference(
    element: XmlAttribute,
    private val configName: String,
) : PsiReferenceBase<XmlAttribute>(
    element,
    TextRange.from(0, element.name.length),
    true,
) {
    override fun resolve(): PsiElement? =
        CaConfigService.getInstance(element.project)
            .findDefinition(configName)
            ?.properties
            ?.firstOrNull { it.name == element.name }
            ?.declaration
}
