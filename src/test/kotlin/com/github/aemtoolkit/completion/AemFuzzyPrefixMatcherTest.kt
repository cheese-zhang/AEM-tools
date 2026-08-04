package com.github.aemtoolkit.completion

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AemFuzzyPrefixMatcherTest {
    @Test
    fun `matches ordered resource path fragments`() {
        val matcher = AemFuzzyPrefixMatcher("gufcont")
        assertTrue(matcher.prefixMatches("granite/ui/components/foundation/container"))
        assertFalse(matcher.prefixMatches("core/wcm/components/container"))
    }
}
