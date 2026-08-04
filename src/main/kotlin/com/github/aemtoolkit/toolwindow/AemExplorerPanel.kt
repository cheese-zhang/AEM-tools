package com.github.aemtoolkit.toolwindow

import com.github.aemtoolkit.resolver.AemArtifactResolver
import com.github.aemtoolkit.resolver.AemComponent
import com.github.aemtoolkit.resolver.AemRepositoryArtifact
import com.github.aemtoolkit.resolver.AemRepositoryArtifactKind
import com.github.aemtoolkit.resolver.ResourceTypeResolver
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.treeStructure.Tree
import com.intellij.ui.TreeSpeedSearch
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Displays indexed AEM components and repository artifacts.
 */
class AemExplorerPanel(private val project: Project) :
    JPanel(BorderLayout()),
    Disposable {
    private val tree = Tree()

    init {
        tree.isRootVisible = false
        tree.showsRootHandles = true
        TreeSpeedSearch.installOn(tree)
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2 && event.button == MouseEvent.BUTTON1) {
                    openSelectedFile()
                }
            }
        })
        add(ScrollPaneFactory.createScrollPane(tree, true), BorderLayout.CENTER)
        showMessage("Loading AEM repository...")
        refresh()
    }

    /** Reloads repository artifacts from IntelliJ indexes. */
    fun refresh() {
        if (project.isDisposed) return
        if (DumbService.isDumb(project)) {
            showMessage("Indexing AEM project...")
            DumbService.getInstance(project).runWhenSmart(::refresh)
            return
        }
        showMessage("Loading AEM repository...")
        ReadAction.nonBlocking<DefaultMutableTreeNode>(::buildTree)
            .expireWith(this)
            .coalesceBy(this)
            .finishOnUiThread(ModalityState.any()) { root ->
                tree.model = DefaultTreeModel(root)
                repeat(minOf(tree.rowCount, 5)) { tree.expandRow(it) }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun showMessage(message: String) {
        val root = DefaultMutableTreeNode("AEM").apply {
            add(DefaultMutableTreeNode(message))
        }
        tree.model = DefaultTreeModel(root)
    }

    private fun openSelectedFile() {
        val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
        val item = node.userObject as? ExplorerItem ?: return
        val target = if (item.file.isDirectory) {
            item.file.findChild(".content.xml") ?: item.file
        } else {
            item.file
        }
        if (!target.isDirectory) {
            FileEditorManager.getInstance(project).openFile(target, true)
        }
    }

    private fun buildTree(): DefaultMutableTreeNode {
        val root = DefaultMutableTreeNode("AEM")
        root.add(componentRoot())

        val artifacts = AemArtifactResolver.getInstance(project).repositoryArtifacts()
        root.add(artifactRoot("Templates", AemRepositoryArtifactKind.TEMPLATE, artifacts))
        root.add(artifactRoot("Policies", AemRepositoryArtifactKind.POLICY, artifacts))
        root.add(artifactRoot("Clientlibs", AemRepositoryArtifactKind.CLIENTLIB, artifacts))
        return root
    }

    private fun componentRoot(): DefaultMutableTreeNode {
        val root = DefaultMutableTreeNode("Components")
        ResourceTypeResolver.getInstance(project).allComponents().forEach { component ->
            val segments = component.resourceType.split('/').filter(String::isNotEmpty)
            val parent = groupPath(root, segments.dropLast(1))
            parent.add(componentNode(component))
        }
        return root
    }

    private fun componentNode(component: AemComponent): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode(
            ExplorerItem(component.name, component.directory),
        )
        component.dialog?.let { node.add(fileNode("Dialog", it)) }
        component.htl?.let { node.add(fileNode("HTL", it)) }
        component.slingModel?.let { node.add(fileNode("Java Model", it)) }
        component.clientlibs.forEach { clientlib ->
            node.add(fileNode("Clientlib: ${clientlib.name}", clientlib))
        }
        findFrontendFiles(component.directory, "js").forEach {
            node.add(fileNode("JS: ${it.name}", it))
        }
        findFrontendFiles(component.directory, "scss").forEach {
            node.add(fileNode("SCSS: ${it.name}", it))
        }
        return node
    }

    private fun artifactRoot(
        label: String,
        kind: AemRepositoryArtifactKind,
        artifacts: List<AemRepositoryArtifact>,
    ): DefaultMutableTreeNode {
        val root = DefaultMutableTreeNode(label)
        artifacts.filter { it.kind == kind }.forEach { artifact ->
            val segments = artifact.repositoryPath
                .trim('/')
                .split('/')
                .filter(String::isNotEmpty)
            val parent = groupPath(root, segments.dropLast(1))
            parent.add(
                DefaultMutableTreeNode(
                    ExplorerItem(artifact.name, artifact.directory),
                ),
            )
        }
        return root
    }

    private fun fileNode(label: String, file: VirtualFile): DefaultMutableTreeNode =
        DefaultMutableTreeNode(ExplorerItem(label, file))

    private fun groupPath(
        root: DefaultMutableTreeNode,
        segments: List<String>,
    ): DefaultMutableTreeNode =
        segments.fold(root) { parent, segment ->
            parent.children()
                .asSequence()
                .filterIsInstance<DefaultMutableTreeNode>()
                .firstOrNull { it.userObject == segment }
                ?: DefaultMutableTreeNode(segment).also(parent::add)
        }

    private fun findFrontendFiles(directory: VirtualFile, extension: String): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        VfsUtilCore.iterateChildrenRecursively(directory, null) { file ->
            if (!file.isDirectory && file.extension.equals(extension, ignoreCase = true)) {
                result.add(file)
            }
            true
        }
        return result.sortedBy(VirtualFile::getPath)
    }

    private data class ExplorerItem(
        val label: String,
        val file: VirtualFile,
    ) {
        override fun toString(): String = label
    }

    override fun dispose() = Unit
}
