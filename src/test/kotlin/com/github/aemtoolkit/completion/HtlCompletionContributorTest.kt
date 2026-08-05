package com.github.aemtoolkit.completion

import com.github.aemtoolkit.resolver.HtlJavaModelResolver
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class HtlCompletionContributorTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        val sourceRoot = myFixture.tempDirFixture.findOrCreateDir("src")
        PsiTestUtil.addSourceRoot(module, sourceRoot)
        myFixture.addFileToProject(
            "src/com/example/UserProfile.java",
            """
            package com.example;
            public class UserProfile {
                public String getQuickNavigationLabel() { return ""; }
                public boolean isNavigationVisible() { return true; }
            }
            """.trimIndent(),
        )
    }

    fun testCompletesModelGetterInTextExpression() {
        myFixture.configureByText(
            "profile.html",
            """
            <div data-sly-use.userProfile="${'$'}{'com.example.UserProfile'}">
                ${'$'}{userProfile.<caret>}
            </div>
            """.trimIndent(),
        )
        assertContainsElements(
            HtlJavaModelResolver.properties(
                myFixture.file.findElementAt(myFixture.caretOffset - 1)!!,
                "userProfile",
            ).map { it.name },
            "quickNavigationLabel",
            "navigationVisible",
        )

        val text = myFixture.file.text
        assertEquals(
            "userProfile",
            HtlCompletionContext.modelVariable(text, myFixture.caretOffset),
        )
        assertTrue(HtlCompletionContext.isInsideExpression(text, myFixture.caretOffset))
    }

    fun testCompletesModelGetterInAttributeExpression() {
        myFixture.configureByText(
            "profile.html",
            """
            <div data-sly-use.userProfile="${'$'}{'com.example.UserProfile'}"
                 title="${'$'}{userProfile.<caret>}"></div>
            """.trimIndent(),
        )

        val text = myFixture.file.text
        assertEquals(
            "userProfile",
            HtlCompletionContext.modelVariable(text, myFixture.caretOffset),
        )
        assertTrue(HtlCompletionContext.isInsideExpression(text, myFixture.caretOffset))
    }
}
