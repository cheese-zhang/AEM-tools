package com.github.aemtoolkit.toolwindow.content

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.treeStructure.Tree
import java.awt.Container

class AemContentTreePanelTest : BasePlatformTestCase() {
    fun testShowsFocusedContentXmlHierarchy() {
        myFixture.configureByText(
            ".content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0">
                <content>
                    <title/>
                </content>
            </jcr:root>
            """.trimIndent(),
        )
        lateinit var panel: AemContentTreePanel
        ApplicationManager.getApplication().invokeAndWait {
            panel = AemContentTreePanel(project)
            Disposer.register(testRootDisposable, panel)
        }
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion()
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

        val tree = findTree(panel)
        assertNotNull(tree)
        assertTrue(tree!!.rowCount >= 3)
        assertTrue(tree.getPathForRow(0).lastPathComponent.toString().contains("root"))
        assertTrue(tree.getPathForRow(1).lastPathComponent.toString().contains("content"))
        assertTrue(tree.getPathForRow(2).lastPathComponent.toString().contains("title"))
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
