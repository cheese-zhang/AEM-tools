package com.github.aemtoolkit.reference

import com.github.aemtoolkit.util.HtlUtil
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlText
import com.intellij.psi.xml.XmlToken
import com.intellij.util.ProcessingContext

/**
 * Registers Java model property references embedded in HTL expressions.
 */
class HtlReferenceContributor : PsiReferenceContributor() {
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
                    val expressionText = when (element) {
                        is XmlAttributeValue -> element.value
                        is XmlText -> element.value
                        is XmlToken -> element.text
                            .takeIf { element.parent is XmlText }
                            ?: return PsiReference.EMPTY_ARRAY
                        else -> return PsiReference.EMPTY_ARRAY
                    }
                    val contentOffset = if (element is XmlAttributeValue) 1 else 0
                    val references = PROPERTY.findAll(expressionText)
                        .map { match ->
                            val propertyRange = match.groups[2]!!.range
                            HtlJavaPropertyReference(
                                element,
                                TextRange(
                                    propertyRange.first + contentOffset,
                                    propertyRange.last + contentOffset + 1,
                                ),
                                match.groupValues[1],
                                match.groupValues[2],
                            )
                        }
                        .toMutableList<PsiReference>()
                    val value = element as? XmlAttributeValue
                        ?: return references.toTypedArray()
                    val attribute = value.parent as? XmlAttribute
                    if (attribute?.name?.startsWith("data-sly-use.") == true) {
                        CLASS_NAME.find(value.value)?.let { match ->
                            references.add(
                                HtlUseClassReference(
                                    value,
                                    TextRange(match.range.first + 1, match.range.last + 2),
                                    match.value,
                                ),
                            )
                        }
                        if (CLASS_NAME.find(value.value) == null) {
                            FILE_PATH.find(value.value)?.let { match ->
                                references.add(
                                    HtlFileReference(
                                        value,
                                        TextRange(match.range.first + 1, match.range.last + 2),
                                        match.value,
                                    ),
                                )
                            }
                        }
                    }
                    if (attribute?.name?.substringBefore('.') == "data-sly-call") {
                        TEMPLATE_CALL.find(value.value)?.let { match ->
                            val nameRange = match.groups[1]!!.range
                            references.add(
                                HtlTemplateReference(
                                    value,
                                    TextRange(nameRange.first + 1, nameRange.last + 2),
                                    match.groupValues[1],
                                ),
                            )
                        }
                    }
                    return references
                        .toTypedArray()
                }
            },
        )
    }

    private companion object {
        val PROPERTY = Regex("""\$\{\s*([A-Za-z_]\w*)\.([A-Za-z_]\w*)""")
        val CLASS_NAME = Regex("""[A-Za-z_$][\w$]*(?:\.[\w$]+)+""")
        val FILE_PATH = Regex("""(?:\./|\.\./)?[\w./-]+\.html""")
        val TEMPLATE_CALL = Regex("""\$\{\s*([A-Za-z_]\w*)\s*(?:@|})""")
    }
}
