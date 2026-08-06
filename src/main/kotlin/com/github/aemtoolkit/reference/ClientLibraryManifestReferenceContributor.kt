package com.github.aemtoolkit.reference

import com.github.aemtoolkit.clientlib.ClientLibraryManifestSupport
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext

/** Makes client library manifest includes and `#base` directives navigable. */
class ClientLibraryManifestReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext,
                ): Array<PsiReference> {
                    val file = element.containingFile?.virtualFile
                    if (!ClientLibraryManifestSupport.isManifest(file)) {
                        return PsiReference.EMPTY_ARRAY
                    }
                    return references(element)
                }
            },
        )
    }

    private fun references(element: PsiElement): Array<PsiReference> {
        val elementStart = element.textRange.startOffset
        return LINE.findAll(element.text).mapNotNull { match ->
            val raw = match.groups[1] ?: return@mapNotNull null
            val value = raw.value.trim()
            if (value.isEmpty()) return@mapNotNull null
            val leading = raw.value.indexOf(value)
            val range = TextRange(
                raw.range.first + leading,
                raw.range.first + leading + value.length,
            )
            if (value.startsWith("#base=")) {
                ClientLibraryManifestReference(
                    element,
                    TextRange(range.startOffset + "#base=".length, range.endOffset),
                    value.substringAfter("#base="),
                    elementStart + range.startOffset,
                    true,
                )
            } else if (!value.startsWith('#')) {
                ClientLibraryManifestReference(
                    element,
                    range,
                    value,
                    elementStart + range.startOffset,
                    false,
                )
            } else {
                null
            }
        }.toList().toTypedArray()
    }

    private companion object {
        val LINE = Regex("""(?m)^([^\r\n]*)$""")
    }
}

private class ClientLibraryManifestReference(
    element: PsiElement,
    range: TextRange,
    private val value: String,
    private val fileOffset: Int,
    private val baseDirective: Boolean,
) : PsiReferenceBase<PsiElement>(element, range, true) {
    override fun resolve(): PsiElement? {
        val file = element.containingFile.virtualFile ?: return null
        val target = if (baseDirective) {
            ClientLibraryManifestSupport.resolveBase(file, value)
        } else {
            ClientLibraryManifestSupport.resolveInclude(file, value, fileOffset)
        } ?: return null
        return PsiManager.getInstance(element.project).findFile(target)
            ?: PsiManager.getInstance(element.project).findDirectory(target)
    }
}
