package com.github.aemtoolkit.completion

import com.github.aemtoolkit.util.HtlUtil
import com.github.aemtoolkit.resolver.HtlJavaModelResolver
import com.github.aemtoolkit.resolver.HtlUseTemplateResolver
import com.github.aemtoolkit.clientlib.AemClientLibraryService
import com.github.aemtoolkit.i18n.AemI18nService
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
        val i18nPrefix = HtlCompletionContext.i18nPrefix(
            parameters.originalFile.text,
            parameters.offset,
        )
        if (i18nPrefix != null) {
            val i18nResult = result.withPrefixMatcher(i18nPrefix)
            AemI18nService.getInstance(position.project).keys().forEach { key ->
                val entry = AemI18nService.getInstance(position.project).find(key).firstOrNull()
                i18nResult.addElement(
                    LookupElementBuilder.create(key)
                        .withTypeText(entry?.message ?: "AEM translation", true),
                )
            }
            return
        }
        val categoryPrefix = ClientLibraryCompletionContext.htlCategoryPrefix(
            parameters.originalFile.text,
            parameters.offset,
        )
        if (categoryPrefix != null) {
            addClientLibraryCategories(
                position,
                result.withPrefixMatcher(categoryPrefix),
            )
            return
        }
        val modelAccess = HtlCompletionContext.modelAccess(
            parameters.originalFile.text,
            parameters.offset,
        )
        if (modelAccess != null) {
            val propertyResult = result.withPrefixMatcher(modelAccess.propertyPrefix)
            HtlJavaModelResolver.properties(
                position,
                modelAccess.variable,
                modelAccess.propertyChain,
            ).forEach { property ->
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
        if (value != null &&
            attribute.name.startsWith("data-sly-use.") &&
            PsiTreeUtil.isAncestor(value, position, false)
        ) {
            addUseObjects(
                position,
                result.withPrefixMatcher(
                    HtlCompletionContext.useClassPrefix(
                        parameters.originalFile.text,
                        parameters.offset,
                    ),
                ),
            )
            return
        }
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

    private fun addUseObjects(position: PsiElement, result: CompletionResultSet) {
        HtlJavaModelResolver.availableModelClasses(position.project).forEach { type ->
            val qualifiedName = type.qualifiedName ?: return@forEach
            result.addElement(
                LookupElementBuilder.create(qualifiedName)
                    .withPresentableText(type.name ?: qualifiedName)
                    .withTailText("  $qualifiedName")
                    .withTypeText("HTL model", true),
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

    private fun addClientLibraryCategories(
        position: PsiElement,
        result: CompletionResultSet,
    ) {
        AemClientLibraryService.getInstance(position.project).categories().forEach { category ->
            val libraries = AemClientLibraryService.getInstance(position.project)
                .findByCategory(category)
            result.addElement(
                LookupElementBuilder.create(category)
                    .withTypeText(
                        libraries.firstOrNull()?.repositoryPath ?: "AEM client library",
                        true,
                    ),
            )
        }
        val source = position.containingFile.virtualFile ?: return
        HtlUseTemplateResolver.candidates(position.project, source).forEach { template ->
            result.addElement(
                    LookupElementBuilder.create(template.lookupString)
                        .withPresentableText(template.lookupString)
                        .withTypeText("HTL template", true)
                        .withTailText("  ${template.location}"),
            )
        }
    }
}

/** Extracts the active HTL expression at the completion caret. */
internal object HtlCompletionContext {
    fun i18nPrefix(text: String, offset: Int): String? {
        val beforeCaret = textBeforeCaret(text, offset)
        Regex("""i18n\s*\[\s*['"]([^'"]*)$""")
            .find(beforeCaret)
            ?.let { return it.groupValues[1] }
        val standard = Regex("""(?<!\\)\$\{\s*['"]([^'"]*)$""")
            .find(beforeCaret)
            ?: return null
        val afterCaret = text.substring(offset.coerceIn(0, text.length))
            .substringBefore('}')
        return standard.groupValues[1].takeIf {
            Regex("""['"]\s*@\s*i18n\b""").containsMatchIn(afterCaret)
        }
    }

    fun useClassPrefix(text: String, offset: Int): String =
        Regex(
            """data-sly-use\.[\w-]+\s*=\s*["'](?:\$\{\s*['"])?([^"'{}]*)$""",
        ).find(textBeforeCaret(text, offset))
            ?.groupValues
            ?.get(1)
            .orEmpty()

    fun modelAccess(text: String, offset: Int): HtlModelAccess? {
        val match = Regex("""\$\{\s*([A-Za-z_]\w*)\.([A-Za-z0-9_.]*)$""")
            .find(textBeforeCaret(text, offset))
            ?: return null
        val segments = match.groupValues[2].split('.')
        return HtlModelAccess(
            variable = match.groupValues[1],
            propertyPrefix = segments.last(),
            propertyChain = segments.dropLast(1),
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
    val propertyChain: List<String> = emptyList(),
)
