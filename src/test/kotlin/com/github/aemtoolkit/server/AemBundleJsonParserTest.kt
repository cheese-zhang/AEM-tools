package com.github.aemtoolkit.server

import kotlin.test.Test
import kotlin.test.assertEquals

class AemBundleJsonParserTest {
    @Test
    fun `parses Felix bundle response`() {
        val bundles = AemBundleJsonParser.parse(
            """
            {"data":[
              {"id":12,"symbolicName":"com.example.core","version":"1.2.0","state":"Active"},
              {"id":3,"symbolicName":"org.apache.sling.api","version":"2.27.0","state":"Active"}
            ]}
            """.trimIndent(),
        )

        assertEquals(
            listOf("com.example.core", "org.apache.sling.api"),
            bundles.map(AemBundle::symbolicName),
        )
    }
}
