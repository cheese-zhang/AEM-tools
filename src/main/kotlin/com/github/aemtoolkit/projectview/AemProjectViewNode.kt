package com.github.aemtoolkit.projectview

import com.github.aemtoolkit.resolver.AemArtifactResolver
import com.github.aemtoolkit.resolver.AemRepositoryArtifactKind
import com.github.aemtoolkit.resolver.ResourceTypeResolver
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiManager

/**
 * Top-level AEM node shown alongside project modules.
 */
class AemProjectViewNode(
    private val currentProject: Project,
    settings: ViewSettings,
) : ProjectViewNode<String>(currentProject, "AEM", settings) {
    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val components = ResourceTypeResolver.getInstance(currentProject)
            .allComponents()
            .map { it.directory }
        val artifacts = AemArtifactResolver.getInstance(currentProject).repositoryArtifacts()
        return listOf(
            group("Components", components),
            group(
                "Templates",
                artifacts.filter { it.kind == AemRepositoryArtifactKind.TEMPLATE }
                    .map { it.directory },
            ),
            group(
                "Policies",
                artifacts.filter { it.kind == AemRepositoryArtifactKind.POLICY }
                    .map { it.directory },
            ),
            group(
                "Clientlibs",
                artifacts.filter { it.kind == AemRepositoryArtifactKind.CLIENTLIB }
                    .map { it.directory },
            ),
        ).filter { it.files.isNotEmpty() }
    }

    private fun group(label: String, files: List<VirtualFile>): AemProjectGroupNode =
        AemProjectGroupNode(currentProject, label, files.distinct(), settings)

    override fun update(presentation: PresentationData) {
        presentation.presentableText = "AEM"
        presentation.setIcon(AllIcons.Nodes.ModuleGroup)
    }

    override fun contains(file: VirtualFile): Boolean =
        file.path.replace('\\', '/').contains("/jcr_root/")
}

/**
 * Category node containing AEM source directories.
 */
class AemProjectGroupNode(
    private val currentProject: Project,
    private val label: String,
    val files: List<VirtualFile>,
    settings: ViewSettings,
) : ProjectViewNode<String>(currentProject, label, settings) {
    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val manager = PsiManager.getInstance(currentProject)
        return files.mapNotNull(manager::findDirectory)
            .map { PsiDirectoryNode(currentProject, it, settings) }
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = label
        presentation.setIcon(AllIcons.Nodes.Folder)
    }

    override fun contains(file: VirtualFile): Boolean =
        files.any { root -> root == file || VfsUtilCore.isAncestor(root, file, false) }
}
