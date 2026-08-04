package com.github.aemtoolkit.config

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet

/** Enables Find Usages and Rename for CND declarations. */
class CndFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner =
        DefaultWordsScanner(
            AemConfigLexer(),
            TokenSet.create(
                AemConfigTokenTypes.NODE_TYPE,
                AemConfigTokenTypes.PROPERTY,
                AemConfigTokenTypes.IDENTIFIER,
            ),
            TokenSet.create(AemConfigTokenTypes.COMMENT),
            TokenSet.create(AemConfigTokenTypes.STRING),
        )

    override fun canFindUsagesFor(element: PsiElement): Boolean =
        element is CndNamedElement

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String =
        if ((element as? CndNamedElement)?.isNodeType == true) {
            "CND node type"
        } else {
            "CND property"
        }

    override fun getDescriptiveName(element: PsiElement): String =
        (element as? CndNamedElement)?.name.orEmpty()

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        getDescriptiveName(element)
}
