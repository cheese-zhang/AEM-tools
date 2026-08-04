package com.github.aemtoolkit.server

/**
 * Parses the flat bundle objects returned by Felix Web Console JSON.
 */
object AemBundleJsonParser {
    private val objectPattern = Regex("""\{[^{}]*}""")

    /** Returns bundle metadata found in [json]. */
    fun parse(json: String): List<AemBundle> =
        objectPattern.findAll(json)
            .mapNotNull { match ->
                val objectJson = match.value
                val id = AemJsonFields.read(objectJson, "id")?.toLongOrNull()
                    ?: return@mapNotNull null
                val symbolicName = AemJsonFields.read(objectJson, "symbolicName")
                    ?: AemJsonFields.read(objectJson, "name")
                    ?: return@mapNotNull null
                AemBundle(
                    id = id,
                    symbolicName = symbolicName,
                    version = AemJsonFields.read(objectJson, "version"),
                    state = AemJsonFields.read(objectJson, "state"),
                )
            }
            .distinctBy(AemBundle::id)
            .sortedBy(AemBundle::symbolicName)
            .toList()
}
