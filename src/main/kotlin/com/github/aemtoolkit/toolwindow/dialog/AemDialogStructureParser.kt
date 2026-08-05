package com.github.aemtoolkit.toolwindow.dialog

import com.github.aemtoolkit.toolwindow.content.AemContentNode
import com.github.aemtoolkit.toolwindow.content.AemContentTreeParser
import com.intellij.psi.xml.XmlFile

/**
 * Maps the shared content hierarchy into Granite UI dialog semantics.
 */
class AemDialogStructureParser(
    private val contentParser: AemContentTreeParser = AemContentTreeParser(),
) {
    /** Parses an AEM dialog `.content.xml`. */
    fun parse(file: XmlFile): AemDialogNode? =
        contentParser.parse(file)?.let(::mapNode)

    private fun mapNode(node: AemContentNode): AemDialogNode =
        AemDialogNode(
            nodeName = node.name,
            resourceType = node.attributes["sling:resourceType"],
            fieldName = node.attributes["name"],
            label = node.attributes["fieldLabel"]
                ?: node.attributes["jcr:title"]
                ?: node.attributes["text"],
            sourceOffset = node.sourceOffset,
            children = node.children.map(::mapNode),
            resourceSuperType = node.attributes["sling:resourceSuperType"],
            attributes = node.attributes.filterKeys { attribute ->
                attribute != "xmlns" && !attribute.startsWith("xmlns:")
            },
        )
}
