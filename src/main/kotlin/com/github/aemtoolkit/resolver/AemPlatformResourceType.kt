package com.github.aemtoolkit.resolver

/**
 * Identifies resource types supplied by AEM itself rather than the current project.
 */
object AemPlatformResourceType {
    private val libsPrefixes = listOf(
        "granite/",
        "cq/",
        "wcm/",
        "foundation/",
    )

    /** Returns true when [resourceType] normally lives below AEM `/libs`. */
    fun isExternal(resourceType: String): Boolean =
        normalize(resourceType).let { normalized ->
            libsPrefixes.any(normalized::startsWith)
        }

    /** Returns the expected repository path for a platform resource type. */
    fun repositoryPath(resourceType: String): String? =
        normalize(resourceType)
            .takeIf(::isExternal)
            ?.let { "/libs/$it" }

    private fun normalize(resourceType: String): String =
        resourceType.removePrefix("/libs/").trim('/')
}
