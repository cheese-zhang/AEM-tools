package com.github.aemtoolkit.toolwindow.content

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TreeSpeedSearch
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
 * Follows the selected editor and displays nested AEM content nodes.
 */
class AemContentTreePanel(private val project: Project) : JPanel(BorderLayout()), Disposable {
    private val parser = AemContentTreeParser()
    private val tree = Tree()
    private var sourceFile: com.intellij.openapi.vfs.VirtualFile? = null
    private val refreshGeneration = AtomicLong()

    init {
        tree.isRootVisible = true
        tree.showsRootHandles = true
        TreeSpeedSearch.installOn(tree)
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
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    refresh()
                }
            },
        )
        refresh()
    }

    /** Reloads the hierarchy for the currently selected editor. */
    fun refresh() {
        val selected = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        val generation = refreshGeneration.incrementAndGet()
        if (selected?.name != ".content.xml") {
            sourceFile = null
            showMessage("Open an AEM .content.xml file")
            return
        }
        showMessage("Loading content hierarchy...")
        ReadAction.nonBlocking<AemContentNode?> {
            val xmlFile = selected?.let { PsiManager.getInstance(project).findFile(it) as? XmlFile }
            xmlFile?.let(parser::parse)
        }
            .expireWith(this)
            .coalesceBy(this)
            .finishOnUiThread(ModalityState.any()) { model ->
                if (generation != refreshGeneration.get()) return@finishOnUiThread
                sourceFile = selected
                tree.model = DefaultTreeModel(
                    model?.let(::toTreeNode)
                        ?: DefaultMutableTreeNode("Open an AEM .content.xml file"),
                )
                repeat(minOf(tree.rowCount, 8)) { tree.expandRow(it) }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun showMessage(message: String) {
        tree.model = DefaultTreeModel(
            DefaultMutableTreeNode(message),
        )
    }

    private fun toTreeNode(node: AemContentNode): DefaultMutableTreeNode =
        DefaultMutableTreeNode(ContentTreeItem(node.displayName, node.sourceOffset)).apply {
            node.children.forEach { add(toTreeNode(it)) }
        }

    private fun navigateToSelection() {
        val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
        val item = node.userObject as? ContentTreeItem ?: return
        val file = sourceFile ?: return
        OpenFileDescriptor(project, file, item.offset).navigate(true)
    }

    override fun dispose() = Unit

    private data class ContentTreeItem(
        val label: String,
        val offset: Int,
    ) {
        override fun toString(): String = label
    }
}
