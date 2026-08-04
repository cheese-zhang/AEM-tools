package com.github.aemtoolkit.toolwindow.content

/**
 * Immutable representation of an XML node in an AEM content tree.
 */
data class AemContentNode(
    val name: String,
    val attributes: Map<String, String>,
    val sourceOffset: Int,
    val children: List<AemContentNode>,
) {
    /** Compact label used by the content tree. */
    val displayName: String
        get() {
            val resourceType = attributes["sling:resourceType"]
            return if (resourceType == null) name else "$name  [$resourceType]"
        }
}
