package com.github.aemtoolkit.completion

import com.github.aemtoolkit.resolver.JcrDefinitionKind
import com.github.aemtoolkit.resolver.JcrSchemaService
import com.github.aemtoolkit.resolver.ResourceTypeResolver
import com.github.aemtoolkit.caconfig.CaConfigService
import com.github.aemtoolkit.acs.AcsCommonsService
import com.github.aemtoolkit.util.AemXmlUtil
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

/**
 * Completes `sling:resourceType` from indexed AEM components.
 */
class ResourceTypeCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withParent(XmlAttributeValue::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet,
                ) {
                    addAemCompletions(parameters.position, result)
                }
            },
        )
    }

    private fun addAemCompletions(position: PsiElement, result: CompletionResultSet) {
        val file = position.containingFile as? XmlFile ?: return
        if (!AemXmlUtil.isContentXml(file)) return
        val attribute = PsiTreeUtil.getParentOfType(position, XmlAttribute::class.java, false)
        if (attribute == null) {
            if (PsiTreeUtil.getParentOfType(position, XmlTag::class.java, false) != null) {
                addDefinitions(position, result, JcrDefinitionKind.NODE_NAME, "JCR node")
            }
            return
        }
        val value = attribute.valueElement

        if (attribute.name == AemXmlUtil.RESOURCE_TYPE && value != null) {
            addResourceTypes(position, result)
            return
        }
        if (attribute.name == AemXmlUtil.PRIMARY_TYPE && value != null) {
            addDefinitions(position, result, JcrDefinitionKind.NODE_TYPE, "JCR node type")
            return
        }
        if (value != null && isGenericListAttribute(attribute)) {
            addGenericLists(position, result)
            return
        }

        if (value == null || !PsiTreeUtil.isAncestor(value, position, false)) {
            if (addCaConfigProperties(file, position, result)) return
            addDefinitions(position, result, JcrDefinitionKind.PROPERTY, "JCR property")
        }

    }

    private fun isGenericListAttribute(attribute: XmlAttribute): Boolean {
        if (attribute.name == "options") {
            return attribute.value.orEmpty().startsWith(AcsCommonsService.GENERIC_LIST_ROOT)
        }
        return attribute.name == "path" &&
            attribute.parent.getAttributeValue(AemXmlUtil.RESOURCE_TYPE) ==
            AcsCommonsService.GENERIC_LIST_DATASOURCE_RESOURCE_TYPE
    }

    private fun addGenericLists(position: PsiElement, result: CompletionResultSet) {
        AcsCommonsService.getInstance(position.project).genericLists().forEach { list ->
            result.addElement(
                LookupElementBuilder.create(list.repositoryPath)
                    .withTypeText("ACS Generic List", true)
                    .withTailText("  ${list.items.size} items"),
            )
        }
    }

    private fun addCaConfigProperties(
        file: XmlFile,
        position: PsiElement,
        result: CompletionResultSet,
    ): Boolean {
        val service = CaConfigService.getInstance(position.project)
        val resource = service.resources().firstOrNull { it.file == file.virtualFile }
            ?: return false
        val definition = service.findDefinition(resource.configName) ?: return false
        definition.properties.forEach { property ->
            result.addElement(
                LookupElementBuilder.create(property.name)
                    .withTypeText(property.type, true)
                    .withTailText(property.label?.let { "  $it" }.orEmpty()),
            )
        }
        return true
    }

    private fun addDefinitions(
        position: PsiElement,
        result: CompletionResultSet,
        kind: JcrDefinitionKind,
        typeText: String,
    ) {
        JcrSchemaService.getInstance(position.project).allDefinitions()
            .filter { it.kind == kind }
            .forEach { definition ->
                result.addElement(
                    LookupElementBuilder.create(definition.name)
                        .withTypeText(typeText, true),
                )
            }
    }

    private fun addResourceTypes(position: PsiElement, result: CompletionResultSet) {
        val fuzzyResult = result.withPrefixMatcher(
            AemFuzzyPrefixMatcher(result.prefixMatcher.prefix),
        )
        ResourceTypeResolver.getInstance(position.project).allComponents().forEach { component ->
            fuzzyResult.addElement(
                LookupElementBuilder.create(component.resourceType)
                    .withPresentableText(component.resourceType)
                    .withTypeText(component.componentPath, true),
            )
        }
    }
}
