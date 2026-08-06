package com.github.aemtoolkit.clientlib

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AemClientLibraryServiceTest : BasePlatformTestCase() {
    fun testIndexesCategoriesDependenciesAndEmbeds() {
        myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/clientlibs/site/.content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                jcr:primaryType="cq:ClientLibraryFolder"
                categories="[example.site,example.base]"
                dependencies="[core.wcm.components]"
                embed="example.shared"/>
            """.trimIndent(),
        )

        val service = AemClientLibraryService.getInstance(project)
        val clientLibrary = service.all().single()

        assertEquals(listOf("example.site", "example.base"), clientLibrary.categories)
        assertEquals(listOf("core.wcm.components"), clientLibrary.dependencies)
        assertEquals(listOf("example.shared"), clientLibrary.embeds)
        assertEquals(
            listOf("example.base", "example.site"),
            service.categories(),
        )
        assertEquals(clientLibrary, service.findByCategory("example.site").single())
    }

    fun testParsesTypedFileVaultArrays() {
        assertEquals(
            listOf("example.one", "example.two"),
            AemClientLibraryService.parseValues("{String}[example.one, example.two]"),
        )
    }
}
