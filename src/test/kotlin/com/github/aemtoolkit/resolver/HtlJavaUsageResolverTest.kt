package com.github.aemtoolkit.resolver

import com.github.aemtoolkit.reference.HtlJavaPropertyReference
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class HtlJavaUsageResolverTest : BasePlatformTestCase() {
    fun testFindsHtlUsageForJavaGetter() {
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
        val model = javaFile.classes.single()
        val htmlFile = myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/card/card.html",
            """
            <div data-sly-use.model="${'$'}{'com.example.CardModel'}">
                <h2>${'$'}{model.title}</h2>
            </div>
            """.trimIndent(),
        )
        myFixture.configureFromExistingVirtualFile(htmlFile.virtualFile)
        val getter = model.findMethodsByName("getTitle", false).single() as PsiMethod
        val expression = PsiTreeUtil.collectElements(htmlFile) {
            it.text.contains("model.title") && it.references.isNotEmpty()
        }
        assertTrue(expression.isNotEmpty())
        val propertyReference = expression
            .flatMap { it.references.asIterable() }
            .filterIsInstance<HtlJavaPropertyReference>()
            .single()
        assertEquals(
            "com.example.CardModel",
            HtlJavaModelResolver.declaredClassName(propertyReference.element, "model"),
        )
        assertNotNull(HtlJavaModelResolver.resolveModelClass(propertyReference.element, "model"))
        assertTrue(
            PsiManager.getInstance(project)
                .areElementsEquivalent(propertyReference.resolve(), getter),
        )

        val usages = HtlJavaUsageResolver.findUsages(getter)

        assertEquals(1, usages.size)
        assertTrue(usages.single().text.contains("model.title"))
    }
}
