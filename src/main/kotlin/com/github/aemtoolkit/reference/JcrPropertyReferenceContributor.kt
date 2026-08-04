package com.github.aemtoolkit.reference

import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlFile
import com.intellij.util.ProcessingContext

/**
 * Registers CND-backed references for FileVault property names.
 */
class JcrPropertyReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
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
                    return arrayOf(JcrPropertyReference(attribute))
                }
            },
        )
    }
}
