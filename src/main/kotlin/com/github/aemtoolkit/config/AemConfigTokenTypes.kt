package com.github.aemtoolkit.config

import com.intellij.psi.tree.IElementType

/** Token types shared by CND and Dispatcher syntax highlighting. */
object AemConfigTokenTypes {
    val COMMENT = IElementType("AEM_CONFIG_COMMENT", AemConfigLanguage)
    val STRING = IElementType("AEM_CONFIG_STRING", AemConfigLanguage)
    val NUMBER = IElementType("AEM_CONFIG_NUMBER", AemConfigLanguage)
    val DIRECTIVE = IElementType("AEM_CONFIG_DIRECTIVE", AemConfigLanguage)
    val NODE_TYPE = IElementType("AEM_CONFIG_NODE_TYPE", AemConfigLanguage)
    val PROPERTY = IElementType("AEM_CONFIG_PROPERTY", AemConfigLanguage)
    val IDENTIFIER = IElementType("AEM_CONFIG_IDENTIFIER", AemConfigLanguage)
    val LEFT_CURLY = IElementType("AEM_CONFIG_LEFT_CURLY", AemConfigLanguage)
    val RIGHT_CURLY = IElementType("AEM_CONFIG_RIGHT_CURLY", AemConfigLanguage)
    val LEFT_BRACKET = IElementType("AEM_CONFIG_LEFT_BRACKET", AemConfigLanguage)
    val RIGHT_BRACKET = IElementType("AEM_CONFIG_RIGHT_BRACKET", AemConfigLanguage)
    val LEFT_PAREN = IElementType("AEM_CONFIG_LEFT_PAREN", AemConfigLanguage)
    val RIGHT_PAREN = IElementType("AEM_CONFIG_RIGHT_PAREN", AemConfigLanguage)
    val OPERATOR = IElementType("AEM_CONFIG_OPERATOR", AemConfigLanguage)
}
