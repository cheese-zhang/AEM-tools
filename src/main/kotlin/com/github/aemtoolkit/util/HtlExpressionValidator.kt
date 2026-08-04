package com.github.aemtoolkit.util

/**
 * Lightweight structural validation for embedded HTL expressions.
 */
object HtlExpressionValidator {
    /** Returns an error message when expression delimiters are unbalanced. */
    fun validate(value: String): String? {
        var cursor = 0
        while (cursor < value.length) {
            val start = value.indexOf("\${", cursor)
            if (start < 0) return null
            val end = value.indexOf('}', start + 2)
            if (end < 0) return "HTL expression is missing a closing brace"
            if (end == start + 2) return "HTL expression cannot be empty"
            cursor = end + 1
        }
        return null
    }
}
