package com.github.aemtoolkit.projectview

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.util.treeView.AbstractTreeNode

/**
 * Adds local AEM repositories directly to IntelliJ Project View.
 */
class AemProjectTreeStructureProvider : TreeStructureProvider {
    override fun modify(
        parent: AbstractTreeNode<*>,
        children: Collection<AbstractTreeNode<*>>,
        settings: ViewSettings,
    ): Collection<AbstractTreeNode<*>> {
        if (parent !is ProjectViewProjectNode) return children
        return children + AemProjectViewNode(parent.project, settings)
    }
}
