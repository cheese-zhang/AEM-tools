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

    fun testExtractsDataSlyUseClassPrefix() {
        val text = """<sly data-sly-use.model="com.example.Us"></sly>"""
        val offset = text.indexOf("\"></sly>")

        assertEquals(
            "com.example.Us",
            HtlCompletionContext.useClassPrefix(text, offset),
        )
    }

    fun testExtractsDataSlyUseTemplatePrefix() {
        val text = """<sly data-sly-use.template="./templates/ca"></sly>"""
        val offset = text.indexOf("\"></sly>")

        assertEquals(
            "./templates/ca",
            HtlCompletionContext.useClassPrefix(text, offset),
        )
    }

    fun testExtractsI18nKeyPrefix() {
        val text = """<h2>${'$'}{i18n['navigation.la']}</h2>"""
        val offset = text.indexOf("']}")

        assertEquals(
            "navigation.la",
            HtlCompletionContext.i18nPrefix(text, offset),
        )
    }

    fun testExtractsStandardHtlI18nKeyPrefix() {
        val text = """<h2>${'$'}{'navigation.la' @ i18n}</h2>"""
        val offset = text.indexOf("' @ i18n")

        assertEquals(
            "navigation.la",
            HtlCompletionContext.i18nPrefix(text, offset),
        )
    }

    fun testExtractsNestedModelPropertyChain() {
        val text = """<h2>${'$'}{userProfile.navigation.quick}</h2>"""
        val offset = text.indexOf("}</h2>")

        assertEquals(
            HtlModelAccess(
                variable = "userProfile",
                propertyPrefix = "quick",
                propertyChain = listOf("navigation"),
            ),
            HtlCompletionContext.modelAccess(text, offset),
        )
    }
}
