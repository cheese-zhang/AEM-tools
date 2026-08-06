package com.github.aemtoolkit.completion

import com.github.aemtoolkit.clientlib.ClientLibraryManifestSupport
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

/** Completes files and `#base` directories in client library manifests. */
class ClientLibraryManifestCompletionContributor : CompletionContributor() {
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
                    val manifest = parameters.originalFile.virtualFile
                    if (!ClientLibraryManifestSupport.isManifest(manifest)) return
                    val prefix = ClientLibraryManifestSupport.currentLinePrefix(
                        parameters.originalFile.text,
                        parameters.offset,
                    )
                    if (prefix.startsWith("#base=")) {
                        val directoryPrefix = prefix.substringAfter("#base=")
                        val baseResult = result.withPrefixMatcher(directoryPrefix)
                        ClientLibraryManifestSupport.baseCandidates(manifest).forEach {
                            baseResult.addElement(
                                LookupElementBuilder.create(it).withTypeText("ClientLib base", true),
                            )
                        }
                        return
                    }
                    if (prefix.startsWith('#')) return
                    val includeResult = result.withPrefixMatcher(prefix)
                    val base = ClientLibraryManifestSupport.basePath(
                        parameters.originalFile.text,
                        parameters.offset,
                    )
                    ClientLibraryManifestSupport.includeCandidates(manifest, base).forEach {
                        includeResult.addElement(
                            LookupElementBuilder.create(it)
                                .withTypeText("ClientLib ${ClientLibraryManifestSupport.extension(manifest)}", true),
                        )
                    }
                }
            },
        )
    }
}
