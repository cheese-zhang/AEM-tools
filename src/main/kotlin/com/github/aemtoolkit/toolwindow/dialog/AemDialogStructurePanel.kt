package com.github.aemtoolkit.toolwindow.dialog

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import java.util.concurrent.atomic.AtomicLong

/**
 * Displays the Granite UI hierarchy of the selected dialog XML file.
 */
class AemDialogStructurePanel(private val project: Project) : JPanel(BorderLayout()), Disposable {
    private val parser = AemDialogStructureParser()
    private val inheritanceResolver = AemDialogInheritanceResolver(project, parser)
    private val tree = Tree()
    private var sourceFile: com.intellij.openapi.vfs.VirtualFile? = null
    private val refreshGeneration = AtomicLong()

    init {
        add(ScrollPaneFactory.createScrollPane(tree, true), BorderLayout.CENTER)
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2 && event.button == MouseEvent.BUTTON1) {
                    navigateToSelection()
                }
            }
        })
        project.messageBus.connect(this).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) = refresh()
            },
        )
        refresh()
    }

    /** Reloads the dialog outline for the currently selected editor. */
    fun refresh() {
        val selected = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        val generation = refreshGeneration.incrementAndGet()
        if (!AemDialogEditorSupport.isDialog(selected)) {
            sourceFile = null
            showMessage("Open an _cq_dialog/.content.xml file")
            return
        }
        showMessage("Loading dialog hierarchy...")
        ReadAction.nonBlocking<AemDialogNode?> {
            AemDialogEditorSupport.parseDialog(project, selected, parser)
                ?.let(inheritanceResolver::resolve)
        }
            .expireWith(this)
            .coalesceBy(this)
            .finishOnUiThread(ModalityState.any()) { model ->
                if (generation != refreshGeneration.get()) return@finishOnUiThread
                sourceFile = selected
                tree.model = DefaultTreeModel(
                    model?.let(::toTreeNode)
                        ?: DefaultMutableTreeNode("Open an _cq_dialog/.content.xml file"),
                )
                repeat(minOf(tree.rowCount, 8)) { tree.expandRow(it) }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun showMessage(message: String) {
        tree.model = DefaultTreeModel(DefaultMutableTreeNode(message))
    }

    private fun toTreeNode(
        node: AemDialogNode,
        tabItem: Boolean = false,
    ): DefaultMutableTreeNode =
        DefaultMutableTreeNode(
            DialogTreeItem(AemDialogPresentation.treeLabel(node, tabItem), node.sourceOffset),
        ).apply {
            val isTabs = node.resourceType?.substringAfterLast('/') == "tabs"
            val children = when {
                isTabs -> AemDialogPresentation.tabItems(node)
                tabItem -> AemDialogPresentation.contentItems(node)
                else -> node.children
            }
            children.forEach { add(toTreeNode(it, isTabs)) }
        }

    private fun navigateToSelection() {
        val treeNode = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
        val item = treeNode.userObject as? DialogTreeItem ?: return
        val file = sourceFile ?: return
        OpenFileDescriptor(project, file, item.offset).navigate(true)
    }

    override fun dispose() = Unit

    private data class DialogTreeItem(
        val label: String,
        val offset: Int,
    ) {
        override fun toString(): String = label
    }
}
