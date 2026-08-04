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

        val tabs = findComponent<JTabbedPane>(panel)!!
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

        val hint = findComponents<JPanel>(panel)
            .single { it.name == AemToolkitPanel.SETUP_HINT_NAME }

        assertTrue(hint.isVisible)
        assertTrue(
            findComponents<JBLabel>(hint)
                .any { it.text.contains("Configure AEM Author") },
        )
    }

    private inline fun <reified T> findComponent(container: Container): T? =
        findComponents<T>(container).firstOrNull()

    private inline fun <reified T> findComponents(container: Container): List<T> =
        buildList {
            container.components.forEach { component ->
                if (component is T) add(component)
                if (component is Container) addAll(findComponents<T>(component))
            }
        }
}
