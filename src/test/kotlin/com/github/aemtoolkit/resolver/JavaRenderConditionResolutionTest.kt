package com.github.aemtoolkit.resolver

import com.intellij.psi.PsiClass
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JavaRenderConditionResolutionTest : BasePlatformTestCase() {
    fun testMapsResourceTypeLeafToRenderConditionClass() {
        val sourceRoot = myFixture.tempDirFixture.findOrCreateDir("src")
        PsiTestUtil.addSourceRoot(module, sourceRoot)
        myFixture.addFileToProject(
            "src/com/adobe/granite/ui/components/rendercondition/RenderCondition.java",
            """
            package com.adobe.granite.ui.components.rendercondition;
            public interface RenderCondition {
                boolean check();
            }
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "src/com/example/aem/renderconditions/FeatureFlagRenderCondition.java",
            """
            package com.example.aem.renderconditions;
            import com.adobe.granite.ui.components.rendercondition.RenderCondition;
            public class FeatureFlagRenderCondition implements RenderCondition {
                public boolean check() { return true; }
            }
            """.trimIndent(),
        )

        val targets = AemResourceTypeTargetResolver.getInstance(project)
            .resolve("example/base/renderconditions/featureflag")

        val implementation = targets.filterIsInstance<PsiClass>().single()
        assertEquals(
            "com.example.aem.renderconditions.FeatureFlagRenderCondition",
            implementation.qualifiedName,
        )
    }

    fun testDoesNotMatchUnrelatedJavaClassByNameOnly() {
        val sourceRoot = myFixture.tempDirFixture.findOrCreateDir("src")
        PsiTestUtil.addSourceRoot(module, sourceRoot)
        myFixture.addFileToProject(
            "src/com/example/FeatureFlagRenderCondition.java",
            """
            package com.example;
            public class FeatureFlagRenderCondition {}
            """.trimIndent(),
        )

        assertEmpty(
            AemResourceTypeTargetResolver.getInstance(project)
                .resolve("example/base/renderconditions/featureflag"),
        )
    }
}
