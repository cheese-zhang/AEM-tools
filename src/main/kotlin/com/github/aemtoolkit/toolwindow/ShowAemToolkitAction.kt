package com.github.aemtoolkit.toolwindow

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.wm.ToolWindowManager

/**
 * Opens the AEM Toolkit even when its New UI stripe button was hidden.
 */
class ShowAemToolkitAction : DumbAwareAction() {
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        ToolWindowManager.getInstance(project)
            .getToolWindow(TOOL_WINDOW_ID)
            ?.show()
    }

    private companion object {
        const val TOOL_WINDOW_ID = "AEM Toolkit"
    }
}
