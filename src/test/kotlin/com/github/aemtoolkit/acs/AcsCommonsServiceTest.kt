package com.github.aemtoolkit.acs

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AcsCommonsServiceTest : BasePlatformTestCase() {
    fun testIndexesSupportedAcsCommonsStructures() {
        myFixture.addFileToProject(
            "ui.content/src/main/content/jcr_root/etc/acs-commons/lists/colors/.content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0">
              <jcr:content>
                <list>
                  <item0 jcr:title="Red" value="red"/>
                  <item1 jcr:title="Blue" value="blue"/>
                </list>
              </jcr:content>
            </jcr:root>
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/config/" +
                "com.adobe.acs.commons.images.impl.NamedImageTransformerImpl-card.xml",
            """
            <jcr:root name="card" transforms="[resize:width=400,quality:quality=82]"/>
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "ui.content/src/main/content/jcr_root/conf/example/settings/redirects/.content.xml",
            """
            <jcr:root>
              <rule source="/old" target="/new" statusCode="{Long}301"/>
            </jcr:root>
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "ui.content/src/main/content/jcr_root/content/example/" +
                "_jcr_content/shared-component-properties/example/components/hero/.content.xml",
            "<jcr:root title=\"Shared title\"/>",
        )

        val service = AcsCommonsService.getInstance(project)

        assertEquals(listOf("red", "blue"), service.genericLists().single().items.map { it.value })
        assertNotNull(service.findGenericList("/etc/acs-commons/lists/colors/_jcr_content.list.json"))
        assertEquals(listOf("resize:width=400", "quality:quality=82"),
            service.namedImageTransforms().single().transforms)
        assertEquals(301, service.redirectRules().single().statusCode)
        assertEquals(
            "example/components/hero",
            service.sharedProperties().single().componentResourceType,
        )
    }
}
