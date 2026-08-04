package com.github.aemtoolkit.resolver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AemRepositoryPathTest {
    @Test
    fun `normalizes repository paths`() {
        assertEquals("/conf/site/settings/wcm/templates/page", AemRepositoryPath.normalize(
            "conf/site/settings/wcm/templates/page",
        ))
    }

    @Test
    fun `extracts repository path below jcr root`() {
        assertEquals(
            "/conf/site/settings/wcm/templates/page/.content.xml",
            AemRepositoryPath.fromFilePath(
                "C:\\project\\ui.content\\src\\main\\content\\jcr_root\\conf\\site\\settings\\wcm\\templates\\page\\.content.xml",
            ),
        )
        assertNull(AemRepositoryPath.fromFilePath("C:\\project\\README.md"))
    }
}
