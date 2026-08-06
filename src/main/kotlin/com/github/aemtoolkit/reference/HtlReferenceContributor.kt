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
                    val references = EXPRESSION.findAll(expressionText)
                        .flatMap { expression ->
                            val body = expression.groups[1]!!
                            PROPERTY_CHAIN.findAll(body.value).flatMap { match ->
                            val variable = match.groupValues[1]
                            val chain = mutableListOf<String>()
                            SEGMENT.findAll(match.groupValues[2]).map { segment ->
                                val propertyRange = segment.groups[1]!!.range
                                val groupOffset =
                                    body.range.first + match.groups[2]!!.range.first
                                HtlJavaPropertyReference(
                                    element,
                                    TextRange(
                                        groupOffset + propertyRange.first + contentOffset,
                                        groupOffset + propertyRange.last + contentOffset + 1,
                                    ),
                                    variable,
                                    segment.groupValues[1],
                                    chain.toList(),
                                ).also {
                                    chain.add(segment.groupValues[1])
                                }
                            }
                        }
                        }
                        .toMutableList<PsiReference>()
                    EXPRESSION.findAll(expressionText).forEach { expression ->
                        val body = expression.groups[1]!!
                        IDENTIFIER.findAll(body.value).forEach { match ->
                            val variableGroup = match.groups[1]!!
                            val variable = variableGroup.value
                            if (com.github.aemtoolkit.resolver.HtlJavaModelResolver
                                    .hasDeclaration(element, variable)
                            ) {
                                references.add(
                                    HtlVariableReference(
                                        element,
                                        TextRange(
                                            body.range.first + variableGroup.range.first +
                                                contentOffset,
                                            body.range.first + variableGroup.range.last +
                                                contentOffset + 1,
                                        ),
                                        variable,
                                    ),
                                )
                            }
                        }
                    }
                    val value = element as? XmlAttributeValue
                        ?: return references.toTypedArray()
                    val attribute = value.parent as? XmlAttribute
                    if (attribute?.name?.startsWith("data-sly-use.") == true) {
                        USE_CLASS.matchEntire(value.value)?.let { match ->
                            val className = match.groupValues[1]
                            if (className.endsWith(".html", true)) return@let
                            val range = match.groups[1]!!.range
                            references.add(
                                HtlUseClassReference(
                                    value,
                                    TextRange(range.first + 1, range.last + 2),
                                    className,
                                ),
                            )
                        }
                        if (USE_CLASS.matchEntire(value.value)
                                ?.groupValues?.get(1)?.endsWith(".html", true) != false
                        ) {
                            USE_FILE.matchEntire(value.value)?.let { match ->
                                val range = match.groups[1]!!.range
                                references.add(
                                    HtlFileReference(
                                        value,
                                        TextRange(range.first + 1, range.last + 2),
                                        match.groupValues[1],
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
        val EXPRESSION = Regex("""(?<!\\)\$\{([^}]*)}""")
        val PROPERTY_CHAIN = Regex("""\b([A-Za-z_]\w*)((?:\.[A-Za-z_]\w*)+)""")
        val SEGMENT = Regex("""\.([A-Za-z_]\w*)""")
        val IDENTIFIER = Regex("""(?<![.\w])([A-Za-z_]\w*)""")
        val USE_CLASS = Regex(
            """(?:\$\{\s*['"])?([A-Za-z_$][\w$]*(?:\.[\w$]+)+)(?:['"]\s*})?""",
        )
        val USE_FILE = Regex(
            """(?:\$\{\s*['"])?((?:/|\./|\.\./)?[\w./-]+\.html)(?:['"]\s*})?""",
        )
        val TEMPLATE_CALL = Regex("""\$\{\s*([A-Za-z_]\w*)\s*(?:@|})""")
    }
}
