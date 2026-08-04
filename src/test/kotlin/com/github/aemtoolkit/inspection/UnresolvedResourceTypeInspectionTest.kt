package com.github.aemtoolkit.inspection

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class UnresolvedResourceTypeInspectionTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(UnresolvedResourceTypeInspection())
    }

    fun testDoesNotReportAemPlatformResourceTypes() {
        myFixture.configureByText(
            ".content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
                sling:resourceType="cq/gui/components/authoring/dialog">
                <content sling:resourceType="granite/ui/components/coral/foundation/container"/>
            </jcr:root>
            """.trimIndent(),
        )

        assertEmpty(
            myFixture.doHighlighting()
                .filter {
                    it.severity == HighlightSeverity.ERROR &&
                        it.description?.startsWith("Cannot resolve AEM component") == true
                },
        )
    }

    fun testStillReportsUnknownProjectResourceType() {
        myFixture.configureByText(
            ".content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
                sling:resourceType="example/components/missing"/>
            """.trimIndent(),
        )

        assertContainsElements(
            myFixture.doHighlighting()
                .filter { it.severity == HighlightSeverity.ERROR }
                .mapNotNull { it.description },
            "Cannot resolve AEM component 'example/components/missing'",
        )
    }
}
