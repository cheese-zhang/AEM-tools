package com.github.aemtoolkit.reference

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ClientLibraryCategoryReferenceContributorTest : BasePlatformTestCase() {
    fun testNavigatesHtlCategoryToClientLibrary() {
        val clientLibrary = myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/clientlibs/site/.content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                jcr:primaryType="cq:ClientLibraryFolder"
                categories="[example.site]"/>
            """.trimIndent(),
        )
        myFixture.configureByText(
            "card.html",
            """
            <sly data-sly-use.clientlib="/libs/granite/sightly/templates/clientlib.html"
                data-sly-call="${'$'}{clientlib.css @ categories=['example.<caret>site']}"/>
            """.trimIndent(),
        )

        assertEquals(
            clientLibrary,
            myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve(),
        )
    }

    fun testNavigatesUnquotedFileVaultArrayItem() {
        val target = myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/clientlibs/base/.content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                jcr:primaryType="cq:ClientLibraryFolder"
                categories="[example.base]"/>
            """.trimIndent(),
        )
        val source = myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/clientlibs/site/.content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                jcr:primaryType="cq:ClientLibraryFolder"
                categories="[example.site]"
                dependencies="[example.base,example.other]"/>
            """.trimIndent(),
        ).virtualFile
        myFixture.configureFromExistingVirtualFile(source)
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("example.base") + 9)

        assertEquals(target, myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve())
    }
}
