package com.github.aemtoolkit.resolver

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ScriptResourceResolutionTest : BasePlatformTestCase() {
    fun testResolvesNonComponentRenderConditionDirectory() {
        val contentXml = myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/base/renderconditions/featureflag/.content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                jcr:primaryType="nt:unstructured"/>
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/base/renderconditions/featureflag/featureflag.jsp",
            "<%@page session=\"false\"%>",
        )

        val resolver = ResourceTypeResolver.getInstance(project)
        val directory = resolver.resolveDirectory("example/base/renderconditions/featureflag")

        assertNotNull(directory)
        assertEquals(contentXml.virtualFile.parent, directory)
        assertNull(resolver.resolve("example/base/renderconditions/featureflag"))
    }
}
