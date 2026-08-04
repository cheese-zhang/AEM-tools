package com.github.aemtoolkit.server

/**
 * Felix bundle metadata returned by the AEM Web Console.
 */
data class AemBundle(
    val id: Long,
    val symbolicName: String,
    val version: String?,
    val state: String?,
) {
    override fun toString(): String = buildString {
        append(symbolicName)
        version?.let { append("  ").append(it) }
        state?.let { append("  [").append(it).append(']') }
    }
}
