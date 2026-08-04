package com.github.aemtoolkit.server

import com.intellij.execution.RunManager
import com.intellij.execution.remote.RemoteConfiguration
import com.intellij.execution.remote.RemoteConfigurationType
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import java.net.URI

/**
 * Creates or updates an IntelliJ Remote JVM configuration for AEM.
 */
class CreateAemDebugConfigurationAction : DumbAwareAction() {
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val server = AemServerSettings.getInstance(project)
        val host = try {
            URI.create(server.normalizedBaseUrl()).host
        } catch (_: IllegalArgumentException) {
            null
        }
        if (host.isNullOrBlank()) {
            notify(project, "Invalid AEM server URL", NotificationType.ERROR)
            return
        }

        val runManager = RunManager.getInstance(project)
        val name = "AEM Remote Debug"
        val settings = runManager.findConfigurationByName(name)
            ?: runManager.createConfiguration(
                name,
                RemoteConfigurationType.getInstance().configurationFactories.first(),
            ).also(runManager::addConfiguration)
        val configuration = settings.configuration as? RemoteConfiguration
        if (configuration == null) {
            notify(
                project,
                "A run configuration named '$name' already exists with another type",
                NotificationType.ERROR,
            )
            return
        }
        configuration.USE_SOCKET_TRANSPORT = true
        configuration.SERVER_MODE = false
        configuration.HOST = host
        configuration.PORT = server.state.debugPort.toString()
        runManager.selectedConfiguration = settings
        notify(
            project,
            "Remote debug configuration ready: $host:${server.state.debugPort}",
            NotificationType.INFORMATION,
        )
    }

    private fun notify(
        project: com.intellij.openapi.project.Project,
        message: String,
        type: NotificationType,
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AEM Toolkit")
            .createNotification("AEM Toolkit", message, type)
            .notify(project)
    }
}
