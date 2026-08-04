package com.github.aemtoolkit.resolver

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

/**
 * Extension point for secure, deployment-specific AEM Author connections.
 */
interface AemAuthorStatusProvider {
    /** Loads Author metadata for [repositoryPath], or null when not configured. */
    fun getStatus(project: Project, repositoryPath: String): AemAuthorStatus?

    companion object {
        val EP_NAME: ExtensionPointName<AemAuthorStatusProvider> =
            ExtensionPointName.create("com.github.aemtoolkit.authorStatusProvider")
    }
}
