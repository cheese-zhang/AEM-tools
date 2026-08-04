package com.github.aemtoolkit.util

/**
 * Parses and validates AEM Style System identifiers.
 */
object AemStyleIdValidator {
    private val validStyleId = Regex("[A-Za-z0-9_-]+")

    /** Returns invalid IDs from a scalar or JCR array value. */
    fun invalidIds(value: String): List<String> =
        value
            .removePrefix("[")
            .removeSuffix("]")
            .split(',')
            .map(String::trim)
            .filter { it.isEmpty() || !validStyleId.matches(it) }
}
