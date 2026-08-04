package com.github.aemtoolkit.toolwindow.dialog

import kotlin.test.Test
import kotlin.test.assertEquals

class AemDialogInheritanceResolverTest {
    @Test
    fun `merges inherited items with local dialog overlays`() {
        val inheritedField = node("file", label = "Image")
        val inherited = node(
            "content",
            children = listOf(
                node("items", children = listOf(inheritedField)),
            ),
        )
        val localHidden = node(
            "resourceType",
            resourceType = "granite/ui/components/coral/foundation/form/hidden",
        )
        val overlay = node(
            "mobileLogo",
            resourceType = "granite/ui/components/coral/foundation/container",
            label = "Mobile Logo",
            children = listOf(
                node("items", children = listOf(localHidden)),
            ),
            resourceSuperType = "core/wcm/components/image/v2/image/cq:dialog/content",
        )

        val merged = AemDialogInheritanceResolver.merge(inherited, overlay)
        val items = merged.children.single { it.nodeName == "items" }

        assertEquals("mobileLogo", merged.nodeName)
        assertEquals("Mobile Logo", merged.label)
        assertEquals(
            listOf("file", "resourceType"),
            items.children.map(AemDialogNode::nodeName),
        )
    }

    private fun node(
        name: String,
        resourceType: String? = null,
        fieldName: String? = null,
        label: String? = null,
        children: List<AemDialogNode> = emptyList(),
        resourceSuperType: String? = null,
    ) = AemDialogNode(
        name,
        resourceType,
        fieldName,
        label,
        0,
        children,
        resourceSuperType,
    )
}
