package com.github.aemtoolkit.reference

import com.github.aemtoolkit.resolver.AemPlatformResourceType
import com.github.aemtoolkit.resolver.ResourceTypeResolver
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.xml.XmlAttributeValue

/**
 * Navigates a `sling:resourceType` value to its component directory.
 */
class ResourceTypeReference(element: XmlAttributeValue) :
    PsiReferenceBase<XmlAttributeValue>(
        element,
        TextRange(1, element.textLength - 1),
        false,
    ) {
    override fun resolve(): PsiDirectory? {
        val component = ResourceTypeResolver.getInstance(element.project).resolve(element.value)
            ?: return null
        return PsiManager.getInstance(element.project).findDirectory(component.directory)
    }

    override fun isSoft(): Boolean =
        AemPlatformResourceType.isExternal(element.value)
}
