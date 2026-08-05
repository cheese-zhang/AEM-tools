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
                    addHtlCompletions(parameters, result)
                }
            },
        )
    }

    private fun addHtlCompletions(
        parameters: CompletionParameters,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        if (!HtlUtil.isHtlFile(parameters.originalFile)) return
        val modelAccess = HtlCompletionContext.modelAccess(
            parameters.originalFile.text,
            parameters.offset,
        )
        if (modelAccess != null) {
            val propertyResult = result.withPrefixMatcher(modelAccess.propertyPrefix)
            HtlJavaModelResolver.properties(position, modelAccess.variable).forEach { property ->
                propertyResult.addElement(
                    LookupElementBuilder.create(property.name)
                        .withTypeText(property.member.containingClass?.name, true),
                )
            }
            return
        }

        val attribute = PsiTreeUtil.getParentOfType(position, XmlAttribute::class.java, false)
        if (attribute == null) {
            if (HtlCompletionContext.isInsideExpression(
                    parameters.originalFile.text,
                    parameters.offset,
                )
            ) {
                addGlobalObjects(result)
            }
            return
        }
        val value = attribute.valueElement
        if (value != null && PsiTreeUtil.isAncestor(value, position, false)) {
            addGlobalObjects(result)
            return
        }

        HtlUtil.blockStatements.forEach { (name, description) ->
            result.addElement(
                LookupElementBuilder.create(name)
                    .withTypeText(description, true),
            )
        }
    }

    private fun addGlobalObjects(result: CompletionResultSet) {
        HtlUtil.globalObjects.forEach { (name, description) ->
            result.addElement(
                LookupElementBuilder.create(name)
                    .withTypeText(description, true),
            )
        }
    }

}

/** Extracts the active HTL expression at the completion caret. */
internal object HtlCompletionContext {
    fun modelAccess(text: String, offset: Int): HtlModelAccess? {
        val match = Regex("""\$\{\s*([A-Za-z_]\w*)\.([A-Za-z_]*)$""")
            .find(textBeforeCaret(text, offset))
            ?: return null
        return HtlModelAccess(
            variable = match.groupValues[1],
            propertyPrefix = match.groupValues[2],
        )
    }

    fun isInsideExpression(text: String, offset: Int): Boolean {
        val beforeCaret = textBeforeCaret(text, offset)
        return beforeCaret.lastIndexOf("\${") > beforeCaret.lastIndexOf('}')
    }

    private fun textBeforeCaret(text: String, offset: Int): String =
        text.substring(0, offset.coerceIn(0, text.length))
            .substringAfterLast('\n')
            .substringBefore("IntellijIdeaRulezzz")
}

/** The variable and property prefix currently being completed. */
internal data class HtlModelAccess(
    val variable: String,
    val propertyPrefix: String,
)
