package com.github.aemtoolkit.toolwindow.content

import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

/**
 * Converts nested `.content.xml` tags into a UI-independent hierarchy.
 */
class AemContentTreeParser {
    /** Parses [file], returning null when it is not an AEM content XML file. */
    fun parse(file: XmlFile): AemContentNode? {
        if (!AemXmlUtil.isContentXml(file)) return null
        return file.rootTag?.let(::parseTag)
    }

    private fun parseTag(tag: XmlTag): AemContentNode =
        AemContentNode(
            name = tag.localName,
            attributes = tag.attributes
                .mapNotNull { attribute ->
                    attribute.value?.let { attribute.name to it }
                }
                .toMap(),
            sourceOffset = tag.textOffset,
            children = tag.subTags.map(::parseTag),
        )
}
