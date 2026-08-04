package com.github.aemtoolkit.util

import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlAttribute

/**
 * Shared helpers for HTML Template Language editor features.
 */
object HtlUtil {
    val blockStatements = linkedMapOf(
        "data-sly-use" to "Initializes a Java, JavaScript, or template use-object.",
        "data-sly-test" to "Conditionally keeps the host element.",
        "data-sly-list" to "Repeats the host element for each item.",
        "data-sly-repeat" to "Repeats only the host element content.",
        "data-sly-resource" to "Includes a Sling resource.",
        "data-sly-include" to "Includes another script file.",
        "data-sly-template" to "Declares a reusable HTL template.",
        "data-sly-call" to "Calls a declared HTL template.",
        "data-sly-unwrap" to "Removes the host element while retaining its content.",
        "data-sly-element" to "Changes the rendered element name.",
        "data-sly-attribute" to "Sets one or more HTML attributes.",
        "data-sly-text" to "Replaces the element content with escaped text.",
        "data-sly-set" to "Assigns an expression to a local variable.",
    )

    val globalObjects = linkedMapOf(
        "properties" to "ValueMap of the current resource.",
        "pageProperties" to "Properties of the current page.",
        "inheritedPageProperties" to "Inherited properties of the current page.",
        "resource" to "Current Sling Resource.",
        "currentPage" to "Current AEM Page.",
        "component" to "Current AEM Component.",
        "componentContext" to "Current component context.",
        "wcmmode" to "Current AEM authoring mode.",
        "request" to "Current Sling HTTP request.",
        "log" to "SLF4J logger.",
    )

    /** Returns true for HTL-capable HTML files. */
    fun isHtlFile(file: PsiFile): Boolean =
        file.virtualFile?.extension.equals("html", ignoreCase = true)

    /** Returns the base statement without an optional variable suffix. */
    fun statementName(attribute: XmlAttribute): String =
        attribute.name.substringBefore('.')
}
