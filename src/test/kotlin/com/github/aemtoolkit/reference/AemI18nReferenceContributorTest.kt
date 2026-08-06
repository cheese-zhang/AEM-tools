package com.github.aemtoolkit.reference

import com.intellij.ide.highlighter.HtmlFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AemI18nReferenceContributorTest : BasePlatformTestCase() {
    fun testHtlI18nReferenceNavigatesToTranslationFile() {
        val translation = myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/i18n/en/.content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
                jcr:mixin="[mix:language]"
                jcr:language="en">
              <navigation sling:key="navigation.label" sling:message="Navigation"/>
            </jcr:root>
            """.trimIndent(),
        )
        myFixture.configureByText(
            HtmlFileType.INSTANCE,
            """<h2>${'$'}{i18n['navigation.<caret>label']}</h2>""",
        )

        assertEquals(
            translation,
            myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve(),
        )
    }

    fun testStandardHtlI18nReferenceNavigatesToTranslationFile() {
        val translation = myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/i18n/en/.content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
                jcr:language="en">
              <navigation sling:key="navigation.label" sling:message="Navigation"/>
            </jcr:root>
            """.trimIndent(),
        )
        myFixture.configureByText(
            HtmlFileType.INSTANCE,
            """<h2>${'$'}{'navigation.<caret>label' @ i18n}</h2>""",
        )

        assertEquals(
            translation,
            myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve(),
        )
    }
}
