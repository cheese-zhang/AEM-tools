package com.github.aemtoolkit.server

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction

/**
 * Opens AEM settings directly from the Tools menu.
 */
class ShowAemSettingsAction : DumbAwareAction() {
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        ShowSettingsUtil.getInstance()
            .showSettingsDialog(project, AemServerConfigurable::class.java)
    }
}
