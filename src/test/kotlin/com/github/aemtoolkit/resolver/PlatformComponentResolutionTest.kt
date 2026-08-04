package com.github.aemtoolkit.resolver

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class PlatformComponentResolutionTest : BasePlatformTestCase() {
    fun testResolvesComponentFromLocalLibsSource() {
        myFixture.addFileToProject(
            "sdk.ui.apps/src/main/content/jcr_root/libs/granite/ui/components/coral/foundation/container/.content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                jcr:primaryType="cq:Component"/>
            """.trimIndent(),
        )

        val component = ResourceTypeResolver.getInstance(project)
            .resolve("granite/ui/components/coral/foundation/container")

        assertNotNull(component)
        assertEquals(
            "/libs/granite/ui/components/coral/foundation/container",
            component!!.componentPath,
        )
    }
}
