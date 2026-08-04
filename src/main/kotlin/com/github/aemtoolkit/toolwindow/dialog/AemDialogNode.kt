package com.github.aemtoolkit.toolwindow.dialog

/**
 * Granite UI node derived from an AEM dialog definition.
 */
data class AemDialogNode(
    val nodeName: String,
    val resourceType: String?,
    val fieldName: String?,
    val label: String?,
    val sourceOffset: Int,
    val children: List<AemDialogNode>,
    val resourceSuperType: String? = null,
    val inheritanceResolved: Boolean = true,
) {
    /** Human-readable hierarchy label. */
    val displayName: String
        get() = buildString {
            append(label ?: nodeName)
            fieldName?.let { append("  ($it)") }
            resourceType?.substringAfterLast('/')?.let { append("  [$it]") }
        }
}
