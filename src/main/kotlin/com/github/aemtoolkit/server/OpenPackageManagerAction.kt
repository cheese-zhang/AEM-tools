package com.github.aemtoolkit.server

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

/** Opens AEM Package Manager for the configured server. */
class OpenPackageManagerAction : DumbAwareAction() {
    override fun update(event: AnActionEvent) {
        val project = event.project
        event.presentation.isEnabledAndVisible =
            project != null && AemServerSettings.getInstance(project).state.enabled
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        AemServerConnectionService.getInstance(project).openPackageManager()
    }
}
