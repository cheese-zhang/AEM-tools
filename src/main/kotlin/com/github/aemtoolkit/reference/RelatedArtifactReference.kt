package com.github.aemtoolkit.reference

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.xml.XmlAttributeValue

/**
 * Exposes a component-owned Dialog, HTL, Sling Model, or Clientlib as a reference target.
 */
class RelatedArtifactReference(
    element: XmlAttributeValue,
    private val target: VirtualFile,
) : PsiReferenceBase<XmlAttributeValue>(
    element,
    TextRange(1, element.textLength - 1),
    true,
) {
    override fun resolve(): PsiElement? {
        val manager = PsiManager.getInstance(element.project)
        return if (target.isDirectory) {
            manager.findDirectory(target)
        } else {
            manager.findFile(target)
        }
    }
}
