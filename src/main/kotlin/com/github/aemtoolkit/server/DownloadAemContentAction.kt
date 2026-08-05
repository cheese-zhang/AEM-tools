package com.github.aemtoolkit.server

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

/** Downloads the selected FileVault resource from AEM into the project. */
class DownloadAemContentAction : DumbAwareAction() {
    override fun update(event: AnActionEvent) {
        AemContentSyncSupport.update(event)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = AemContentActionSelection.selectedFile(event) ?: return
        AemContentSyncSupport.download(project, file)
    }
}
