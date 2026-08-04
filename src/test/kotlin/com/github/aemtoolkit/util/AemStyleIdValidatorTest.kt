package com.github.aemtoolkit.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AemStyleIdValidatorTest {
    @Test
    fun `accepts scalar and array style ids`() {
        assertTrue(AemStyleIdValidator.invalidIds("hero-dark").isEmpty())
        assertTrue(AemStyleIdValidator.invalidIds("[hero-dark,12345]").isEmpty())
    }

    @Test
    fun `reports malformed style ids`() {
        assertEquals(
            listOf("invalid style", ""),
            AemStyleIdValidator.invalidIds("[valid,invalid style,]"),
        )
    }
}
