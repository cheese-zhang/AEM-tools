package com.github.aemtoolkit.reference

import com.github.aemtoolkit.i18n.AemI18nService
import com.github.aemtoolkit.util.HtlUtil
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.ResolveResult
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlText
import com.intellij.psi.xml.XmlToken
import com.intellij.util.ProcessingContext

/** Navigates HTL `i18n['key']` expressions to FileVault translation entries. */
class AemI18nReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(XmlAttributeValue::class.java),
                PlatformPatterns.psiElement(XmlText::class.java),
                PlatformPatterns.psiElement(XmlToken::class.java),
            ),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext,
                ): Array<PsiReference> {
                    if (!HtlUtil.isHtlFile(element.containingFile)) {
                        return PsiReference.EMPTY_ARRAY
                    }
                    val (text, baseOffset) = when (element) {
                        is XmlAttributeValue -> element.value to 1
                        is XmlText -> element.value to 0
                        is XmlToken -> {
                            val parent = element.parent as? XmlText
                            if (parent != null) {
                                parent.value to
                                    (parent.textRange.startOffset - element.textRange.startOffset)
                            } else {
                                element.text to 0
                            }
                        }
                        else -> return PsiReference.EMPTY_ARRAY
                    }
                    return I18N.findAll(text).mapNotNull { match ->
                        val key = match.groups[1] ?: match.groups[2]!!
                        val range = TextRange(
                            key.range.first + baseOffset,
                            key.range.last + baseOffset + 1,
                        )
                        if (range.startOffset < 0 || range.endOffset > element.textLength) {
                            return@mapNotNull null
                        }
                        AemI18nReference(
                            element,
                            range,
                            key.value,
                        )
                    }.toList().toTypedArray()
                }
            },
        )
    }

    private companion object {
        val I18N = Regex(
            """(?<!\\)\$\{(?:[^}]*\bi18n\s*\[\s*['"]([^'"]+)['"]\s*][^}]*|""" +
                """\s*['"]([^'"]+)['"]\s*@\s*i18n\b[^}]*)}""",
        )
    }
}

private class AemI18nReference(
    element: PsiElement,
    range: TextRange,
    private val key: String,
) : PsiPolyVariantReferenceBase<PsiElement>(element, range, true) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        AemI18nService.getInstance(element.project).find(key)
            .mapNotNull { entry ->
                PsiManager.getInstance(element.project).findFile(entry.file)
            }
            .distinct()
            .map(::PsiElementResolveResult)
            .toTypedArray()
}
