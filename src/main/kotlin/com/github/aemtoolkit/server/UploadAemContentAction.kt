package com.github.aemtoolkit.server

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

/** Uploads the selected FileVault resource to AEM as a temporary package. */
class UploadAemContentAction : DumbAwareAction() {
    override fun update(event: AnActionEvent) {
        AemContentSyncSupport.update(event)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = AemContentActionSelection.selectedFile(event) ?: return
        AemContentSyncSupport.upload(project, file)
    }
}
