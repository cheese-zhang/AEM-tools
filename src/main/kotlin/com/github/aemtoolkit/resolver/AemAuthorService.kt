package com.github.aemtoolkit.resolver

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Delegates Author requests to registered connection providers.
 */
@Service(Service.Level.PROJECT)
class AemAuthorService(private val project: Project) {
    /** Returns the first provider status available for [repositoryPath]. */
    fun getStatus(repositoryPath: String): AemAuthorStatus? =
        AemAuthorStatusProvider.EP_NAME.extensionList
            .firstNotNullOfOrNull { it.getStatus(project, repositoryPath) }

    companion object {
        /** Returns the project-level Author service. */
        fun getInstance(project: Project): AemAuthorService = project.service()
    }
}
