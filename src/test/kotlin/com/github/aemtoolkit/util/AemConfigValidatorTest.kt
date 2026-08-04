package com.github.aemtoolkit.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AemConfigValidatorTest {
    @Test
    fun `validates dispatcher block balance`() {
        assertTrue(AemConfigValidator.validate("dispatcher.any", "/farms { }").isEmpty())
        assertEquals(
            listOf("Dispatcher block is not closed"),
            AemConfigValidator.validate("dispatcher.any", "/farms {"),
        )
    }

    @Test
    fun `requires cnd node type declaration`() {
        assertEquals(
            listOf("CND file does not declare a node type"),
            AemConfigValidator.validate("types.cnd", "<app='urn:app'>"),
        )
    }

    @Test
    fun `validates felix property syntax`() {
        assertEquals(
            listOf("Felix configuration property is missing '='"),
            AemConfigValidator.validate("service.config", "enabled B:true"),
        )
    }
}
