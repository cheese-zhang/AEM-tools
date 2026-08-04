package com.github.aemtoolkit.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.treeStructure.Tree
import java.awt.Container

class AemExplorerPanelTest : BasePlatformTestCase() {
    fun testShowsStatusWhileRepositoryLoads() {
        lateinit var panel: AemExplorerPanel
        ApplicationManager.getApplication().invokeAndWait {
            panel = AemExplorerPanel(project)
            Disposer.register(testRootDisposable, panel)
        }

        val tree = findTree(panel)
        assertNotNull(tree)
        assertTrue(tree!!.rowCount > 0)
        assertTrue(tree.getPathForRow(0).lastPathComponent.toString().isNotBlank())
    }

    private fun findTree(container: Container): Tree? {
        container.components.forEach { component ->
            if (component is Tree) return component
            if (component is Container) {
                findTree(component)?.let { return it }
            }
        }
        return null
    }
}
