package com.github.aemtoolkit.server

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

/**
 * Opens the configured AEM Web Console.
 */
class OpenWebConsoleAction : DumbAwareAction() {
    override fun update(event: AnActionEvent) {
        val project = event.project
        event.presentation.isEnabledAndVisible =
            project != null && AemServerSettings.getInstance(project).state.enabled
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        AemServerConnectionService.getInstance(project).openWebConsole()
    }
}
