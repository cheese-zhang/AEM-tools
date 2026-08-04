package com.github.aemtoolkit.config

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * Creates a stable PSI file while preserving lexer tokens for editor services.
 */
class AemConfigParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = AemConfigLexer()

    override fun createParser(project: Project?): PsiParser =
        PsiParser { root, builder ->
            val rootMarker = builder.mark()
            while (!builder.eof()) {
                when (builder.tokenType) {
                    AemConfigTokenTypes.NODE_TYPE -> {
                        val declaration = builder.mark()
                        builder.advanceLexer()
                        declaration.done(AemConfigElementTypes.CND_NODE_TYPE_DECLARATION)
                    }
                    AemConfigTokenTypes.PROPERTY -> {
                        val declaration = builder.mark()
                        builder.advanceLexer()
                        declaration.done(AemConfigElementTypes.CND_PROPERTY_DECLARATION)
                    }
                    else -> builder.advanceLexer()
                }
            }
            rootMarker.done(root)
            builder.treeBuilt
        }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getWhitespaceTokens(): TokenSet = TokenSet.create(TokenType.WHITE_SPACE)

    override fun getCommentTokens(): TokenSet = TokenSet.create(AemConfigTokenTypes.COMMENT)

    override fun getStringLiteralElements(): TokenSet =
        TokenSet.create(AemConfigTokenTypes.STRING)

    override fun createElement(node: ASTNode): PsiElement =
        when (node.elementType) {
            AemConfigElementTypes.CND_NODE_TYPE_DECLARATION,
            AemConfigElementTypes.CND_PROPERTY_DECLARATION,
            -> CndNamedElement(node)
            else -> ASTWrapperPsiElement(node)
        }

    override fun createFile(viewProvider: FileViewProvider): PsiFile =
        object : PsiFileBase(viewProvider, AemConfigLanguage) {
            override fun getFileType() = AemConfigFileType()

            override fun toString(): String = "AEM configuration file"
        }

    private companion object {
        val FILE = IFileElementType(AemConfigLanguage)
    }
}
