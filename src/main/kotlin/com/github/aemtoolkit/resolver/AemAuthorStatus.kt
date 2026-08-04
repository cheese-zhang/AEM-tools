package com.github.aemtoolkit.resolver

/**
 * Author-environment metadata for an AEM repository resource.
 */
data class AemAuthorStatus(
    val publishStatus: String?,
    val workflowStatus: String?,
    val lastModified: String?,
    val version: String?,
    val liveCopy: String?,
)
