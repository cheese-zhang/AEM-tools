package com.github.aemtoolkit.server

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction

/**
 * Opens the selected FileVault resource in CRXDE Lite.
 */
class OpenInCrxdeAction : DumbAwareAction() {
    override fun update(event: AnActionEvent) {
        val project = event.project
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val service = project?.let(AemServerConnectionService::getInstance)
        event.presentation.isEnabledAndVisible =
            file != null &&
                service?.repositoryPath(file) != null &&
                AemServerSettings.getInstance(project).state.enabled
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val service = AemServerConnectionService.getInstance(project)
        val path = service.repositoryPath(file) ?: return
        service.openInCrxde(path)
    }
}
