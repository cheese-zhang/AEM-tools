package com.github.aemtoolkit.clientlib

import com.intellij.openapi.vfs.VirtualFile

/** A FileVault `cq:ClientLibraryFolder` and its category relationships. */
data class AemClientLibrary(
    val repositoryPath: String,
    val directory: VirtualFile,
    val contentXml: VirtualFile,
    val categories: List<String>,
    val dependencies: List<String>,
    val embeds: List<String>,
)
