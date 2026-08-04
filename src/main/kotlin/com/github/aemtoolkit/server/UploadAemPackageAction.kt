package com.github.aemtoolkit.server

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import java.io.IOException

/**
 * Uploads and installs a selected FileVault package after explicit confirmation.
 */
class UploadAemPackageAction : DumbAwareAction() {
    override fun update(event: AnActionEvent) {
        val project = event.project
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible =
            project != null &&
                file?.extension.equals("zip", ignoreCase = true) &&
                AemServerSettings.getInstance(project).state.enabled
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val answer = Messages.showYesNoDialog(
            project,
            "Upload and install '${file.name}' on the configured AEM server?",
            "Install AEM Package",
            Messages.getWarningIcon(),
        )
        if (answer != Messages.YES) return

        object : Task.Backgroundable(project, "Installing AEM Package", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    AemPackageService.getInstance(project).uploadAndInstall(file, indicator)
                    notify("Package installed", file.name, NotificationType.INFORMATION)
                } catch (error: IOException) {
                    notify(
                        "Package installation failed",
                        error.message ?: "AEM Package Manager request failed",
                        NotificationType.ERROR,
                    )
                } catch (error: IllegalArgumentException) {
                    notify(
                        "Package installation failed",
                        error.message ?: "Invalid package or server URL",
                        NotificationType.ERROR,
                    )
                }
            }

            private fun notify(title: String, content: String, type: NotificationType) {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("AEM Toolkit")
                    .createNotification(title, content, type)
                    .notify(project)
            }
        }.queue()
    }
}
