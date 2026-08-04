package com.github.aemtoolkit.toolwindow.osgi

import com.github.aemtoolkit.server.AemBundle

/** Filters Felix bundles by free text and lifecycle state. */
object AemBundleFilter {
    /** Returns true when [bundle] matches [query] and [state]. */
    fun matches(bundle: AemBundle, query: String, state: String?): Boolean {
        val normalizedQuery = query.trim()
        val textMatches = normalizedQuery.isEmpty() ||
            bundle.symbolicName.contains(normalizedQuery, ignoreCase = true) ||
            bundle.version?.contains(normalizedQuery, ignoreCase = true) == true ||
            bundle.id.toString().contains(normalizedQuery)
        val stateMatches = state == null || bundle.state.equals(state, ignoreCase = true)
        return textMatches && stateMatches
    }
}
