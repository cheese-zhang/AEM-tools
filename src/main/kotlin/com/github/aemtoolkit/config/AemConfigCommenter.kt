package com.github.aemtoolkit.config

import com.intellij.lang.Commenter

/** Enables line comment actions in AEM configuration files. */
class AemConfigCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "#"

    override fun getBlockCommentPrefix(): String? = null

    override fun getBlockCommentSuffix(): String? = null

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null
}
