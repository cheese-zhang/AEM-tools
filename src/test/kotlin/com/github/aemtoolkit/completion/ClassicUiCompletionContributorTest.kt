package com.github.aemtoolkit.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ClassicUiCompletionContributorTest : BasePlatformTestCase() {
    fun testCompletesXtypesInClassicDialog() {
        myFixture.configureByText(
            "dialog.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                jcr:primaryType="cq:Dialog">
              <field xtype="<caret>"/>
            </jcr:root>
            """.trimIndent(),
        )

        myFixture.completeBasic()

        assertContainsElements(
            myFixture.lookupElementStrings.orEmpty(),
            "textfield",
            "pathfield",
            "multifield",
        )
    }

    fun testCompletesFieldsForSelectedXtype() {
        myFixture.configureByText(
            "dialog.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                jcr:primaryType="cq:Dialog">
              <field xtype="textfield" <caret>/>
            </jcr:root>
            """.trimIndent(),
        )

        myFixture.completeBasic()

        assertContainsElements(
            myFixture.lookupElementStrings.orEmpty(),
            "fieldLabel",
            "allowBlank",
            "maxLength",
        )
    }
}
