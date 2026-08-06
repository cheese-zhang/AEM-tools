package com.github.aemtoolkit.resolver

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class HtlUseTemplateResolverTest : BasePlatformTestCase() {
    fun testFindsRelativeAndPlatformTemplates() {
        val source = myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/components/card/card.html",
            "<div/>",
        ).virtualFile
        myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/components/card/templates/actions.html",
            "<template/>",
        )

        val paths = HtlUseTemplateResolver.candidates(project, source).map { it.lookupString }

        assertContainsElements(
            paths,
            "templates/actions.html",
            HtlUseTemplateResolver.CLIENT_LIBRARY_TEMPLATE,
        )
    }
}
