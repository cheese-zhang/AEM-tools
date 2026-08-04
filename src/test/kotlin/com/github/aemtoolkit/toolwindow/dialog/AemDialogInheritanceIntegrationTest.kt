package com.github.aemtoolkit.toolwindow.dialog

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AemDialogInheritanceIntegrationTest : BasePlatformTestCase() {
    fun testResolvesLocalCoreComponentDialogSupertype() {
        myFixture.addFileToProject(
            "core.ui.apps/src/main/content/jcr_root/apps/core/wcm/components/image/v2/image/.content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                jcr:primaryType="cq:Component"/>
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "core.ui.apps/src/main/content/jcr_root/apps/core/wcm/components/image/v2/image/_cq_dialog/.content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                xmlns:sling="http://sling.apache.org/jcr/sling/1.0">
                <content>
                    <items>
                        <file fieldLabel="Image"
                            sling:resourceType="granite/ui/components/coral/foundation/form/fileupload"/>
                    </items>
                </content>
            </jcr:root>
            """.trimIndent(),
        )
        val overlay = myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/header/_cq_dialog/.content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                xmlns:sling="http://sling.apache.org/jcr/sling/1.0">
                <content>
                    <items>
                        <mobileLogo jcr:title="Mobile Logo"
                            sling:resourceType="granite/ui/components/coral/foundation/container"
                            sling:resourceSuperType="core/wcm/components/image/v2/image/cq:dialog/content">
                            <items>
                                <resourceType name="./mobileLogo/sling:resourceType"
                                    sling:resourceType="granite/ui/components/coral/foundation/form/hidden"/>
                            </items>
                        </mobileLogo>
                    </items>
                </content>
            </jcr:root>
            """.trimIndent(),
        )
        val parser = AemDialogStructureParser()
        val resolved = ReadAction.compute<AemDialogNode, RuntimeException> {
            val file = PsiManager.getInstance(project).findFile(overlay.virtualFile) as XmlFile
            AemDialogInheritanceResolver(project, parser).resolve(parser.parse(file)!!)
        }
        val mobileLogo = resolved.descendants().single { it.nodeName == "mobileLogo" }
        val items = mobileLogo.children.single { it.nodeName == "items" }

        assertTrue(mobileLogo.inheritanceResolved)
        assertEquals(
            listOf("file", "resourceType"),
            items.children.map(AemDialogNode::nodeName),
        )
    }

    private fun AemDialogNode.descendants(): Sequence<AemDialogNode> =
        sequenceOf(this) + children.asSequence().flatMap { it.descendants() }
}
