package com.github.aemtoolkit.i18n

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AemI18nServiceTest : BasePlatformTestCase() {
    fun testIndexesNestedTranslationEntries() {
        myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/i18n/en/.content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
                jcr:language="en">
              <navigation sling:key="navigation.label" sling:message="Navigation"/>
              <footer sling:key="footer.label" sling:message="Footer"/>
            </jcr:root>
            """.trimIndent(),
        )

        val service = AemI18nService.getInstance(project)

        assertEquals(listOf("footer.label", "navigation.label"), service.keys())
        assertEquals("Navigation", service.find("navigation.label").single().message)
        assertEquals("en", service.find("navigation.label").single().language)
    }
}
