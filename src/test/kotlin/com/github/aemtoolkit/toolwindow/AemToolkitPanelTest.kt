package com.github.aemtoolkit.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import javax.swing.JTabbedPane
import java.awt.Container
import javax.swing.JPanel

class AemToolkitPanelTest : BasePlatformTestCase() {
    fun testCurrentContentIsDefaultTab() {
        lateinit var panel: AemToolkitPanel
        ApplicationManager.getApplication().invokeAndWait {
            panel = AemToolkitPanel(project)
            Disposer.register(testRootDisposable, panel)
        }

        val tabs = findComponent(panel, JTabbedPane::class.java)!!
        assertEquals("Content", tabs.getTitleAt(0))
        assertEquals("Content", (tabs.getTabComponentAt(0) as JBLabel).text)
        assertEquals(0, tabs.selectedIndex)
    }

    fun testShowsSettingsHintWhenServerFeaturesAreDisabled() {
        lateinit var panel: AemToolkitPanel
        ApplicationManager.getApplication().invokeAndWait {
            panel = AemToolkitPanel(project)
            Disposer.register(testRootDisposable, panel)
        }

        val hint = findComponents(panel, JPanel::class.java)
            .single { it.name == AemToolkitPanel.SETUP_HINT_NAME }

        assertTrue(hint.isVisible)
        assertTrue(
            findComponents(hint, JBLabel::class.java)
                .any { it.text.contains("Configure AEM Author") },
        )
    }

    private fun <T> findComponent(container: Container, type: Class<T>): T? =
        findComponents(container, type).firstOrNull()

    private fun <T> findComponents(container: Container, type: Class<T>): List<T> =
        buildList {
            container.components.forEach { component ->
                if (type.isInstance(component)) add(type.cast(component))
                if (component is Container) addAll(findComponents(component, type))
            }
        }
}
