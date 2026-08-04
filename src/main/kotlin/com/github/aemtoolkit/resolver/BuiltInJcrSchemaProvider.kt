package com.github.aemtoolkit.resolver

import com.intellij.openapi.project.Project

/**
 * Supplies common JCR, Sling, and AEM definitions without server setup.
 */
class BuiltInJcrSchemaProvider : JcrSchemaProvider {
    override fun getDefinitions(project: Project): Collection<JcrDefinition> =
        nodeTypes + properties

    private val nodeTypes = definitions(
        JcrDefinitionKind.NODE_TYPE,
        "nt:unstructured" to "Unstructured JCR node accepting arbitrary properties and children.",
        "nt:folder" to "Standard JCR folder node.",
        "nt:file" to "Standard JCR file node containing jcr:content.",
        "nt:resource" to "Binary JCR resource node.",
        "sling:Folder" to "Sling folder with unordered children.",
        "sling:OrderedFolder" to "Sling folder preserving child order.",
        "cq:Page" to "AEM page node containing a jcr:content child.",
        "cq:PageContent" to "AEM page content node.",
        "cq:Component" to "AEM component definition.",
        "cq:Template" to "AEM template definition.",
        "cq:ClientLibraryFolder" to "AEM client library folder.",
        "rep:ACL" to "Access-control list node.",
    )

    private val properties = definitions(
        JcrDefinitionKind.PROPERTY,
        "jcr:primaryType" to "Primary JCR node type.",
        "jcr:mixinTypes" to "Mixin node types applied to the node.",
        "jcr:title" to "Human-readable node title.",
        "jcr:description" to "Human-readable node description.",
        "sling:resourceType" to "Sling component used to render the resource.",
        "sling:resourceSuperType" to "Inherited Sling component resource type.",
        "cq:template" to "Template used to create or render an AEM page.",
        "cq:policy" to "Content policy assigned to a component.",
        "cq:styleIds" to "AEM Style System identifiers.",
        "cq:allowedTemplates" to "Template paths allowed below a page.",
        "categories" to "Client library categories.",
        "dependencies" to "Client library category dependencies.",
        "embed" to "Client library categories embedded into this library.",
    )

    private fun definitions(
        kind: JcrDefinitionKind,
        vararg entries: Pair<String, String>,
    ): List<JcrDefinition> =
        entries.map { (name, description) -> JcrDefinition(name, kind, description) }
}
