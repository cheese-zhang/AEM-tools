package com.github.aemtoolkit.resolver

import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class HtlJavaModelResolverTest : BasePlatformTestCase() {
    fun testFindsAnnotatedSlingModelsAndJavaUseClasses() {
        val sourceRoot = myFixture.tempDirFixture.findOrCreateDir("src")
        PsiTestUtil.addSourceRoot(module, sourceRoot)
        myFixture.addFileToProject(
            "src/org/apache/sling/models/annotations/Model.java",
            """
            package org.apache.sling.models.annotations;
            public @interface Model {}
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "src/com/example/ProfileModel.java",
            """
            package com.example;
            import org.apache.sling.models.annotations.Model;
            @Model
            public class ProfileModel {}
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "src/com/example/WCMUsePojo.java",
            "package com.example; public class WCMUsePojo {}",
        )
        myFixture.addFileToProject(
            "src/com/example/NavigationUse.java",
            "package com.example; public class NavigationUse extends WCMUsePojo {}",
        )
        myFixture.addFileToProject(
            "src/com/example/Helper.java",
            "package com.example; public class Helper {}",
        )

        assertEquals(
            listOf("com.example.NavigationUse", "com.example.ProfileModel"),
            HtlJavaModelResolver.availableModelClasses(project).mapNotNull { it.qualifiedName },
        )
    }

    fun testResolvesNestedAndIterableProperties() {
        myFixture.addFileToProject(
            "src/com/example/ProfileModel.java",
            """
            package com.example;
            import java.util.List;
            public class ProfileModel {
                public Navigation getNavigation() { return null; }
                public List<Navigation> getItems() { return null; }
            }
            class Navigation {
                public String getLabel() { return ""; }
            }
            """.trimIndent(),
        )
        val html = myFixture.addFileToProject(
            "card.html",
            """
            <sly data-sly-use.profile="${'$'}{'com.example.ProfileModel'}">
                ${'$'}{profile.navigation.label}
            </sly>
            """.trimIndent(),
        )
        assertEquals(
            listOf("label"),
            HtlJavaModelResolver.properties(
                html,
                "profile",
                listOf("navigation"),
            ).map { it.name },
        )
        assertEquals(
            listOf("label"),
            HtlJavaModelResolver.properties(
                html,
                "profile",
                listOf("items"),
            ).map { it.name },
        )
    }
}
