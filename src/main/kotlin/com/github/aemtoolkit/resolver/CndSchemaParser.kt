package com.github.aemtoolkit.resolver

/**
 * Extracts node types and property definitions from CND source text.
 */
object CndSchemaParser {
    private val nodeType = Regex("""(?m)^\s*\[([^\]]+)]""")
    private val property = Regex("""(?m)^\s*-\s+([^\s(=]+)""")

    /** Returns JCR definitions declared by [text]. */
    fun parse(text: String): List<JcrDefinition> =
        buildList {
            nodeType.findAll(text).forEach { match ->
                val name = match.groupValues[1].trim()
                add(
                    JcrDefinition(
                        name,
                        JcrDefinitionKind.NODE_TYPE,
                        "Node type declared in the project CND schema.",
                    ),
                )
            }
            property.findAll(text).forEach { match ->
                val name = match.groupValues[1].trim()
                if (name != "*") {
                    add(
                        JcrDefinition(
                            name,
                            JcrDefinitionKind.PROPERTY,
                            "Property declared in the project CND schema.",
                        ),
                    )
                }
            }
        }.distinctBy { it.kind to it.name }
}
