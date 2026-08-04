package com.github.aemtoolkit.config

import com.intellij.psi.tree.IElementType

/** Composite PSI element types created by the AEM configuration parser. */
object AemConfigElementTypes {
    val CND_NODE_TYPE_DECLARATION =
        IElementType("CND_NODE_TYPE_DECLARATION", AemConfigLanguage)
    val CND_PROPERTY_DECLARATION =
        IElementType("CND_PROPERTY_DECLARATION", AemConfigLanguage)
}
