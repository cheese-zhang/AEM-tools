package com.github.aemtoolkit.config

import kotlin.test.Test
import kotlin.test.assertEquals

class AemConfigLexerTest {
    @Test
    fun `recognizes dispatcher and cnd constructs`() {
        val lexer = AemConfigLexer()
        lexer.start("/cache { }\n[app:Page]\n- app:title (string)")
        val tokens = buildList {
            while (lexer.tokenType != null) {
                if (lexer.tokenType != com.intellij.psi.TokenType.WHITE_SPACE) {
                    add(lexer.tokenType)
                }
                lexer.advance()
            }
        }

        assertEquals(
            listOf(
                AemConfigTokenTypes.DIRECTIVE,
                AemConfigTokenTypes.LEFT_CURLY,
                AemConfigTokenTypes.RIGHT_CURLY,
                AemConfigTokenTypes.NODE_TYPE,
                AemConfigTokenTypes.PROPERTY,
                AemConfigTokenTypes.LEFT_PAREN,
                AemConfigTokenTypes.IDENTIFIER,
                AemConfigTokenTypes.RIGHT_PAREN,
            ),
            tokens,
        )
    }
}
