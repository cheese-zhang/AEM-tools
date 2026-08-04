package com.github.aemtoolkit.config

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil

/**
 * Renameable CND node type or property declaration.
 */
class CndNamedElement(node: ASTNode) : ASTWrapperPsiElement(node), PsiNameIdentifierOwner {
    /** Returns true for a CND node type declaration. */
    val isNodeType: Boolean
        get() = node.elementType == AemConfigElementTypes.CND_NODE_TYPE_DECLARATION

    override fun getName(): String? =
        if (isNodeType) {
            text.removePrefix("[").substringBefore(']').trim()
        } else {
            text.removePrefix("-")
                .trim()
                .takeWhile { !it.isWhitespace() && it != '(' }
        }.takeIf(String::isNotBlank)

    override fun getNameIdentifier(): PsiElement = this

    override fun setName(name: String): PsiElement {
        val declarationText = if (isNodeType) "[$name]" else "- $name (undefined)"
        val temporaryFile = PsiFileFactory.getInstance(project).createFileFromText(
            "rename.cnd",
            AemConfigFileType(),
            declarationText,
        )
        val replacement = PsiTreeUtil.findChildOfType(
            temporaryFile,
            CndNamedElement::class.java,
        ) ?: return this
        return replace(replacement)
    }

    override fun getTextOffset(): Int {
        val relativeOffset = if (isNodeType) {
            text.indexOf('[').takeIf { it >= 0 }?.plus(1) ?: 0
        } else {
            text.indexOfFirst { !it.isWhitespace() && it != '-' }
                .takeIf { it >= 0 } ?: 0
        }
        return textRange.startOffset + relativeOffset
    }
}
