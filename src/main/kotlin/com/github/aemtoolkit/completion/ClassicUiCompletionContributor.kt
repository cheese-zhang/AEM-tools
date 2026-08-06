package com.github.aemtoolkit.completion

import com.github.aemtoolkit.classicui.ClassicUiWidgetRepository
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext

/** Completes Classic UI xtypes and xtype-specific XML attributes. */
class ClassicUiCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet,
                ) {
                    val file = parameters.originalFile as? XmlFile ?: return
                    if (!isClassicUiFile(file)) return
                    val position = parameters.position
                    val attribute = PsiTreeUtil.getParentOfType(
                        position,
                        XmlAttribute::class.java,
                        false,
                    )
                    if (attribute?.name == XTYPE &&
                        PsiTreeUtil.getParentOfType(
                            position,
                            XmlAttributeValue::class.java,
                            false,
                        ) != null
                    ) {
                        ClassicUiWidgetRepository.all().forEach { widget ->
                            result.addElement(
                                LookupElementBuilder.create(widget.xtype)
                                    .withTypeText(widget.description, true),
                            )
                        }
                        return
                    }
                    if (attribute?.valueElement != null) return
                    val tag = attribute?.parent ?: return
                    val widget = tag.getAttributeValue(XTYPE)
                        ?.let(ClassicUiWidgetRepository::find)
                        ?: return
                    widget.fields.forEach { field ->
                        result.addElement(
                            LookupElementBuilder.create(field.name)
                                .withTypeText(field.description, true),
                        )
                    }
                }
            },
        )
    }

    private fun isClassicUiFile(file: XmlFile): Boolean =
        file.name == "dialog.xml" ||
            file.rootTag?.getAttributeValue("jcr:primaryType") == "cq:Dialog" ||
            file.rootTag?.let(::containsXtype) == true

    private fun containsXtype(tag: XmlTag): Boolean =
        tag.getAttribute(XTYPE) != null || tag.subTags.any(::containsXtype)

    private companion object {
        const val XTYPE = "xtype"
    }
}
