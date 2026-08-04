package com.github.aemtoolkit.config

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

/** Matches structural delimiters in Dispatcher and CND files. */
class AemConfigBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> =
        arrayOf(
            BracePair(AemConfigTokenTypes.LEFT_CURLY, AemConfigTokenTypes.RIGHT_CURLY, true),
            BracePair(AemConfigTokenTypes.LEFT_BRACKET, AemConfigTokenTypes.RIGHT_BRACKET, false),
            BracePair(AemConfigTokenTypes.LEFT_PAREN, AemConfigTokenTypes.RIGHT_PAREN, false),
        )

    override fun isPairedBracesAllowedBeforeType(
        leftBraceType: IElementType,
        contextType: IElementType?,
    ): Boolean = true

    override fun getCodeConstructStart(
        file: PsiFile,
        openingBraceOffset: Int,
    ): Int = openingBraceOffset
}
