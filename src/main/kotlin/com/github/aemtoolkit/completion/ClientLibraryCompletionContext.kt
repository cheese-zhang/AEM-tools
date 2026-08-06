package com.github.aemtoolkit.completion

/** Extracts category and manifest prefixes without depending on legacy HTL PSI. */
internal object ClientLibraryCompletionContext {
    fun htlCategoryPrefix(text: String, offset: Int): String? {
        val values = Regex(
            """clientlib\.(?:js|css|all)\s*@[^}]*\bcategories\s*=\s*\[([^]]*)$""",
        )
            .find(textBeforeCaret(text, offset))
            ?.groupValues
            ?.get(1)
            ?: return null
        return values.substringAfterLast(',').trim().trimStart('"', '\'')
    }

    fun xmlArrayPrefix(text: String, offset: Int): String =
        textBeforeCaret(text, offset)
            .substringAfterLast('[')
            .substringAfterLast(',')
            .substringAfterLast('=')
            .trim()
            .trimStart('"', '\'')

    private fun textBeforeCaret(text: String, offset: Int): String =
        text.substring(0, offset.coerceIn(0, text.length))
            .substringAfterLast('\n')
            .substringBefore("IntellijIdeaRulezzz")
}
