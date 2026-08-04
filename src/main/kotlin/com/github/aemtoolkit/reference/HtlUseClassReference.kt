package com.github.aemtoolkit.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlAttributeValue

/**
 * Navigates a `data-sly-use` class declaration to its Java source.
 */
class HtlUseClassReference(
    element: XmlAttributeValue,
    range: TextRange,
    private val qualifiedName: String,
) : PsiReferenceBase<XmlAttributeValue>(element, range, false) {
    override fun resolve(): PsiClass? =
        JavaPsiFacade.getInstance(element.project).findClass(
            qualifiedName,
            GlobalSearchScope.projectScope(element.project),
        )
}
