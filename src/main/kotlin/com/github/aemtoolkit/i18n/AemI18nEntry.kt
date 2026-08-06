package com.github.aemtoolkit.i18n

import com.intellij.openapi.vfs.VirtualFile

/** A FileVault-backed AEM translation entry. */
data class AemI18nEntry(
    val key: String,
    val message: String,
    val language: String?,
    val file: VirtualFile,
)
