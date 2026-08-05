package com.github.aemtoolkit.toolwindow.dialog

import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AemDialogStructureParserTest : BasePlatformTestCase() {
    fun testPreservesPropertiesForNodesWithoutResourceType() {
        val file = myFixture.configureByText(
            ".content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                xmlns:granite="http://www.adobe.com/jcr/granite/1.0"
                xmlns:cq="http://www.day.com/jcr/cq/1.0"
                jcr:primaryType="nt:unstructured">
                <linkURL
                    jcr:primaryType="nt:unstructured"
                    fieldLabel="Link to the Homepage"
                    name="./mobileLogo/linkURL">
                    <granite:data
                        jcr:primaryType="nt:unstructured"
                        cq-msm-lockable="./mobileLogo/linkURL"/>
                </linkURL>
                <id
                    jcr:primaryType="nt:unstructured"
                    name="./mobileLogo/id"/>
            </jcr:root>
            """.trimIndent(),
        ) as XmlFile

        val root = AemDialogStructureParser().parse(file)!!
        val link = root.children.single { it.nodeName == "linkURL" }
        val data = link.children.single { it.nodeName == "data" }
        val id = root.children.single { it.nodeName == "id" }

        assertEquals("Link to the Homepage", link.attributes["fieldLabel"])
        assertEquals("./mobileLogo/linkURL", link.attributes["name"])
        assertEquals("./mobileLogo/linkURL", data.attributes["cq-msm-lockable"])
        assertEquals("./mobileLogo/id", id.attributes["name"])
        assertFalse(root.attributes.keys.any { it.startsWith("xmlns") })
    }
}
