package com.github.aemtoolkit.caconfig

import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CaConfigServiceTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        val sourceRoot = myFixture.tempDirFixture.findOrCreateDir("src")
        PsiTestUtil.addSourceRoot(module, sourceRoot)
        myFixture.addFileToProject(
            "src/org/apache/sling/caconfig/annotation/Configuration.java",
            """
            package org.apache.sling.caconfig.annotation;
            public @interface Configuration {
                String name() default "";
                String label() default "";
                String description() default "";
                boolean collection() default false;
            }
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "src/org/apache/sling/caconfig/annotation/Property.java",
            """
            package org.apache.sling.caconfig.annotation;
            public @interface Property {
                String label() default "";
                String description() default "";
                int order() default 0;
            }
            """.trimIndent(),
        )
    }

    fun testIndexesDefinitionsResourcesAndConfigRefs() {
        myFixture.addFileToProject(
            "src/com/example/config/SiteConfig.java",
            """
            package com.example.config;
            import org.apache.sling.caconfig.annotation.Configuration;
            import org.apache.sling.caconfig.annotation.Property;
            @Configuration(name = "site", label = "Site Configuration")
            public @interface SiteConfig {
                @Property(label = "API URL")
                String apiUrl() default "https://example.invalid";
                boolean enabled() default true;
            }
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "ui.content/src/main/content/jcr_root/conf/example/_sling_configs/site/.content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                jcr:primaryType="nt:unstructured"
                apiUrl="https://api.example.invalid"
                enabled="{Boolean}true"/>
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "ui.content/src/main/content/jcr_root/content/example/.content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
                jcr:primaryType="cq:Page"
                sling:configRef="/conf/example"/>
            """.trimIndent(),
        )

        val service = CaConfigService.getInstance(project)
        val definition = service.findDefinition("site")!!
        val resource = service.findResources("site").single()
        val reference = service.references().single()

        assertEquals("com.example.config.SiteConfig", definition.qualifiedName)
        assertEquals(listOf("apiUrl", "enabled"), definition.properties.map(CaConfigProperty::name))
        assertEquals("Site Configuration", definition.label)
        assertEquals("/conf/example", resource.contextPath)
        assertEquals("https://api.example.invalid", resource.properties["apiUrl"])
        assertEquals("/content/example", reference.repositoryPath)
        assertEquals("/conf/example", reference.configRoot)
        assertEquals(listOf(resource), service.effectiveResources("/conf/example/site", "site"))
    }
}
