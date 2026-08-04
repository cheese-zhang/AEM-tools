package com.github.aemtoolkit.reference

import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class HtlJavaGetterGotoDeclarationHandlerTest : BasePlatformTestCase() {
    fun testReturnsAllHtlUsagesForGetterIdentifier() {
        val sourceRoot = myFixture.tempDirFixture.findOrCreateDir("src")
        PsiTestUtil.addSourceRoot(module, sourceRoot)
        val javaFile = myFixture.addFileToProject(
            "src/com/example/CardModel.java",
            """
            package com.example;
            public class CardModel {
                public String getTitle() { return "Title"; }
            }
            """.trimIndent(),
        ) as PsiJavaFile
        myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/card/card.html",
            """
            <div data-sly-use.model="${'$'}{'com.example.CardModel'}">
                <h2>${'$'}{model.title}</h2>
                <span title="${'$'}{model.title}"></span>
            </div>
            """.trimIndent(),
        )
        myFixture.configureFromExistingVirtualFile(javaFile.virtualFile)
        val identifier = javaFile.classes.single()
            .findMethodsByName("getTitle", false)
            .single()
            .nameIdentifier as PsiIdentifier

        val targets = HtlJavaGetterGotoDeclarationHandler()
            .getGotoDeclarationTargets(identifier, identifier.textOffset, myFixture.editor)

        assertNotNull(targets)
        assertEquals(2, targets!!.size)
        assertTrue(targets.all { it.containingFile.name == "card.html" })
    }

    fun testIgnoresNonGetterMethods() {
        val javaFile = myFixture.configureByText(
            "CardModel.java",
            """
            public class CardModel {
                public void refresh() {}
            }
            """.trimIndent(),
        ) as PsiJavaFile
        val identifier = javaFile.classes.single()
            .findMethodsByName("refresh", false)
            .single()
            .nameIdentifier as PsiIdentifier

        val targets = HtlJavaGetterGotoDeclarationHandler()
            .getGotoDeclarationTargets(identifier, identifier.textOffset, myFixture.editor)

        assertNull(targets)
    }
}
