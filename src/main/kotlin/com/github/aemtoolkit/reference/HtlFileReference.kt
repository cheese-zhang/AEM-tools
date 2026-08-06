package com.github.aemtoolkit.reference

import com.github.aemtoolkit.resolver.AemRepositoryPath
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlAttributeValue

/**
 * Resolves an HTL use/include path relative to the current script.
 */
class HtlFileReference(
    element: XmlAttributeValue,
    range: TextRange,
    private val relativePath: String,
) : PsiReferenceBase<XmlAttributeValue>(element, range, true) {
    override fun resolve(): PsiElement? {
        val source = element.containingFile.virtualFile ?: return null
        val target = if (relativePath.startsWith('/')) {
            FilenameIndex.getVirtualFilesByName(
                relativePath.substringAfterLast('/'),
                GlobalSearchScope.projectScope(element.project),
            ).firstOrNull {
                AemRepositoryPath.fromFilePath(it.path) == relativePath
            }
        } else {
            source.parent?.findFileByRelativePath(relativePath)
        } ?: return null
        return PsiManager.getInstance(element.project).findFile(target)
    }
}
