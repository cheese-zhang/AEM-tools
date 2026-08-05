package com.github.aemtoolkit.caconfig

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

/** A Java annotation type registered as a Sling Context-Aware Configuration. */
data class CaConfigDefinition(
    val name: String,
    val qualifiedName: String,
    val label: String?,
    val description: String?,
    val collection: Boolean,
    val declaration: PsiClass,
    val properties: List<CaConfigProperty>,
)

/** A property method declared by a CAConfig annotation type. */
data class CaConfigProperty(
    val name: String,
    val type: String,
    val label: String?,
    val description: String?,
    val order: Int,
    val defaultValue: String?,
    val declaration: PsiMethod,
)

/** A project-local CAConfig resource below `/conf/.../sling:configs`. */
data class CaConfigResource(
    val configName: String,
    val contextPath: String,
    val repositoryPath: String,
    val file: VirtualFile,
    val properties: Map<String, String>,
)

/** A `sling:configRef` declaration in project content. */
data class CaConfigReference(
    val repositoryPath: String,
    val configRoot: String,
    val file: VirtualFile,
)
