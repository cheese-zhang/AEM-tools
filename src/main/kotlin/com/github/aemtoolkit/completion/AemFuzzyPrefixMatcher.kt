package com.github.aemtoolkit.completion

import com.intellij.codeInsight.completion.PrefixMatcher

/**
 * Matches Sling resource paths by ordered characters across path fragments.
 */
class AemFuzzyPrefixMatcher(prefix: String) : PrefixMatcher(prefix) {
    override fun prefixMatches(name: String): Boolean {
        if (prefix.isEmpty()) return true
        val query = prefix.lowercase()
        val candidate = name.lowercase()
        var queryIndex = 0
        for (character in candidate) {
            if (character == query[queryIndex]) {
                queryIndex++
                if (queryIndex == query.length) return true
            }
        }
        return false
    }

    override fun cloneWithPrefix(prefix: String): PrefixMatcher =
        AemFuzzyPrefixMatcher(prefix)
}
