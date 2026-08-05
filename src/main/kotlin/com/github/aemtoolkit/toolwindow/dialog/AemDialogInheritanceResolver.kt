package com.github.aemtoolkit.toolwindow.dialog

import com.github.aemtoolkit.resolver.ResourceTypeResolver
import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile

/**
 * Resolves and merges dialog nodes inherited through `sling:resourceSuperType`.
 *
 * Callers must hold IntelliJ read access.
 */
class AemDialogInheritanceResolver(
    private val project: Project,
    private val parser: AemDialogStructureParser,
) {
    /** Resolves inheritance throughout [root]. */
    fun resolve(root: AemDialogNode): AemDialogNode = resolveNode(root, emptySet())

    private fun resolveNode(
        node: AemDialogNode,
        visited: Set<String>,
    ): AemDialogNode {
        val superType = node.resourceSuperType
        val inherited = if (superType != null && superType !in visited) {
            resolveSuperType(superType)
                ?.let { resolveNode(it, visited + superType) }
        } else {
            null
        }
        val merged = when {
            superType == null -> node
            inherited != null -> merge(inherited, node).copy(inheritanceResolved = true)
            else -> node.copy(inheritanceResolved = false)
        }
        return merged.copy(
            children = merged.children.map { child ->
                resolveNode(child, visited + listOfNotNull(superType))
            },
        )
    }

    private fun resolveSuperType(superType: String): AemDialogNode? {
        val reference = parseReference(superType) ?: return null
        val dialog = findDialog(reference.componentResourceType) ?: return null
        val xmlFile = PsiManager.getInstance(project).findFile(dialog) as? XmlFile ?: return null
        val root = parser.parse(xmlFile) ?: return null
        return reference.relativeNodePath.fold(root) { current, segment ->
            current.children.firstOrNull { it.nodeName == segment }
                ?: return null
        }
    }

    private fun findDialog(componentResourceType: String): VirtualFile? {
        ResourceTypeResolver.getInstance(project)
            .resolve(componentResourceType)
            ?.dialog
            ?.let { dialog ->
                return if (dialog.isDirectory) {
                    dialog.findChild(AemXmlUtil.CONTENT_XML)
                } else {
                    dialog
                }
            }
        val suffix = "/jcr_root/apps/$componentResourceType/_cq_dialog/${AemXmlUtil.CONTENT_XML}"
        return FilenameIndex.getVirtualFilesByName(
            AemXmlUtil.CONTENT_XML,
            GlobalSearchScope.projectScope(project),
        ).firstOrNull {
            it.path.replace('\\', '/').endsWith(suffix)
        }
    }

    private fun parseReference(superType: String): DialogSuperTypeReference? {
        val normalized = superType.removePrefix("/apps/").trim('/')
        val marker = "/cq:dialog/"
        val markerIndex = normalized.indexOf(marker)
        if (markerIndex < 1) return null
        return DialogSuperTypeReference(
            componentResourceType = normalized.substring(0, markerIndex),
            relativeNodePath = normalized
                .substring(markerIndex + marker.length)
                .split('/')
                .filter(String::isNotBlank),
        )
    }

    companion object {
        /** Merges a local [overlay] over an [inherited] dialog node. */
        fun merge(inherited: AemDialogNode, overlay: AemDialogNode): AemDialogNode {
            val overlaysByName = overlay.children.associateBy(AemDialogNode::nodeName)
            val mergedChildren = inherited.children.map { inheritedChild ->
                overlaysByName[inheritedChild.nodeName]
                    ?.let { merge(inheritedChild, it) }
                    ?: inheritedChild
            } + overlay.children.filter { child ->
                inherited.children.none { it.nodeName == child.nodeName }
            }
            return inherited.copy(
                nodeName = overlay.nodeName,
                resourceType = overlay.resourceType ?: inherited.resourceType,
                fieldName = overlay.fieldName ?: inherited.fieldName,
                label = overlay.label ?: inherited.label,
                sourceOffset = overlay.sourceOffset,
                children = mergedChildren,
                resourceSuperType = overlay.resourceSuperType,
                inheritanceResolved = true,
                attributes = inherited.attributes + overlay.attributes,
            )
        }
    }

    private data class DialogSuperTypeReference(
        val componentResourceType: String,
        val relativeNodePath: List<String>,
    )
}
