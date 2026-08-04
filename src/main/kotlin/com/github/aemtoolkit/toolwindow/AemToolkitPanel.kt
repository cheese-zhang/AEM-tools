package com.github.aemtoolkit.toolwindow

import com.github.aemtoolkit.server.AemServerConfigurable
import com.github.aemtoolkit.server.AemServerSettings
import com.github.aemtoolkit.toolwindow.author.AemAuthorStatusPanel
import com.github.aemtoolkit.toolwindow.content.AemContentTreePanel
import com.github.aemtoolkit.toolwindow.dialog.AemDialogPreviewPanel
import com.github.aemtoolkit.toolwindow.dialog.AemDialogStructurePanel
import com.github.aemtoolkit.toolwindow.osgi.AemBundlesPanel
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import com.intellij.openapi.ui.SimpleToolWindowPanel
import javax.swing.JTabbedPane
import javax.swing.JPanel
import java.awt.BorderLayout

/**
 * Single AEM workspace that keeps repository and editor-context tools together.
 */
class AemToolkitPanel(private val project: Project) : SimpleToolWindowPanel(true, true), Disposable {
    private val explorer = AemExplorerPanel(project)
    private val contentTree = AemContentTreePanel(project)
    private val dialogStructure = AemDialogStructurePanel(project)
    private val dialogPreview = AemDialogPreviewPanel(project)
    private val authorStatus = AemAuthorStatusPanel(project)
    private val bundles = AemBundlesPanel(project)

    init {
        Disposer.register(this, explorer)
        Disposer.register(this, contentTree)
        Disposer.register(this, dialogStructure)
        Disposer.register(this, dialogPreview)
        Disposer.register(this, authorStatus)

        val tabs = JBTabbedPane(JTabbedPane.TOP).apply {
            tabLayoutPolicy = JTabbedPane.WRAP_TAB_LAYOUT
            addTab("Content", contentTree)
            addTab("Repository", explorer)
            addTab("Dialog", dialogStructure)
            addTab("Preview", dialogPreview)
            addTab("Author", authorStatus)
            addTab("Bundles", bundles)
            repeat(tabCount) { index ->
                setTabComponentAt(
                    index,
                    JBLabel(getTitleAt(index)).apply {
                        border = JBUI.Borders.empty(0, 4)
                    },
                )
            }
        }
        setContent(
            JPanel(BorderLayout()).apply {
                add(createSetupHint(), BorderLayout.NORTH)
                add(tabs, BorderLayout.CENTER)
            },
        )
        setToolbar(createToolbar())
    }

    private fun createSetupHint(): JPanel =
        JPanel(BorderLayout()).apply {
            name = SETUP_HINT_NAME
            border = JBUI.Borders.empty(6, 8)
            isVisible = !AemServerSettings.getInstance(project).state.enabled
            add(
                JBLabel("Configure AEM Author to enable upload, download, bundles, and server actions."),
                BorderLayout.CENTER,
            )
            add(
                ActionLink("Open Settings") {
                    ShowSettingsUtil.getInstance()
                        .showSettingsDialog(project, AemServerConfigurable::class.java)
                    isVisible = !AemServerSettings.getInstance(project).state.enabled
                },
                BorderLayout.EAST,
            )
        }

    private fun createToolbar() =
        ActionManager.getInstance().createActionToolbar(
            "AEM Toolkit",
            DefaultActionGroup(
                object : DumbAwareAction(
                    "Refresh",
                    "Refresh AEM project and current editor context",
                    AllIcons.Actions.Refresh,
                ) {
                    override fun actionPerformed(event: AnActionEvent) {
                        explorer.refresh()
                        contentTree.refresh()
                        dialogStructure.refresh()
                        dialogPreview.refresh()
                        authorStatus.refresh()
                        bundles.refresh()
                    }
                },
            ),
            true,
        ).also { it.targetComponent = this }.component

    override fun dispose() = Unit

    companion object {
        internal const val SETUP_HINT_NAME = "AemToolkitSetupHint"
    }
}
