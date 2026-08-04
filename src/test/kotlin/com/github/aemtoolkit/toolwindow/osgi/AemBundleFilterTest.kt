package com.github.aemtoolkit.toolwindow.osgi

import com.github.aemtoolkit.server.AemBundle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AemBundleFilterTest {
    private val bundle = AemBundle(42, "com.example.core", "1.2.3", "Active")

    @Test
    fun `filters by text and state`() {
        assertTrue(AemBundleFilter.matches(bundle, "example", null))
        assertTrue(AemBundleFilter.matches(bundle, "1.2", "Active"))
        assertTrue(AemBundleFilter.matches(bundle, "42", "active"))
        assertFalse(AemBundleFilter.matches(bundle, "other", null))
        assertFalse(AemBundleFilter.matches(bundle, "", "Resolved"))
    }
}
