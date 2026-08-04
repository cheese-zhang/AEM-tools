package com.github.aemtoolkit.resolver

import com.github.aemtoolkit.util.AemXmlUtil
import com.github.aemtoolkit.config.CndNamedElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.util.PsiTreeUtil

/**
 * Builds JCR vocabulary from project CND and FileVault files.
 */
class IndexedJcrSchemaProvider : JcrSchemaProvider {
    override fun getDefinitions(project: Project): Collection<JcrDefinition> {
        val scope = GlobalSearchScope.projectScope(project)
        val manager = PsiManager.getInstance(project)
        val cndDefinitions = cndFiles(project, scope)
            .flatMap { file -> manager.findFile(file)?.text?.let(CndSchemaParser::parse).orEmpty() }
        val fileVaultDefinitions = FilenameIndex.getVirtualFilesByName(
            AemXmlUtil.CONTENT_XML,
            scope,
        ).flatMap { file ->
            val xml = manager.findFile(file) as? XmlFile ?: return@flatMap emptyList()
            xml.rootTag?.let(::collectFileVaultDefinitions).orEmpty()
        }
        return (cndDefinitions + fileVaultDefinitions)
            .distinctBy { it.kind to it.name }
    }

    override fun findSource(project: Project, name: String): PsiElement? {
        val manager = PsiManager.getInstance(project)
        return cndFiles(project, GlobalSearchScope.projectScope(project))
            .firstNotNullOfOrNull { file ->
                val psiFile = manager.findFile(file) ?: return@firstNotNullOfOrNull null
                PsiTreeUtil.findChildrenOfType(psiFile, CndNamedElement::class.java)
                    .firstOrNull { it.name == name }
            }
    }

    private fun collectFileVaultDefinitions(root: XmlTag): List<JcrDefinition> =
        buildList {
            fun visit(tag: XmlTag) {
                add(
                    JcrDefinition(
                        tag.localName,
                        JcrDefinitionKind.NODE_NAME,
                        "JCR node name used in project FileVault content.",
                    ),
                )
                tag.attributes
                    .filterNot { it.isNamespaceDeclaration }
                    .forEach { attribute ->
                        add(
                            JcrDefinition(
                                attribute.name,
                                JcrDefinitionKind.PROPERTY,
                                "JCR property used in project FileVault content.",
                            ),
                        )
                    }
                tag.subTags.forEach(::visit)
            }
            visit(root)
        }

    private fun cndFiles(project: Project, scope: GlobalSearchScope) =
        FilenameIndex.getAllFilesByExt(project, "cnd", scope)
}
