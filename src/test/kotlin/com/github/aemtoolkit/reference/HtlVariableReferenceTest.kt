package com.github.aemtoolkit.reference

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class HtlVariableReferenceTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        val sourceRoot = myFixture.tempDirFixture.findOrCreateDir("src")
        PsiTestUtil.addSourceRoot(module, sourceRoot)
        myFixture.addFileToProject(
            "src/com/example/Profile.java",
            """
            package com.example;
            public class Profile {
                public String getLabel() { return ""; }
            }
            """.trimIndent(),
        )
    }

    fun testResolvesVariableToUseDeclaration() {
        myFixture.configureByText(
            "card.html",
            """
            <sly data-sly-use.profile="${'$'}{'com.example.Profile'}">
                ${'$'}{pro<caret>file.label}
            </sly>
            """.trimIndent(),
        )

        val target = myFixture.file.findReferenceAt(myFixture.caretOffset)!!.resolve()

        assertEquals("data-sly-use.profile", target!!.text.substringBefore('='))
    }

    fun testGetterRenameUsesHtlPropertyName() {
        myFixture.configureByText(
            "card.html",
            """
            <sly data-sly-use.profile="${'$'}{'com.example.Profile'}">
                ${'$'}{profile.la<caret>bel}
            </sly>
            """.trimIndent(),
        )
        val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)!!

        WriteCommandAction.runWriteCommandAction(project) {
            reference.handleElementRename("getHeading")
        }

        assertTrue(myFixture.file.text.contains("${'$'}{profile.heading}"))
    }

    fun testResolvesTemplatePathAsFileInsteadOfClass() {
        val template = myFixture.addFileToProject(
            "templates/card.html",
            "<template/>",
        )
        myFixture.configureByText(
            "component.html",
            """<sly data-sly-use.card="./templates/ca<caret>rd.html"/>""",
        )

        assertEquals(template, myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve())
    }

    fun testFindsVariableAfterAnotherOperandAndIgnoresEscapedExpression() {
        myFixture.configureByText(
            "card.html",
            """
            <sly data-sly-use.profile="${'$'}{'com.example.Profile'}"
                title="${'$'}{enabled and pro<caret>file.label}"
                data-test="\${'$'}{profile.label}"></sly>
            """.trimIndent(),
        )

        assertNotNull(myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve())
        val escapedOffset = myFixture.file.text.lastIndexOf("profile.label") + 2
        assertNull(myFixture.file.findReferenceAt(escapedOffset))
    }

    fun testPublicFieldRenameDoesNotApplyBeanPrefixConversion() {
        myFixture.addFileToProject(
            "src/com/example/Fields.java",
            "package com.example; public class Fields { public String island; }",
        )
        myFixture.configureByText(
            "fields.html",
            """
            <sly data-sly-use.fields="${'$'}{'com.example.Fields'}">
                ${'$'}{fields.is<caret>land}
            </sly>
            """.trimIndent(),
        )
        val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)!!

        WriteCommandAction.runWriteCommandAction(project) {
            reference.handleElementRename("island")
        }

        assertTrue(myFixture.file.text.contains("${'$'}{fields.island}"))
    }
}
