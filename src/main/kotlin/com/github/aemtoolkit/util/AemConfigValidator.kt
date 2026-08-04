package com.github.aemtoolkit.util

/**
 * Lightweight structural validators for AEM ecosystem configuration files.
 */
object AemConfigValidator {
    /** Returns validation messages appropriate for [fileName]. */
    fun validate(fileName: String, text: String): List<String> =
        when {
            fileName.endsWith(".any", true) || fileName.endsWith(".farm", true) ->
                validateDispatcher(text)
            fileName.endsWith(".cnd", true) -> validateCnd(text)
            fileName.endsWith(".config", true) -> validateFelixConfig(text)
            else -> emptyList()
        }

    private fun validateDispatcher(text: String): List<String> = buildList {
        addAll(unbalanced(text, '{', '}', "Dispatcher block"))
        text.lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith('#') && it !in setOf("{", "}") }
            .filter { !it.startsWith('/') && !it.startsWith('"') }
            .firstOrNull()
            ?.let { add("Dispatcher directives must start with '/'") }
    }

    private fun validateCnd(text: String): List<String> = buildList {
        addAll(unbalanced(text, '[', ']', "CND node type declaration"))
        val declarations = text.lineSequence()
            .map(String::trim)
            .filter { it.startsWith('[') }
            .toList()
        if (declarations.isEmpty()) add("CND file does not declare a node type")
    }

    private fun validateFelixConfig(text: String): List<String> = buildList {
        addAll(unbalanced(text, '[', ']', "Felix array value"))
        text.lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith('#') }
            .filter { '=' !in it }
            .firstOrNull()
            ?.let { add("Felix configuration property is missing '='") }
    }

    private fun unbalanced(
        text: String,
        open: Char,
        close: Char,
        label: String,
    ): List<String> {
        var depth = 0
        for (character in text) {
            when (character) {
                open -> depth++
                close -> {
                    depth--
                    if (depth < 0) return listOf("$label has an unexpected '$close'")
                }
            }
        }
        return if (depth == 0) emptyList() else listOf("$label is not closed")
    }
}
