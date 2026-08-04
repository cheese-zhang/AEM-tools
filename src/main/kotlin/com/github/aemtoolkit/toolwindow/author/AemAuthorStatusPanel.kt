package com.github.aemtoolkit.toolwindow.author

import com.github.aemtoolkit.resolver.AemAuthorService
import com.github.aemtoolkit.resolver.AemAuthorStatus
import com.github.aemtoolkit.resolver.AemRepositoryPath
import com.github.aemtoolkit.server.AemContentPullSupport
import com.github.aemtoolkit.server.AemServerConnectionService
import com.github.aemtoolkit.server.AemServerSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBTextField
import com.intellij.util.concurrency.AppExecutorUtil
import java.io.IOException
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.datatransfer.StringSelection
import java.util.concurrent.atomic.AtomicLong
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Displays metadata supplied by an optional AEM Author connection provider.
 */
class AemAuthorStatusPanel(private val project: Project) :
    JPanel(BorderLayout()),
    Disposable {
    private val values = linkedMapOf(
        "Publish Status" to JLabel("Not connected"),
        "Workflow Status" to JLabel("Not connected"),
        "Last Modified" to JLabel("Not connected"),
        "Version" to JLabel("Not connected"),
        "Live Copy" to JLabel("Not connected"),
    )
    private val path = JBTextField().apply { isEditable = false }
    private val refreshButton = JButton("Refresh Status").apply {
        addActionListener { refresh() }
    }
    private val crxdeButton = JButton("Open CRXDE").apply {
        addActionListener { currentPath?.let(::openCrxde) }
    }
    private val copyButton = JButton("Copy JCR Path").apply {
        addActionListener { currentPath?.let(::copyPath) }
    }
    private val pullButton = JButton("Pull Content").apply {
        addActionListener {
            val file = currentFile
            val repositoryPath = currentPath
            if (file != null && repositoryPath != null) {
                AemContentPullSupport.pull(project, file, repositoryPath)
            }
        }
    }
    private val requestGeneration = AtomicLong()
    private var currentFile: VirtualFile? = null
    private var currentPath: String? = null

    init {
        val header = JPanel(BorderLayout(8, 4)).apply {
            add(JLabel("Current JCR path:"), BorderLayout.WEST)
            add(path, BorderLayout.CENTER)
        }
        val grid = JPanel(GridLayout(0, 2, 8, 4))
        values.forEach { (name, value) ->
            grid.add(JLabel("$name:"))
            grid.add(value)
        }
        val body = JPanel(BorderLayout(0, 8)).apply {
            add(header, BorderLayout.NORTH)
            add(grid, BorderLayout.CENTER)
        }
        add(body, BorderLayout.NORTH)
        add(
            JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
                add(refreshButton)
                add(crxdeButton)
                add(copyButton)
                add(pullButton)
            },
            BorderLayout.SOUTH,
        )
        project.messageBus.connect(this).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    updateContext()
                }
            },
        )
        updateContext()
    }

    /** Reloads Author metadata for the selected repository resource. */
    fun refresh() {
        updateContext()
        val repositoryPath = currentPath
        if (repositoryPath == null) {
            showStatus(null, "Open a file below jcr_root")
            return
        }
        if (!AemServerSettings.getInstance(project).state.enabled) {
            showStatus(null, "Enable AEM server features in Settings")
            return
        }

        val generation = requestGeneration.incrementAndGet()
        values.values.forEach { it.text = "Loading..." }
        AppExecutorUtil.getAppExecutorService().execute {
            val result = try {
                AuthorLoadResult(
                    AemAuthorService.getInstance(project).getStatus(repositoryPath),
                    null,
                )
            } catch (error: IOException) {
                AuthorLoadResult(null, error.message ?: "AEM Author request failed")
            } catch (error: IllegalArgumentException) {
                AuthorLoadResult(null, error.message ?: "Invalid AEM server URL")
            }
            ApplicationManager.getApplication().invokeLater {
                if (!project.isDisposed && generation == requestGeneration.get()) {
                    showStatus(
                        result.status,
                        result.error ?: "No Author status is available",
                    )
                }
            }
        }
    }

    private fun updateContext() {
        requestGeneration.incrementAndGet()
        val previousPath = currentPath
        currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        currentPath = currentFile
            ?.let { AemRepositoryPath.fromFilePath(it.path) }
            ?.removeSuffix("/.content.xml")
        path.text = currentPath.orEmpty()
        path.emptyText.text = "Open a file below jcr_root"
        val serverEnabled = AemServerSettings.getInstance(project).state.enabled
        refreshButton.isEnabled = currentPath != null && serverEnabled
        crxdeButton.isEnabled = currentPath != null && serverEnabled
        copyButton.isEnabled = currentPath != null
        pullButton.isEnabled = currentPath != null && serverEnabled
        if (currentPath == null) {
            showStatus(null, "Open a file below jcr_root")
        } else if (currentPath != previousPath) {
            showStatus(null, "Click Refresh Status")
        }
    }

    private fun openCrxde(repositoryPath: String) {
        AemServerConnectionService.getInstance(project).openInCrxde(repositoryPath)
    }

    private fun copyPath(repositoryPath: String) {
        CopyPasteManager.getInstance().setContents(StringSelection(repositoryPath))
    }

    private fun showStatus(status: AemAuthorStatus?, fallback: String) {
        values.getValue("Publish Status").text = status?.publishStatus ?: fallback
        values.getValue("Workflow Status").text = status?.workflowStatus ?: fallback
        values.getValue("Last Modified").text = status?.lastModified ?: fallback
        values.getValue("Version").text = status?.version ?: fallback
        values.getValue("Live Copy").text = status?.liveCopy ?: fallback
    }

    private data class AuthorLoadResult(
        val status: AemAuthorStatus?,
        val error: String?,
    )

    override fun dispose() = Unit
}
