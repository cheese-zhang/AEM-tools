package com.github.aemtoolkit.toolwindow.author

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBTextField
import java.awt.Container
import javax.swing.JButton

class AemAuthorStatusPanelTest : BasePlatformTestCase() {
    fun testShowsCurrentJcrPathAndContextActions() {
        val file = myFixture.addFileToProject(
            "ui.content/src/main/content/jcr_root/content/site/page/.content.xml",
            "<jcr:root xmlns:jcr=\"http://www.jcp.org/jcr/1.0\"/>",
        )
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        lateinit var panel: AemAuthorStatusPanel
        ApplicationManager.getApplication().invokeAndWait {
            panel = AemAuthorStatusPanel(project)
            Disposer.register(testRootDisposable, panel)
        }

        val fields = descendants(panel).filterIsInstance<JBTextField>().toList()
        assertEquals("/content/site/page", fields.single().text)
        val buttons = descendants(panel).filterIsInstance<JButton>().associateBy { it.text }
        assertTrue(buttons.getValue("Copy JCR Path").isEnabled)
        assertFalse(buttons.getValue("Open CRXDE").isEnabled)
        assertFalse(buttons.getValue("Pull Content").isEnabled)
    }

    private fun descendants(container: Container): Sequence<java.awt.Component> =
        container.components.asSequence().flatMap { component ->
            sequenceOf(component) +
                if (component is Container) descendants(component) else emptySequence()
        }
}
