package com.github.aemtoolkit.server

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException

/** Shared UI workflow for pulling a JCR resource as a FileVault package. */
object AemContentPullSupport {
    /** Prompts for a destination and downloads [repositoryPath]. */
    fun pull(project: Project, sourceFile: VirtualFile, repositoryPath: String) {
        val defaultName = repositoryPath.trim('/')
            .replace(Regex("""[^A-Za-z0-9._-]+"""), "-")
            .ifBlank { "aem-content" } + ".zip"
        val destination = FileChooserFactory.getInstance()
            .createSaveFileDialog(
                FileSaverDescriptor(
                    "Pull AEM Content",
                    "Choose where to save the generated FileVault package",
                    "zip",
                ),
                project,
            )
            .save(sourceFile.parent, defaultName)
            ?.file
            ?.toPath()
            ?: return

        object : Task.Backgroundable(project, "Pulling AEM Content", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    AemPackageService.getInstance(project)
                        .pullContent(repositoryPath, destination, indicator)
                    notify(
                        project,
                        "AEM content pulled",
                        destination.toString(),
                        NotificationType.INFORMATION,
                    )
                } catch (error: IOException) {
                    notify(
                        project,
                        "AEM content pull failed",
                        error.message ?: "Package Manager request failed",
                        NotificationType.ERROR,
                    )
                } catch (error: IllegalArgumentException) {
                    notify(
                        project,
                        "AEM content pull failed",
                        error.message ?: "Invalid repository path or server URL",
                        NotificationType.ERROR,
                    )
                }
            }
        }.queue()
    }

    private fun notify(
        project: Project,
        title: String,
        content: String,
        type: NotificationType,
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AEM Toolkit")
            .createNotification(title, content, type)
            .notify(project)
    }
}
