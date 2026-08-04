package com.github.aemtoolkit.resolver

/**
 * A known JCR node type or property exposed to editor assistance.
 */
data class JcrDefinition(
    val name: String,
    val kind: JcrDefinitionKind,
    val description: String,
)

/** Kinds of JCR vocabulary entries. */
enum class JcrDefinitionKind {
    NODE_TYPE,
    NODE_NAME,
    PROPERTY,
}
