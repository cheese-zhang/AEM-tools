package com.github.aemtoolkit.server

import org.junit.Test
import java.nio.file.Files
import kotlin.test.assertEquals

class AemContentXmlParserTest {
    @Test
    fun `maps content XML to recursive JCR nodes`() {
        val file = Files.createTempFile("aem-content-", ".xml")
        Files.writeString(
            file,
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
                jcr:primaryType="nt:unstructured"
                sling:resourceType="example/components/card"
                enabled="{Boolean}true"
                cq:styleIds="{Long}[1001,1002]"
                xmlns:cq="http://www.day.com/jcr/cq/1.0">
                <content jcr:primaryType="nt:unstructured" title="Card"/>
            </jcr:root>
            """.trimIndent(),
        )

        val root = AemContentXmlParser.parse(file, "/content/example/card")

        assertEquals("/content/example/card", root.repositoryPath)
        assertEquals(
            AemContentXmlProperty("enabled", listOf("true"), "Boolean"),
            root.properties.single { it.name == "enabled" },
        )
        assertEquals(
            AemContentXmlProperty("cq:styleIds", listOf("1001", "1002"), "Long"),
            root.properties.single { it.name == "cq:styleIds" },
        )
        assertEquals(
            listOf("/content/example/card", "/content/example/card/content"),
            root.flatten().map(AemContentXmlNode::repositoryPath).toList(),
        )
    }

    @Test
    fun `preserves escaped commas in multi value properties`() {
        val file = Files.createTempFile("aem-content-array-", ".xml")
        Files.writeString(
            file,
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                jcr:primaryType="nt:unstructured"
                values="[first\,value,second]"/>
            """.trimIndent(),
        )

        val root = AemContentXmlParser.parse(file, "/content/example")

        assertEquals(
            listOf("first,value", "second"),
            root.properties.single { it.name == "values" }.values,
        )
    }
}
