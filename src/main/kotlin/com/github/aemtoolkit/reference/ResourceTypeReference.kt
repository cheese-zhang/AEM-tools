package com.github.aemtoolkit.reference

import com.github.aemtoolkit.resolver.AemPlatformResourceType
import com.github.aemtoolkit.resolver.AemResourceTypeTargetResolver
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.xml.XmlAttributeValue

/**
 * Navigates a `sling:resourceType` value to its component directory.
 */
class ResourceTypeReference(element: XmlAttributeValue) :
    PsiPolyVariantReferenceBase<XmlAttributeValue>(
        element,
        TextRange(1, element.textLength - 1),
        false,
    ) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        AemResourceTypeTargetResolver.getInstance(element.project)
            .resolve(element.value)
            .map(::PsiElementResolveResult)
            .toTypedArray()

    override fun isSoft(): Boolean =
        AemPlatformResourceType.isExternal(element.value)
}
