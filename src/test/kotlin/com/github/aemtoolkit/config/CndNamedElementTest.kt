package com.github.aemtoolkit.config

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CndNamedElementTest : BasePlatformTestCase() {
    fun testParsesRenameableNodeAndPropertySymbols() {
        val file = myFixture.configureByText(
            AemConfigFileType(),
            """
            [app:Page] > cq:Page
            - app:title (string)
            """.trimIndent(),
        )
        val symbols = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(
            file,
            CndNamedElement::class.java,
        ).toList()

        assertEquals(listOf("app:Page", "app:title"), symbols.map { it.name })
    }
}
