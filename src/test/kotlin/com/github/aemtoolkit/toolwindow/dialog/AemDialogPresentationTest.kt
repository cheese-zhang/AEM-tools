package com.github.aemtoolkit.toolwindow.dialog

import kotlin.test.Test
import kotlin.test.assertEquals

class AemDialogPresentationTest {
    @Test
    fun `unwraps Granite tabs items and labels tabs`() {
        val title = node("title", label = "Title")
        val general = node(
            "general",
            label = "General",
            children = listOf(node("items", children = listOf(title))),
        )
        val advanced = node("advanced", fieldName = "./advanced")
        val tabs = node(
            "tabs",
            resourceType = "granite/ui/components/coral/foundation/tabs",
            children = listOf(node("items", children = listOf(general, advanced))),
        )

        assertEquals(listOf(general, advanced), AemDialogPresentation.tabItems(tabs))
        assertEquals("Tab: General", AemDialogPresentation.treeLabel(general, true))
        assertEquals("Tab: ./advanced", AemDialogPresentation.treeLabel(advanced, true))
        assertEquals(listOf(title), AemDialogPresentation.contentItems(general))
    }

    @Test
    fun `flattens items without dropping siblings`() {
        val sibling = node("sibling", label = "Sibling")
        val wrapped = node("wrapped", label = "Wrapped")
        val container = node(
            "container",
            children = listOf(
                sibling,
                node("items", children = listOf(wrapped)),
            ),
        )

        assertEquals(
            listOf(sibling, wrapped),
            AemDialogPresentation.contentItems(container),
        )
    }

    @Test
    fun `shows node attributes as structure properties`() {
        val link = AemDialogNode(
            nodeName = "linkURL",
            resourceType = null,
            fieldName = "./mobileLogo/linkURL",
            label = "Link to the Homepage",
            sourceOffset = 0,
            children = emptyList(),
            attributes = linkedMapOf(
                "jcr:primaryType" to "nt:unstructured",
                "fieldLabel" to "Link to the Homepage",
                "name" to "./mobileLogo/linkURL",
            ),
        )

        assertEquals(
            listOf(
                "@jcr:primaryType = nt:unstructured",
                "@fieldLabel = Link to the Homepage",
                "@name = ./mobileLogo/linkURL",
            ),
            AemDialogPresentation.treeProperties(link),
        )
    }

    private fun node(
        name: String,
        resourceType: String? = null,
        fieldName: String? = null,
        label: String? = null,
        children: List<AemDialogNode> = emptyList(),
    ) = AemDialogNode(name, resourceType, fieldName, label, 0, children)
}
