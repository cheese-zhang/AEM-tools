package com.github.aemtoolkit.toolwindow.dialog

/**
 * Converts Granite's structural wrapper nodes into user-facing dialog items.
 */
object AemDialogPresentation {
    /** Returns the actual tabs below a Granite `tabs/items` structure. */
    fun tabItems(node: AemDialogNode): List<AemDialogNode> =
        node.children
            .firstOrNull { it.nodeName == "items" && it.resourceType == null }
            ?.children
            ?: node.children

    /** Flattens Granite `items` wrappers while retaining their sibling fields. */
    fun contentItems(node: AemDialogNode): List<AemDialogNode> =
        node.children.flatMap { child ->
            if (child.nodeName == "items" && child.resourceType == null) {
                child.children
            } else {
                listOf(child)
            }
        }

    /** Returns a concise label for a dialog tree node. */
    fun treeLabel(node: AemDialogNode, tabItem: Boolean = false): String =
        buildString {
            if (tabItem) append("Tab: ")
            append(node.label ?: node.fieldName ?: node.nodeName)
            if (node.label != null && node.fieldName != null) {
                append("  (").append(node.fieldName).append(')')
            }
            node.resourceType
                ?.substringAfterLast('/')
                ?.let { append("  [").append(it).append(']') }
        }
}
