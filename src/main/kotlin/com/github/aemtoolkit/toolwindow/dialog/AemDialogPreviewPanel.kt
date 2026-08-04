package com.github.aemtoolkit.toolwindow.dialog

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import java.util.concurrent.atomic.AtomicLong

/**
 * Renders an approximate Swing preview from the shared dialog model.
 */
class AemDialogPreviewPanel(private val project: Project) : JPanel(BorderLayout()), Disposable {
    private val parser = AemDialogStructureParser()
    private val inheritanceResolver = AemDialogInheritanceResolver(project, parser)
    private val viewport = JPanel(BorderLayout())
    private val refreshGeneration = AtomicLong()

    init {
        add(ScrollPaneFactory.createScrollPane(viewport, true), BorderLayout.CENTER)
        project.messageBus.connect(this).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) = refresh()
            },
        )
        refresh()
    }

    /** Reloads the preview for the currently selected dialog. */
    fun refresh() {
        val selected = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        val generation = refreshGeneration.incrementAndGet()
        if (!AemDialogEditorSupport.isDialog(selected)) {
            showModel(null, "Open an _cq_dialog/.content.xml file")
            return
        }
        showModel(null, "Loading dialog preview...")
        ReadAction.nonBlocking<AemDialogNode?> {
            AemDialogEditorSupport.parseDialog(project, selected, parser)
                ?.let(inheritanceResolver::resolve)
        }
            .expireWith(this)
            .coalesceBy(this)
            .finishOnUiThread(ModalityState.any()) { model ->
                if (generation == refreshGeneration.get()) {
                    showModel(model, "Open an _cq_dialog/.content.xml file")
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun showModel(model: AemDialogNode?, fallback: String) {
        viewport.removeAll()
        viewport.add(
            model?.let(AemDialogPreviewRenderer::render)
                ?: JLabel(fallback),
            BorderLayout.NORTH,
        )
        viewport.revalidate()
        viewport.repaint()
    }

    override fun dispose() = Unit
}
