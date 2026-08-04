package com.github.aemtoolkit.completion

import com.github.aemtoolkit.resolver.JcrDefinitionKind
import com.github.aemtoolkit.resolver.JcrSchemaService
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

/**
 * Provides baseline completion for Dispatcher, CND, and Felix config files.
 */
class AemConfigCompletionContributor : CompletionContributor() {
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
                    val fileName = parameters.originalFile.name
                    when {
                        fileName.endsWith(".any", true) ||
                            fileName.endsWith(".farm", true) ->
                            addEntries(result, DISPATCHER_DIRECTIVES, "Dispatcher directive")
                        fileName.endsWith(".cnd", true) ->
                            JcrSchemaService.getInstance(parameters.position.project)
                                .allDefinitions()
                                .filter { it.kind == JcrDefinitionKind.NODE_TYPE }
                                .forEach { definition ->
                                    result.addElement(
                                        LookupElementBuilder.create(definition.name)
                                            .withTypeText("JCR node type", true),
                                    )
                                }
                        fileName.endsWith(".config", true) ->
                            addEntries(result, FELIX_TYPES, "Felix property type")
                    }
                }
            },
        )
    }

    private fun addEntries(
        result: CompletionResultSet,
        entries: Collection<String>,
        typeText: String,
    ) {
        entries.forEach { entry ->
            result.addElement(
                LookupElementBuilder.create(entry).withTypeText(typeText, true),
            )
        }
    }

    private companion object {
        val DISPATCHER_DIRECTIVES = listOf(
            "/farms",
            "/renders",
            "/filter",
            "/cache",
            "/rules",
            "/invalidate",
            "/allowedClients",
            "/clientheaders",
            "/ignoreUrlParams",
            "/virtualhosts",
            "/headers",
            "/timeout",
            "/gracePeriod",
            "/enableTTL",
        )
        val FELIX_TYPES = listOf("B", "I", "L", "F", "D", "S", "X")
    }
}
