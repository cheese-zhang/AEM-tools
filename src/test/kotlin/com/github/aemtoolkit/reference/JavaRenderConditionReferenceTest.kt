package com.github.aemtoolkit.reference

import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JavaRenderConditionReferenceTest : BasePlatformTestCase() {
    fun testResourceTypeReferenceTargetsJavaRenderCondition() {
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
        val xml = myFixture.configureByText(
            ".content.xml",
            """
            <jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0"
                xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
                jcr:primaryType="nt:unstructured"
                sling:resourceType="example/base/renderconditions/featureflag"/>
            """.trimIndent(),
        ) as XmlFile
        val value = PsiTreeUtil.findChildrenOfType(xml, XmlAttributeValue::class.java)
            .single { it.value == "example/base/renderconditions/featureflag" }
        val reference = value.references.filterIsInstance<ResourceTypeReference>().single()

        val targets = reference.multiResolve(false)
            .mapNotNull { it.element as? PsiClass }

        assertEquals(
            "com.example.aem.renderconditions.FeatureFlagRenderCondition",
            targets.single().qualifiedName,
        )
    }
}
