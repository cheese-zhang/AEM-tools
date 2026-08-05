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
            HtlModelAccess("userProfile", ""),
            HtlCompletionContext.modelAccess(text, myFixture.caretOffset),
        )
        assertTrue(HtlCompletionContext.isInsideExpression(text, myFixture.caretOffset))
    }

    fun testCompletesModelGetterInAttributeExpression() {
        myFixture.configureByText(
            "profile.html",
            """
            <div data-sly-use.userProfile="${'$'}{'com.example.UserProfile'}"
                 title="${'$'}{userProfile.quick<caret>}"></div>
            """.trimIndent(),
        )

        val text = myFixture.file.text
        assertEquals(
            HtlModelAccess("userProfile", "quick"),
            HtlCompletionContext.modelAccess(text, myFixture.caretOffset),
        )
        assertTrue(HtlCompletionContext.isInsideExpression(text, myFixture.caretOffset))
    }

    fun testRecognizesAnyDeclaredModelVariable() {
        myFixture.addFileToProject(
            "src/com/example/Article.java",
            """
            package com.example;
            public class Article {
                public String getHeadline() { return ""; }
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "article.html",
            """
            <sly data-sly-use.article="${'$'}{'com.example.Article'}"></sly>
            <h1>${'$'}{article.head<caret>}</h1>
            """.trimIndent(),
        )

        val access = HtlCompletionContext.modelAccess(
            myFixture.file.text,
            myFixture.caretOffset,
        )

        assertEquals(HtlModelAccess("article", "head"), access)
        assertEquals(
            listOf("headline"),
            HtlJavaModelResolver.properties(
                myFixture.file.findElementAt(myFixture.caretOffset - 1)!!,
                access!!.variable,
            ).map { it.name },
        )
    }
}
