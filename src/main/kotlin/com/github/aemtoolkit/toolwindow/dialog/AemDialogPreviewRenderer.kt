package com.github.aemtoolkit.toolwindow.dialog

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane

/** Renders a stable Swing approximation of Granite UI dialog components. */
object AemDialogPreviewRenderer {
    /** Renders [node] and its visible children. */
    fun render(node: AemDialogNode): Component =
        renderNode(node) ?: JBLabel("No authorable dialog fields found")

    private fun renderNode(node: AemDialogNode): Component? {
        val type = node.resourceType?.substringAfterLast('/').orEmpty()
        if (isTechnicalNode(node, type)) return null
        return when (type) {
            "tabs" -> renderTabs(node)
            "textfield", "pathfield", "pathbrowser", "numberfield", "datepicker",
            "colorfield", "tagfield" -> fieldRow(node, JBTextField())
            "textarea" -> fieldRow(
                node,
                JScrollPane(JBTextArea(3, 24)).apply {
                    horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                },
            )
            "select", "autocomplete", "radiogroup" ->
                fieldRow(node, JComboBox(arrayOf("Option")))
            "checkbox", "switch" -> fieldRow(node, JBCheckBox())
            "fileupload" -> fieldRow(
                node,
                JButton("Choose file...").apply { isEnabled = false },
            )
            "multifield" -> renderMultifield(node)
            "heading" -> JBLabel(node.label ?: node.nodeName).apply {
                font = font.deriveFont(font.style or java.awt.Font.BOLD)
            }
            "description", "text" -> JBLabel(node.label ?: node.nodeName)
            else -> {
                val children = AemDialogPresentation.contentItems(node)
                if (children.any(::isRenderable) || node.resourceSuperType != null) {
                    renderContainer(node)
                } else if (type.isNotBlank() || node.label != null) {
                    unsupportedField(node, type.ifBlank { "field" })
                } else {
                    null
                }
            }
        }
    }

    private fun renderTabs(node: AemDialogNode): Component =
        JTabbedPane().apply {
            tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
            val items = AemDialogPresentation.tabItems(node)
            val renderedItems = items.map { child ->
                child to renderContainer(child, showTitle = false)
            }
            renderedItems.forEach { (child, content) ->
                addTab(
                    child.label ?: child.fieldName ?: child.nodeName,
                    content,
                )
            }
            if (items.isEmpty()) {
                addTab("Empty", JBLabel("No Granite tab items found"))
            }
            val contentHeight = renderedItems.maxOfOrNull { (_, content) ->
                content.preferredSize.height
            } ?: JBUI.scale(120)
            preferredSize = Dimension(preferredSize.width, contentHeight + JBUI.scale(48))
        }

    private fun renderContainer(
        node: AemDialogNode,
        showTitle: Boolean = true,
    ): JPanel =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT
            if (showTitle && node.label != null) {
                border = BorderFactory.createTitledBorder(node.label)
            } else {
                border = JBUI.Borders.empty(4)
            }
            val children = AemDialogPresentation.contentItems(node)
                .mapNotNull(::renderNode)
            if (node.resourceSuperType != null && !node.inheritanceResolved) {
                add(
                    JBLabel(
                        "Inherited fields are unavailable locally: ${node.resourceSuperType}",
                    ),
                )
                if (children.isNotEmpty()) add(Box.createVerticalStrut(JBUI.scale(4)))
            }
            if (children.isEmpty()) {
                if (node.resourceSuperType == null || node.inheritanceResolved) {
                    add(JBLabel("No authorable fields in this tab"))
                }
            } else {
                children.forEachIndexed { index, child ->
                    if (index > 0) add(Box.createVerticalStrut(JBUI.scale(4)))
                    add(child)
                }
            }
        }

    private fun renderMultifield(node: AemDialogNode): Component =
        JPanel(BorderLayout()).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            border = BorderFactory.createTitledBorder(
                node.label ?: node.fieldName ?: node.nodeName,
            )
            val children = AemDialogPresentation.contentItems(node)
                .mapNotNull(::renderNode)
            add(
                if (children.isEmpty()) {
                    JBLabel("Multifield item")
                } else {
                    JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.Y_AXIS)
                        children.forEach { add(it) }
                    }
                },
                BorderLayout.CENTER,
            )
            capHeight(this)
        }

    private fun unsupportedField(node: AemDialogNode, type: String): Component =
        fieldRow(
            node,
            JBLabel(
                type.takeIf(String::isNotBlank)
                    ?.let { "[$it]" }
                    ?: "[container]",
            ),
        )

    private fun isRenderable(node: AemDialogNode): Boolean {
        val type = node.resourceType?.substringAfterLast('/').orEmpty()
        if (isTechnicalNode(node, type)) return false
        if (type.isNotBlank() || node.label != null) return true
        return AemDialogPresentation.contentItems(node).any(::isRenderable)
    }

    private fun isTechnicalNode(node: AemDialogNode, type: String): Boolean =
        type == "hidden" ||
            node.nodeName.equals("rendercondition", ignoreCase = true) ||
            type.contains("rendercondition", ignoreCase = true) ||
            (type == "featureflag" && node.nodeName.contains("condition", ignoreCase = true))

    private fun fieldRow(node: AemDialogNode, input: JComponent): JPanel =
        JPanel(GridBagLayout()).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.empty(2, 4)
            input.isEnabled = false
            val label = JBLabel(node.label ?: node.fieldName ?: node.nodeName).apply {
                toolTipText = text
                preferredSize = Dimension(
                    preferredSize.width.coerceAtLeast(JBUI.scale(180)),
                    preferredSize.height,
                )
            }
            add(
                label,
                GridBagConstraints().apply {
                    gridx = 0
                    anchor = GridBagConstraints.NORTHWEST
                    insets = Insets(2, 0, 2, JBUI.scale(12))
                },
            )
            add(
                input,
                GridBagConstraints().apply {
                    gridx = 1
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    anchor = GridBagConstraints.NORTHWEST
                    insets = Insets(0, 0, 0, 0)
                },
            )
            capHeight(this)
        }

    private fun capHeight(component: JComponent) {
        val height = component.preferredSize.height.coerceAtLeast(JBUI.scale(24))
        component.maximumSize = Dimension(Int.MAX_VALUE, height)
    }
}
