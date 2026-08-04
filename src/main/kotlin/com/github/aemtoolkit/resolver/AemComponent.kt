package com.github.aemtoolkit.resolver

import com.intellij.openapi.vfs.VirtualFile

/**
 * A component indexed from an AEM `/apps` content package.
 */
data class AemComponent(
    val name: String,
    val resourceType: String,
    val componentPath: String,
    val directory: VirtualFile,
    val dialog: VirtualFile?,
    val htl: VirtualFile?,
    val slingModel: VirtualFile?,
    val clientlibs: List<VirtualFile>,
)
