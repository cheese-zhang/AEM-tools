package com.github.aemtoolkit.reference

import com.github.aemtoolkit.acs.AcsCommonsService
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.ProcessingContext

/** Navigates ACS Generic List datasource and Classic UI list paths. */
class AcsCommonsReferenceContributor : PsiReferenceContributor() {
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
                    if (attribute.name !in setOf("path", "options")) {
                        return PsiReference.EMPTY_ARRAY
                    }
                    if (AcsCommonsService.getInstance(element.project)
                            .findGenericList(value.value) == null
                    ) {
                        return PsiReference.EMPTY_ARRAY
                    }
                    return arrayOf(AcsGenericListReference(value))
                }
            },
        )
    }
}

private class AcsGenericListReference(element: XmlAttributeValue) :
    PsiReferenceBase<XmlAttributeValue>(
        element,
        TextRange(1, element.textLength - 1),
        true,
    ) {
    override fun resolve(): PsiElement? {
        val list = AcsCommonsService.getInstance(element.project)
            .findGenericList(element.value)
            ?: return null
        return PsiManager.getInstance(element.project).findFile(list.file)
    }
}
