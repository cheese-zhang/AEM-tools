package com.github.aemtoolkit.reference

import com.github.aemtoolkit.clientlib.AemClientLibraryService
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
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlText
import com.intellij.psi.xml.XmlToken
import com.intellij.util.ProcessingContext

/** Navigates ClientLib categories in HTL calls and FileVault XML properties. */
class ClientLibraryCategoryReferenceContributor : PsiReferenceContributor() {
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
                    val text = when (element) {
                        is XmlAttributeValue -> element.value
                        is XmlText -> element.value
                        is XmlToken -> element.text
                        else -> return PsiReference.EMPTY_ARRAY
                    }
                    val contentOffset = if (element is XmlAttributeValue) 1 else 0
                    val categories = if (element is XmlAttributeValue &&
                        (element.parent as? XmlAttribute)?.name in XML_PROPERTIES
                    ) {
                        val service = AemClientLibraryService.getInstance(element.project)
                        if (service.findByFile(element.containingFile.virtualFile) == null) {
                            return PsiReference.EMPTY_ARRAY
                        }
                        fileVaultValues(text)
                    } else if (HtlUtil.isHtlFile(element.containingFile)) {
                        htlCategoryValues(text)
                    } else {
                        emptyList()
                    }
                    return categories.map { value ->
                        ClientLibraryCategoryReference(
                            element,
                            value.range.shiftRight(contentOffset),
                            value.value,
                        )
                    }.toTypedArray()
                }
            },
        )
    }

    private fun htlCategoryValues(text: String): List<ValueRange> =
        HTL_CATEGORIES.findAll(text).flatMap { categories ->
            val group = categories.groups[1]!!
            quotedValues(group.value).map {
                it.copy(range = it.range.shiftRight(group.range.first))
            }
        }.toList()

    private fun quotedValues(text: String): List<ValueRange> =
        QUOTED.findAll(text).map {
            val group = it.groups[1]!!
            ValueRange(group.value, TextRange(group.range.first, group.range.last + 1))
        }.toList()

    private fun fileVaultValues(text: String): List<ValueRange> {
        val prefixEnd = Regex("""^\{[^}]+}""").find(text)?.range?.last?.plus(1) ?: 0
        val contentStart = text.indexOf('[', prefixEnd).let { if (it >= 0) it + 1 else prefixEnd }
        val contentEnd = text.lastIndexOf(']').let {
            if (it >= contentStart) it else text.length
        }
        val content = text.substring(contentStart, contentEnd)
        return VALUE.findAll(content).mapNotNull { match ->
            val raw = match.value
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            val leading = raw.indexOf(trimmed)
            val quoteOffset = if (trimmed.first() in charArrayOf('"', '\'')) 1 else 0
            val value = trimmed.trim('"', '\'')
            ValueRange(
                value,
                TextRange(
                    contentStart + match.range.first + leading + quoteOffset,
                    contentStart + match.range.first + leading + quoteOffset + value.length,
                ),
            )
        }.toList()
    }

    private data class ValueRange(val value: String, val range: TextRange)

    private companion object {
        val XML_PROPERTIES = setOf("categories", "dependencies", "embed")
        val HTL_CATEGORIES = Regex(
            """clientlib\.(?:js|css|all)\s*@[^}]*\bcategories\s*=\s*\[([^]]*)]""",
        )
        val QUOTED = Regex("""['"]([^'"]+)['"]""")
        val VALUE = Regex("""[^,]+""")
    }
}

private class ClientLibraryCategoryReference(
    element: PsiElement,
    range: TextRange,
    private val category: String,
) : PsiPolyVariantReferenceBase<PsiElement>(element, range, true) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        AemClientLibraryService.getInstance(element.project)
            .findByCategory(category)
            .mapNotNull {
                PsiManager.getInstance(element.project).findFile(it.contentXml)
            }
            .map(::PsiElementResolveResult)
            .toTypedArray()
}
