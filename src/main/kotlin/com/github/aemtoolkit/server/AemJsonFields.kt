package com.github.aemtoolkit.server

/**
 * Reads scalar fields from the flat JCR JSON responses used by Author status.
 */
object AemJsonFields {
    /** Returns a decoded string, boolean, or numeric field. */
    fun read(json: String, name: String): String? {
        val escapedName = Regex.escape(name)
        val stringValue = Regex(
            """"$escapedName"\s*:\s*"((?:\\.|[^"\\])*)"""",
        ).find(json)?.groupValues?.get(1)
        if (stringValue != null) return decode(stringValue)

        return Regex(
            """"$escapedName"\s*:\s*(true|false|-?\d+(?:\.\d+)?)""",
        ).find(json)?.groupValues?.get(1)
    }

    private fun decode(value: String): String =
        value
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
}
