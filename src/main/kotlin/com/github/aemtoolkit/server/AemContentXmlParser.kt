package com.github.aemtoolkit.server

import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Converts FileVault `.content.xml` files into JCR nodes suitable for Sling POST.
 */
object AemContentXmlParser {
    /** Parses [file] and maps its root element to [repositoryPath]. */
    fun parse(file: Path, repositoryPath: String): AemContentXmlNode {
        require(repositoryPath.startsWith('/')) { "Repository path must be absolute" }
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        val root = Files.newInputStream(file).use { input ->
            factory.newDocumentBuilder().parse(input).documentElement
        }
        return root.toNode(repositoryPath)
    }

    private fun Element.toNode(path: String): AemContentXmlNode {
        val properties = (0 until attributes.length).mapNotNull { index ->
            val attribute = attributes.item(index)
            if (attribute.namespaceURI == XMLConstants.XMLNS_ATTRIBUTE_NS_URI ||
                attribute.nodeName == "xmlns" ||
                attribute.nodeName.startsWith("xmlns:")
            ) {
                return@mapNotNull null
            }
            parseProperty(attribute.nodeName, attribute.nodeValue)
        }
        val children = (0 until childNodes.length).mapNotNull { index ->
            (childNodes.item(index) as? Element)?.let { child ->
                child.toNode("${path.trimEnd('/')}/${child.tagName}")
            }
        }
        return AemContentXmlNode(path, properties, children)
    }

    private fun parseProperty(name: String, rawValue: String): AemContentXmlProperty {
        val typed = TYPE_PREFIX.matchEntire(rawValue)
        val type = typed?.groupValues?.get(1)
        val value = typed?.groupValues?.get(2) ?: rawValue
        val values = if (value.startsWith('[') && value.endsWith(']')) {
            splitArray(value.substring(1, value.length - 1))
        } else {
            listOf(value)
        }
        return AemContentXmlProperty(name, values, type)
    }

    private fun splitArray(value: String): List<String> {
        if (value.isEmpty()) return emptyList()
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        value.forEach { character ->
            when {
                escaped -> {
                    current.append(character)
                    escaped = false
                }
                character == '\\' -> escaped = true
                character == ',' -> {
                    values += current.toString()
                    current.setLength(0)
                }
                else -> current.append(character)
            }
        }
        if (escaped) current.append('\\')
        values += current.toString()
        return values
    }

    private val TYPE_PREFIX = Regex("""^\{([^}]+)}(.*)$""", RegexOption.DOT_MATCHES_ALL)
}

/** A JCR node parsed from FileVault XML. */
data class AemContentXmlNode(
    val repositoryPath: String,
    val properties: List<AemContentXmlProperty>,
    val children: List<AemContentXmlNode>,
) {
    /** Returns this node and all descendants in parent-first order. */
    fun flatten(): Sequence<AemContentXmlNode> =
        sequenceOf(this) + children.asSequence().flatMap(AemContentXmlNode::flatten)
}

/** A property parsed from FileVault XML. */
data class AemContentXmlProperty(
    val name: String,
    val values: List<String>,
    val type: String?,
)
