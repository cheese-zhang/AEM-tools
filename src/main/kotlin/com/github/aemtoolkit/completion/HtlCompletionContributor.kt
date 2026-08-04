package com.github.aemtoolkit.completion

import com.github.aemtoolkit.util.HtlUtil
import com.github.aemtoolkit.resolver.HtlJavaModelResolver
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.ProcessingContext

/**
 * Completes HTL block statements and standard global objects.
 */
class HtlCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet,
                ) {
                    addHtlCompletions(parameters.position, result)
                }
            },
        )
    }

    private fun addHtlCompletions(position: PsiElement, result: CompletionResultSet) {
        if (!HtlUtil.isHtlFile(position.containingFile)) return
        val attribute = PsiTreeUtil.getParentOfType(position, XmlAttribute::class.java, false)
            ?: return
        val value = attribute.valueElement
        if (value != null && PsiTreeUtil.isAncestor(value, position, false)) {
            val variable = modelVariable(value)
            if (variable != null) {
                HtlJavaModelResolver.properties(value, variable).forEach { property ->
                    result.addElement(
                        LookupElementBuilder.create(property.name)
                            .withTypeText(property.member.containingClass?.name, true),
                    )
                }
                return
            }
            HtlUtil.globalObjects.forEach { (name, description) ->
                result.addElement(
                    LookupElementBuilder.create(name)
                        .withTypeText(description, true),
                )
            }
            return
        }

        HtlUtil.blockStatements.forEach { (name, description) ->
            result.addElement(
                LookupElementBuilder.create(name)
                    .withTypeText(description, true),
            )
        }
    }

    private fun modelVariable(value: XmlAttributeValue): String? {
        val beforeCaret = value.value.substringBefore("IntellijIdeaRulezzz")
        return Regex("""\$\{\s*([A-Za-z_]\w*)\.[A-Za-z_]*$""")
            .find(beforeCaret)
            ?.groupValues
            ?.get(1)
    }
}
