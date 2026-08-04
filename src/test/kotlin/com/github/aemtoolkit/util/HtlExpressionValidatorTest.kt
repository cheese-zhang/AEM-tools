package com.github.aemtoolkit.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HtlExpressionValidatorTest {
    @Test
    fun `accepts balanced expressions`() {
        assertNull(HtlExpressionValidator.validate("Hello \${properties.title}"))
    }

    @Test
    fun `reports incomplete expressions`() {
        assertEquals(
            "HTL expression is missing a closing brace",
            HtlExpressionValidator.validate("\${properties.title"),
        )
    }
}
