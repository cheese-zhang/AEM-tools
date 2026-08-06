package com.github.aemtoolkit.osgi

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiClass
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class OsgiConfigurationServiceTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        val sourceRoot = myFixture.tempDirFixture.findOrCreateDir("src")
        PsiTestUtil.addSourceRoot(module, sourceRoot)
        myFixture.addFileToProject(
            "src/org/osgi/service/component/annotations/Component.java",
            """
            package org.osgi.service.component.annotations;
            public @interface Component {
                String[] configurationPid() default {};
            }
            """.trimIndent(),
        )
    }

    fun testIndexesRunModeConfigAndMatchesComponent() {
        val config = myFixture.addFileToProject(
            "ui.config/src/main/content/jcr_root/apps/example/osgiconfig/config.author/" +
                "com.example.SearchService.cfg.json",
            """{"enabled":true}""",
        ).virtualFile
        val java = myFixture.addFileToProject(
            "src/com/example/SearchService.java",
            """
            package com.example;
            import org.osgi.service.component.annotations.Component;
            @Component
            public class SearchService {}
            """.trimIndent(),
        )
        val type = PsiTreeUtil.findChildOfType(java, PsiClass::class.java)!!
        val service = OsgiConfigurationService.getInstance(project)

        assertEquals("com.example.SearchService", service.all().single().pid)
        assertEquals(listOf("author"), service.all().single().runModes)
        assertEquals(config, service.findForClass(type).single().file)
        assertEquals(type, service.findClasses(service.all().single()).single())
    }

    fun testUsesExplicitConfigurationPid() {
        val config = myFixture.addFileToProject(
            "ui.config/src/main/content/jcr_root/apps/example/osgiconfig/config/" +
                "example.search.cfg.json",
            "{}",
        ).virtualFile
        val java = myFixture.addFileToProject(
            "src/com/example/SearchService.java",
            """
            package com.example;
            import org.osgi.service.component.annotations.Component;
            @Component(configurationPid = {"example.search"})
            public class SearchService {}
            """.trimIndent(),
        )
        val type = PsiTreeUtil.findChildOfType(java, PsiClass::class.java)!!

        assertContainsElements(
            OsgiConfigurationService.getInstance(project).servicePids(type),
            "example.search",
            "com.example.SearchService",
        )
        val service = OsgiConfigurationService.getInstance(project)
        assertEquals(config, service.findForClass(type).single().file)
        assertEquals(type, service.findClasses(service.findByFile(config)!!).single())
    }

    fun testFindsFelixPropertyValueInXmlConfiguration() {
        myFixture.addFileToProject(
            "src/org/apache/felix/scr/annotations/Component.java",
            "package org.apache.felix.scr.annotations; public @interface Component {}",
        )
        val java = myFixture.addFileToProject(
            "src/com/example/LegacyService.java",
            """
            package com.example;
            import org.apache.felix.scr.annotations.Component;
            @Component
            public class LegacyService {}
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "ui.apps/src/main/content/jcr_root/apps/example/config/" +
                "com.example.LegacyService.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
                jcr:primaryType="sling:OsgiConfig"
                enabled="{Boolean}true"/>
            """.trimIndent(),
        )
        val type = PsiTreeUtil.findChildOfType(java, PsiClass::class.java)!!

        val targets = OsgiConfigurationService.getInstance(project)
            .findPropertyTargets(type, "enabled")

        assertEquals("{Boolean}true", targets.single().value)
    }

    fun testNormalizesFactoryConfigurationNames() {
        assertEquals(
            "com.example.SearchService",
            OsgiConfigurationService.pidFromFileName(
                "com.example.SearchService~site.cfg.json",
            ),
        )
        assertEquals(
            "my-service",
            OsgiConfigurationService.pidFromFileName("my-service.cfg.json"),
        )
        assertEquals(
            "com.example.SearchService",
            OsgiConfigurationService.pidFromFileName(
                "com.example.SearchService-author.xml",
            ),
        )
    }
}
