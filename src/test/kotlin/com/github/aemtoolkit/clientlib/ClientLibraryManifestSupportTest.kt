package com.github.aemtoolkit.clientlib

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ClientLibraryManifestSupportTest : BasePlatformTestCase() {
    fun testResolvesIncludesFromBaseDirectory() {
        myFixture.addFileToProject("clientlib/js/app.js", "window.app = {};")
        val manifest = myFixture.addFileToProject(
            "clientlib/js.txt",
            """
            #base=js
            app.js
            """.trimIndent(),
        ).virtualFile

        val resolved = ClientLibraryManifestSupport.resolveInclude(
            manifest,
            "app.js",
            manifest.inputStream.bufferedReader().use { it.readText() }.length,
        )

        assertEquals("app.js", resolved?.name)
        assertEquals(listOf("app.js"), ClientLibraryManifestSupport.includeCandidates(manifest, "js"))
        assertEquals(listOf("js"), ClientLibraryManifestSupport.baseCandidates(manifest))
    }

    fun testUsesManifestExtensionForCandidates() {
        myFixture.addFileToProject("clientlib/css/site.css", "")
        myFixture.addFileToProject("clientlib/css/app.js", "")
        val manifest = myFixture.addFileToProject(
            "clientlib/css.txt",
            "#base=css",
        ).virtualFile

        assertEquals(
            listOf("site.css"),
            ClientLibraryManifestSupport.includeCandidates(manifest, "css"),
        )
    }

    fun testManifestReferenceNavigatesToIncludedFile() {
        val script = myFixture.addFileToProject(
            "clientlib/js/app.js",
            "window.app = {};",
        )
        val manifest = myFixture.addFileToProject(
            "clientlib/js.txt",
            """
            #base=js
            app.js
            """.trimIndent(),
        ).virtualFile
        myFixture.configureFromExistingVirtualFile(manifest)
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("app.js") + 3)

        assertEquals(script, myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve())
    }
}
