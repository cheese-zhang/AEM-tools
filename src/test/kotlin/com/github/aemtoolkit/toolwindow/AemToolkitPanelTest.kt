package com.github.aemtoolkit.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import javax.swing.JTabbedPane

class AemToolkitPanelTest : BasePlatformTestCase() {
    fun testCurrentContentIsDefaultTab() {
        lateinit var panel: AemToolkitPanel
        ApplicationManager.getApplication().invokeAndWait {
            panel = AemToolkitPanel(project)
            Disposer.register(testRootDisposable, panel)
        }

        val tabs = panel.components
            .asSequence()
            .filterIsInstance<JTabbedPane>()
            .first()
        assertEquals("Content", tabs.getTitleAt(0))
        assertEquals("Content", (tabs.getTabComponentAt(0) as JBLabel).text)
        assertEquals(0, tabs.selectedIndex)
    }
}
