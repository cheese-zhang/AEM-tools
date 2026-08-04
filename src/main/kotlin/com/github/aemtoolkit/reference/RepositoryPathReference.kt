package com.github.aemtoolkit.reference

import com.github.aemtoolkit.resolver.AemArtifactResolver
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.xml.XmlAttributeValue

/**
 * Navigates an AEM repository path such as `cq:template` or `cq:policy`.
 */
class RepositoryPathReference(element: XmlAttributeValue) :
    PsiReferenceBase<XmlAttributeValue>(
        element,
        TextRange(1, element.textLength - 1),
        false,
    ) {
    override fun resolve(): PsiDirectory? {
        val target = AemArtifactResolver.getInstance(element.project)
            .resolveRepositoryPath(element.value)
            ?: return null
        return PsiManager.getInstance(element.project).findDirectory(target)
    }
}
