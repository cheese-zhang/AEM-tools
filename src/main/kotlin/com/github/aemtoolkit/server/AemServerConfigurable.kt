package com.github.aemtoolkit.server

import com.github.aemtoolkit.util.AemPluginInfo
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Configures the AEM Author server used by contextual workflows.
 */
class AemServerConfigurable(project: Project) : Configurable {
    private val settings = AemServerSettings.getInstance(project)
    private val enabled = JBCheckBox("Enable AEM server features")
    private val baseUrl = JBTextField()
    private val username = JBTextField()
    private val password = JBPasswordField()
    private val debugPort = JBTextField()
    private var panel: JPanel? = null

    override fun getDisplayName(): String = "AEM Servers"

    override fun createComponent(): JComponent =
        JPanel(GridBagLayout()).also { form ->
            panel = form
            val constraints = GridBagConstraints().apply {
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
                insets = Insets(4, 4, 4, 4)
            }
            constraints.gridx = 0
            constraints.gridy = 0
            constraints.gridwidth = 2
            form.add(enabled, constraints)
            constraints.gridwidth = 1
            addRow(form, constraints, "Base URL:", baseUrl)
            addRow(form, constraints, "Username:", username)
            addRow(form, constraints, "Password:", password)
            addRow(form, constraints, "Remote debug port:", debugPort)
            constraints.gridy++
            constraints.gridx = 0
            constraints.gridwidth = 2
            constraints.weightx = 1.0
            form.add(TitledSeparator("About"), constraints)
            constraints.gridy++
            form.add(
                JBLabel("AEM Toolkit by ${AemPluginInfo.AUTHOR}"),
                constraints,
            )
            constraints.gridy++
            form.add(
                ActionLink("Open author profile") {
                    BrowserUtil.browse(AemPluginInfo.AUTHOR_URL)
                },
                constraints,
            )
            constraints.gridy++
            form.add(
                ActionLink("Open project on GitHub") {
                    BrowserUtil.browse(AemPluginInfo.PROJECT_URL)
                },
                constraints,
            )
            constraints.gridy++
            constraints.weighty = 1.0
            form.add(JPanel(), constraints)
            reset()
        }

    private fun addRow(
        form: JPanel,
        constraints: GridBagConstraints,
        label: String,
        field: JComponent,
    ) {
        constraints.gridy++
        constraints.gridx = 0
        constraints.weightx = 0.0
        form.add(JBLabel(label), constraints)
        constraints.gridx = 1
        constraints.weightx = 1.0
        form.add(field, constraints)
    }

    override fun isModified(): Boolean =
        enabled.isSelected != settings.state.enabled ||
            baseUrl.text.trim() != settings.state.baseUrl ||
            username.text.trim() != settings.state.username ||
            debugPort.text.toIntOrNull() != settings.state.debugPort ||
            String(password.password) != settings.getPassword().orEmpty()

    override fun apply() {
        settings.state.enabled = enabled.isSelected
        settings.state.baseUrl = baseUrl.text.trim()
        settings.state.username = username.text.trim()
        settings.state.debugPort = debugPort.text.toIntOrNull()
            ?.takeIf { it in 1..65535 }
            ?: throw com.intellij.openapi.options.ConfigurationException(
                "Remote debug port must be between 1 and 65535",
            )
        settings.setPassword(String(password.password))
    }

    override fun reset() {
        enabled.isSelected = settings.state.enabled
        baseUrl.text = settings.state.baseUrl
        username.text = settings.state.username
        debugPort.text = settings.state.debugPort.toString()
        password.text = settings.getPassword().orEmpty()
    }

    override fun disposeUIResources() {
        panel = null
        password.text = ""
    }
}
