package com.github.aemtoolkit.util

import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

/**
 * Shared PSI helpers for AEM `.content.xml` files.
 */
object AemXmlUtil {
    const val CONTENT_XML = ".content.xml"
    const val RESOURCE_TYPE = "sling:resourceType"
    const val TEMPLATE = "cq:template"
    const val POLICY = "cq:policy"
    const val STYLE_IDS = "cq:styleIds"
    const val PRIMARY_TYPE = "jcr:primaryType"

    val supportedAttributes = setOf(RESOURCE_TYPE, TEMPLATE, POLICY, STYLE_IDS, PRIMARY_TYPE)

    /** Returns true when [file] is an AEM content XML file. */
    fun isContentXml(file: XmlFile): Boolean = file.name == CONTENT_XML

    /** Returns the XML attribute containing [element], if any. */
    fun containingAttribute(element: PsiElement?): XmlAttribute? = when (element) {
        is XmlAttribute -> element
        is XmlAttributeValue -> element.parent as? XmlAttribute
        else -> element?.parent as? XmlAttribute
            ?: element?.parent?.parent as? XmlAttribute
    }

    /** Reads the supported AEM attributes declared on [tag]. */
    fun readAemAttributes(tag: XmlTag): Map<String, String> =
        tag.attributes
            .filter { it.name in supportedAttributes }
            .mapNotNull { attribute -> attribute.value?.let { attribute.name to it } }
            .toMap()

    /** Returns true when [attribute] is a resource type in `.content.xml`. */
    fun isResourceType(attribute: XmlAttribute): Boolean =
        attribute.name == RESOURCE_TYPE &&
            (attribute.containingFile as? XmlFile)?.let(::isContentXml) == true

    /** Returns true for repository-path attributes supported by AEM navigation. */
    fun isRepositoryPath(attribute: XmlAttribute): Boolean =
        attribute.name in setOf(TEMPLATE, POLICY) &&
            (attribute.containingFile as? XmlFile)?.let(::isContentXml) == true
}
