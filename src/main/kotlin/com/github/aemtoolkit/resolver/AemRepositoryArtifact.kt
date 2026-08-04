package com.github.aemtoolkit.resolver

import com.intellij.openapi.vfs.VirtualFile

/** A navigable template, policy, or client library below `jcr_root`. */
data class AemRepositoryArtifact(
    val name: String,
    val repositoryPath: String,
    val directory: VirtualFile,
    val kind: AemRepositoryArtifactKind,
)

/** Repository artifact categories displayed by AEM Explorer. */
enum class AemRepositoryArtifactKind {
    TEMPLATE,
    POLICY,
    CLIENTLIB,
}
