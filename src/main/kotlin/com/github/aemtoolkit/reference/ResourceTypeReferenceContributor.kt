package com.github.aemtoolkit.reference

import com.github.aemtoolkit.resolver.AemArtifactResolver
import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.ProcessingContext

/**
 * Registers references for AEM resource type attribute values.
 */
class ResourceTypeReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(XmlAttributeValue::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext,
                ): Array<PsiReference> {
                    val value = element as? XmlAttributeValue ?: return PsiReference.EMPTY_ARRAY
                    val attribute = AemXmlUtil.containingAttribute(value)
                    if (attribute == null) {
                        return PsiReference.EMPTY_ARRAY
                    }
                    if (AemXmlUtil.isRepositoryPath(attribute)) {
                        return arrayOf(RepositoryPathReference(value))
                    }
                    if (attribute.name == AemXmlUtil.PRIMARY_TYPE &&
                        (attribute.containingFile as? com.intellij.psi.xml.XmlFile)
                            ?.let(AemXmlUtil::isContentXml) == true
                    ) {
                        return arrayOf(JcrTypeReference(value))
                    }
                    if (!AemXmlUtil.isResourceType(attribute)) {
                        return PsiReference.EMPTY_ARRAY
                    }

                    val related = AemArtifactResolver.getInstance(element.project)
                        .relatedArtifacts(value.value)
                        .map { RelatedArtifactReference(value, it) }
                    return listOf(ResourceTypeReference(value))
                        .plus(related)
                        .toTypedArray()
                }
            },
        )
    }
}
