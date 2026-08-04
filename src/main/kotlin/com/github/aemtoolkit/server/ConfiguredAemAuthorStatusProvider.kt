package com.github.aemtoolkit.server

import com.github.aemtoolkit.resolver.AemAuthorStatus
import com.github.aemtoolkit.resolver.AemAuthorStatusProvider
import com.intellij.openapi.project.Project

/**
 * Loads Author metadata from the server configured for the project.
 */
class ConfiguredAemAuthorStatusProvider : AemAuthorStatusProvider {
    override fun getStatus(project: Project, repositoryPath: String): AemAuthorStatus? {
        if (!AemServerSettings.getInstance(project).state.enabled) return null
        val json = AemServerConnectionService.getInstance(project)
            .getRepositoryJson(repositoryPath)
        return AemAuthorStatus(
            publishStatus = AemJsonFields.read(json, "cq:lastReplicationAction"),
            workflowStatus = AemJsonFields.read(json, "cq:workflowStatus")
                ?: AemJsonFields.read(json, "workflowStatus"),
            lastModified = AemJsonFields.read(json, "jcr:lastModified"),
            version = AemJsonFields.read(json, "jcr:versionName")
                ?: AemJsonFields.read(json, "cq:lastRolledout"),
            liveCopy = AemJsonFields.read(json, "cq:isLiveRelationship")
                ?: AemJsonFields.read(json, "cq:LiveSyncConfig"),
        )
    }
}
