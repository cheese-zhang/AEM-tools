package com.github.aemtoolkit.config

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Lightweight lexer covering CND declarations and Dispatcher directives.
 */
class AemConfigLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var endOffset = 0
    private var position = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null

    override fun start(
        buffer: CharSequence,
        startOffset: Int,
        endOffset: Int,
        initialState: Int,
    ) {
        this.buffer = buffer
        this.endOffset = endOffset
        position = startOffset
        locateToken()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        position = tokenEnd
        locateToken()
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun locateToken() {
        if (position >= endOffset) {
            tokenType = null
            tokenStart = endOffset
            tokenEnd = endOffset
            return
        }
        tokenStart = position
        val character = buffer[position]
        when {
            character.isWhitespace() -> consumeWhile(Char::isWhitespace, TokenType.WHITE_SPACE)
            character == '#' -> consumeUntilLineEnd(AemConfigTokenTypes.COMMENT)
            character == '"' || character == '\'' -> consumeString(character)
            character == '/' -> consumeWhile(
                { it.isLetterOrDigit() || it in "_-/" },
                AemConfigTokenTypes.DIRECTIVE,
            )
            character == '[' && looksLikeNodeTypeDeclaration() ->
                consumeDelimited(']', AemConfigTokenTypes.NODE_TYPE)
            character == '-' && isLinePrefix() -> consumeProperty()
            character.isDigit() -> consumeWhile(
                { it.isDigit() || it == '.' },
                AemConfigTokenTypes.NUMBER,
            )
            character == '{' -> consumeSingle(AemConfigTokenTypes.LEFT_CURLY)
            character == '}' -> consumeSingle(AemConfigTokenTypes.RIGHT_CURLY)
            character == '[' -> consumeSingle(AemConfigTokenTypes.LEFT_BRACKET)
            character == ']' -> consumeSingle(AemConfigTokenTypes.RIGHT_BRACKET)
            character == '(' -> consumeSingle(AemConfigTokenTypes.LEFT_PAREN)
            character == ')' -> consumeSingle(AemConfigTokenTypes.RIGHT_PAREN)
            character in "<>" -> consumeSingle(AemConfigTokenTypes.OPERATOR)
            character in "=,+-" -> consumeSingle(AemConfigTokenTypes.OPERATOR)
            character.isLetter() || character in "_:" -> consumeWhile(
                { it.isLetterOrDigit() || it in "_:.-" },
                AemConfigTokenTypes.IDENTIFIER,
            )
            else -> consumeSingle(TokenType.BAD_CHARACTER)
        }
    }

    private fun consumeWhile(
        predicate: (Char) -> Boolean,
        type: IElementType,
    ) {
        var cursor = position
        while (cursor < endOffset && predicate(buffer[cursor])) cursor++
        tokenEnd = cursor
        tokenType = type
    }

    private fun consumeUntilLineEnd(type: IElementType) {
        var cursor = position
        while (cursor < endOffset && buffer[cursor] !in "\r\n") cursor++
        tokenEnd = cursor
        tokenType = type
    }

    private fun consumeString(quote: Char) {
        var cursor = position + 1
        var escaped = false
        while (cursor < endOffset) {
            val current = buffer[cursor++]
            if (current == quote && !escaped) break
            escaped = current == '\\' && !escaped
            if (current != '\\') escaped = false
        }
        tokenEnd = cursor
        tokenType = AemConfigTokenTypes.STRING
    }

    private fun consumeDelimited(close: Char, type: IElementType) {
        var cursor = position + 1
        while (cursor < endOffset && buffer[cursor] != close) cursor++
        if (cursor < endOffset) cursor++
        tokenEnd = cursor
        tokenType = type
    }

    private fun consumeProperty() {
        var cursor = position + 1
        while (cursor < endOffset && buffer[cursor].isWhitespace()) cursor++
        while (cursor < endOffset &&
            (buffer[cursor].isLetterOrDigit() || buffer[cursor] in "_:.*-")
        ) {
            cursor++
        }
        tokenEnd = cursor
        tokenType = AemConfigTokenTypes.PROPERTY
    }

    private fun consumeSingle(type: IElementType) {
        tokenEnd = position + 1
        tokenType = type
    }

    private fun isLinePrefix(): Boolean {
        var cursor = position - 1
        while (cursor >= 0 && buffer[cursor] !in "\r\n") {
            if (!buffer[cursor].isWhitespace()) return false
            cursor--
        }
        return true
    }

    private fun looksLikeNodeTypeDeclaration(): Boolean =
        isLinePrefix() && buffer.indexOf(']', position + 1) in (position + 2)..<endOffset
}
