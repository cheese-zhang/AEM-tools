package com.github.aemtoolkit.server

import com.intellij.testFramework.LightVirtualFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains
import kotlin.test.assertNull
import java.nio.file.Path

class AemContentActionSelectionTest {
    @Test
    fun `editor content XML wins over a contextual directory`() {
        val contentXml = LightVirtualFile(".content.xml")
        val directory = LightVirtualFile("component")

        assertEquals(
            contentXml,
            AemContentActionSelection.select(
                contentXml,
                editorContext = true,
                projectFiles = arrayOf(directory),
                contextFile = directory,
            ),
        )
    }

    @Test
    fun `one Project View file is selected exactly`() {
        val contentXml = LightVirtualFile(".content.xml")

        assertEquals(
            contentXml,
            AemContentActionSelection.select(
                editorFile = null,
                editorContext = false,
                projectFiles = arrayOf(contentXml),
                contextFile = null,
            ),
        )
    }

    @Test
    fun `ambiguous Project View selection is rejected`() {
        assertNull(
            AemContentActionSelection.select(
                null,
                editorContext = false,
                projectFiles = arrayOf(
                    LightVirtualFile(".content.xml"),
                    LightVirtualFile("dialog.xml"),
                ),
                contextFile = null,
            ),
        )
    }

    @Test
    fun `editor context never falls back to a contextual directory`() {
        assertNull(
            AemContentActionSelection.select(
                editorFile = null,
                editorContext = true,
                projectFiles = null,
                contextFile = LightVirtualFile("component"),
            ),
        )
    }

    @Test
    fun `non-editor context keeps the single-file fallback`() {
        val contentXml = LightVirtualFile(".content.xml")

        assertEquals(
            contentXml,
            AemContentActionSelection.select(
                editorFile = null,
                editorContext = false,
                projectFiles = null,
                contextFile = contentXml,
            ),
        )
    }

    @Test
    fun `content XML confirmation states that sibling files are excluded`() {
        val message = AemContentSyncSupport.uploadPrompt(
            AemContentSelection(
                source = Path.of("component", ".content.xml"),
                jcrRoot = Path.of("jcr_root"),
                repositoryPath = "/apps/example/component/.content.xml",
                directory = false,
            ),
            LightVirtualFile(".content.xml"),
        )

        assertContains(message, "/apps/example/component")
        assertContains(message, "Sibling files will not be uploaded")
    }
}
