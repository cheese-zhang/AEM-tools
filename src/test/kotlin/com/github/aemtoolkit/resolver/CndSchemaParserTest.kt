package com.github.aemtoolkit.resolver

import kotlin.test.Test
import kotlin.test.assertEquals

class CndSchemaParserTest {
    @Test
    fun `extracts node types and named properties`() {
        val definitions = CndSchemaParser.parse(
            """
            <app='https://example.com/app'>
            [app:Page] > cq:Page
            - app:category (string)
            - * (undefined)
            """.trimIndent(),
        )

        assertEquals(
            listOf("app:Page", "app:category"),
            definitions.map(JcrDefinition::name),
        )
    }
}
