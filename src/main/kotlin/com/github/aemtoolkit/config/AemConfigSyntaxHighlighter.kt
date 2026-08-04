package com.github.aemtoolkit.config

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/** Syntax highlighting for CND and Dispatcher configuration. */
class AemConfigSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = AemConfigLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType) {
            AemConfigTokenTypes.COMMENT -> COMMENT
            AemConfigTokenTypes.STRING -> STRING
            AemConfigTokenTypes.NUMBER -> NUMBER
            AemConfigTokenTypes.DIRECTIVE -> KEYWORD
            AemConfigTokenTypes.NODE_TYPE -> TYPE
            AemConfigTokenTypes.PROPERTY -> PROPERTY
            AemConfigTokenTypes.LEFT_CURLY,
            AemConfigTokenTypes.RIGHT_CURLY,
            AemConfigTokenTypes.LEFT_BRACKET,
            AemConfigTokenTypes.RIGHT_BRACKET,
            AemConfigTokenTypes.LEFT_PAREN,
            AemConfigTokenTypes.RIGHT_PAREN,
            -> BRACES
            AemConfigTokenTypes.OPERATOR -> OPERATOR
            TokenType.BAD_CHARACTER -> BAD
            else -> EMPTY
        }

    private companion object {
        val COMMENT = pack(DefaultLanguageHighlighterColors.LINE_COMMENT)
        val STRING = pack(DefaultLanguageHighlighterColors.STRING)
        val NUMBER = pack(DefaultLanguageHighlighterColors.NUMBER)
        val KEYWORD = pack(DefaultLanguageHighlighterColors.KEYWORD)
        val TYPE = pack(DefaultLanguageHighlighterColors.CLASS_NAME)
        val PROPERTY = pack(DefaultLanguageHighlighterColors.INSTANCE_FIELD)
        val BRACES = pack(DefaultLanguageHighlighterColors.BRACES)
        val OPERATOR = pack(DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val BAD = pack(DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE)
        val EMPTY = emptyArray<TextAttributesKey>()
    }
}
