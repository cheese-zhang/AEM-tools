package com.github.aemtoolkit.server

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException

/** Shared action workflow for contextual AEM content synchronization. */
object AemContentSyncSupport {
    /** Updates action visibility for a selected FileVault resource. */
    fun update(event: AnActionEvent) {
        val project = event.project
        val file = AemContentActionSelection.selectedFile(event)
        event.presentation.isEnabledAndVisible =
            project != null &&
                file != null &&
                AemServerSettings.getInstance(project).state.enabled &&
                AemContentSyncService.getInstance(project).selectionOrNull(file) != null
    }

    /** Confirms and downloads [file] from AEM. */
    fun download(project: Project, file: VirtualFile) {
        val selection = AemContentSyncService.getInstance(project).selectionOrNull(file) ?: return
        val answer = Messages.showYesNoDialog(
            project,
            "Download '${selection.repositoryPath}' from AEM and overwrite matching local files?\n" +
                "Local-only files will be kept.",
            "Download from AEM",
            Messages.getWarningIcon(),
        )
        if (answer != Messages.YES) return

        run(project, "Downloading AEM Content", "AEM content downloaded") { indicator ->
            val count = AemContentSyncService.getInstance(project).download(file, indicator)
            "$count local file(s) updated"
        }
    }

    /** Confirms and uploads [file] to AEM. */
    fun upload(project: Project, file: VirtualFile) {
        val selection = AemContentSyncService.getInstance(project).selectionOrNull(file) ?: return
        val answer = Messages.showYesNoDialog(
            project,
            uploadPrompt(selection, file),
            "Upload content to AEM?",
            "Upload",
            "Cancel",
            Messages.getWarningIcon(),
        )
        if (answer != Messages.YES) return

        run(project, "Uploading AEM Content", "AEM content uploaded") { indicator ->
            val count = AemContentSyncService.getInstance(project).upload(selection, indicator)
            "$count file(s) uploaded"
        }
    }

    internal fun uploadPrompt(
        selection: AemContentSelection,
        file: VirtualFile,
    ): String = when {
        selection.directory ->
            "Upload '${selection.repositoryPath}' and every file in this directory to AEM?"
        file.name == ".content.xml" ->
            "Apply only the node definitions in '.content.xml' to " +
                "'${selection.repositoryPath.removeSuffix("/.content.xml")}'? " +
                "Sibling files will not be uploaded."
        else ->
            "Upload only '${file.name}' to '${selection.repositoryPath}'? " +
                "Sibling files will not be uploaded."
    }

    private fun run(
        project: Project,
        taskTitle: String,
        successTitle: String,
        operation: (ProgressIndicator) -> String,
    ) {
        object : Task.Backgroundable(project, taskTitle, true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    notify(project, successTitle, operation(indicator), NotificationType.INFORMATION)
                } catch (error: IOException) {
                    notify(
                        project,
                        "$taskTitle failed",
                        error.message ?: "AEM Package Manager request failed",
                        NotificationType.ERROR,
                    )
                } catch (error: IllegalArgumentException) {
                    notify(
                        project,
                        "$taskTitle failed",
                        error.message ?: "Invalid FileVault resource",
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
