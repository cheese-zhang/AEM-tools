package com.github.aemtoolkit.util

import kotlin.test.Test
import kotlin.test.assertEquals

class AemXmlUtilTest {
    @Test
    fun `supported attributes contain milestone one attributes`() {
        assertEquals(
            setOf(
                "sling:resourceType",
                "cq:template",
                "cq:policy",
                "cq:styleIds",
                "jcr:primaryType",
            ),
            AemXmlUtil.supportedAttributes,
        )
    }
}
