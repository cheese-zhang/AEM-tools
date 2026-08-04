package com.github.aemtoolkit.toolwindow.dialog

import java.awt.Container
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.JTextField
import javax.swing.JLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AemDialogPreviewRendererTest {
    @Test
    fun `renders tab fields without duplicate title and supports file upload`() {
        val upload = node(
            "mobileLogo",
            "granite/ui/components/coral/foundation/form/fileupload",
            label = "Mobile Logo",
        )
        val tab = node(
            "mobile",
            label = "Mobile Logo",
            children = listOf(node("items", children = listOf(upload))),
        )
        val tabs = node(
            "tabs",
            "granite/ui/components/coral/foundation/tabs",
            children = listOf(node("items", children = listOf(tab))),
        )

        val rendered = AemDialogPreviewRenderer.render(tabs) as JTabbedPane
        val content = rendered.getComponentAt(0) as JPanel

        assertEquals("Mobile Logo", rendered.getTitleAt(0))
        assertTrue(content.border !is javax.swing.border.TitledBorder)
        assertTrue(descendants(content).filterIsInstance<JButton>().any())
    }

    @Test
    fun `caps text field row height`() {
        val field = node(
            "company",
            "granite/ui/components/coral/foundation/form/textfield",
            label = "Company",
        )

        val rendered = AemDialogPreviewRenderer.render(field) as JPanel
        val input = descendants(rendered).filterIsInstance<JTextField>().single()

        assertTrue(rendered.maximumSize.height < 100)
        assertTrue(input.preferredSize.height < 100)
    }

    @Test
    fun `hides technical nodes and retains fields below transparent metadata`() {
        val hidden = node(
            "resourceType",
            "granite/ui/components/coral/foundation/form/hidden",
            fieldName = "./sling:resourceType",
        )
        val renderCondition = node(
            "rendercondition",
            "example/components/rendercondition/featureflag",
        )
        val field = node(
            "root",
            "granite/ui/components/coral/foundation/form/pathfield",
            label = "DAM Search Root",
        )
        val data = node("data", children = listOf(field))
        val tab = node(
            "search",
            label = "Search",
            children = listOf(node("items", children = listOf(hidden, renderCondition, data))),
        )
        val tabs = node(
            "tabs",
            "granite/ui/components/coral/foundation/tabs",
            children = listOf(node("items", children = listOf(tab))),
        )

        val rendered = AemDialogPreviewRenderer.render(tabs) as JTabbedPane
        val labels = descendants(rendered)
            .filterIsInstance<JLabel>()
            .map { it.text }
            .toList()

        assertTrue(labels.any { it == "DAM Search Root" })
        assertTrue(labels.none { it.contains("sling:resourceType") })
        assertTrue(labels.none { it.contains("rendercondition") })
        assertTrue(labels.none { it == "data" })
    }

    private fun descendants(container: Container): Sequence<java.awt.Component> =
        container.components.asSequence().flatMap { component ->
            sequenceOf(component) +
                if (component is Container) descendants(component) else emptySequence()
        }

    private fun node(
        name: String,
        resourceType: String? = null,
        fieldName: String? = null,
        label: String? = null,
        children: List<AemDialogNode> = emptyList(),
    ) = AemDialogNode(name, resourceType, fieldName, label, 0, children)
}
