package com.github.aemtoolkit.resolver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AemPlatformResourceTypeTest {
    @Test
    fun `recognizes AEM libs resource types`() {
        assertTrue(AemPlatformResourceType.isExternal("granite/ui/components/coral/foundation/container"))
        assertTrue(AemPlatformResourceType.isExternal("cq/gui/components/authoring/dialog"))
        assertTrue(AemPlatformResourceType.isExternal("/libs/wcm/foundation/components/responsivegrid"))
        assertFalse(AemPlatformResourceType.isExternal("example/components/card"))
    }

    @Test
    fun `maps platform resource type to libs path`() {
        assertEquals(
            "/libs/granite/ui/components/coral/foundation/container",
            AemPlatformResourceType.repositoryPath(
                "granite/ui/components/coral/foundation/container",
            ),
        )
        assertNull(AemPlatformResourceType.repositoryPath("example/components/card"))
    }
}
