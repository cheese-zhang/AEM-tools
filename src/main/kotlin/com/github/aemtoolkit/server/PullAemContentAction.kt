package com.github.aemtoolkit.server

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction

/**
 * Pulls a selected JCR resource into a local FileVault ZIP package.
 */
class PullAemContentAction : DumbAwareAction() {
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
        val repositoryPath = AemServerConnectionService.getInstance(project)
            .repositoryPath(file)
            ?: return
        AemContentPullSupport.pull(project, file, repositoryPath)
    }
}
