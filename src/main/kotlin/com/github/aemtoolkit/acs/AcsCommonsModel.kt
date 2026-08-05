package com.github.aemtoolkit.acs

import com.intellij.openapi.vfs.VirtualFile

/** An ACS AEM Commons Generic List and its authored items. */
data class AcsGenericList(
    val name: String,
    val repositoryPath: String,
    val file: VirtualFile,
    val items: List<AcsGenericListItem>,
)

/** A title/value pair in an ACS Generic List. */
data class AcsGenericListItem(val title: String, val value: String)

/** An ACS named image transform OSGi factory configuration. */
data class AcsNamedImageTransform(
    val name: String,
    val transforms: List<String>,
    val file: VirtualFile,
)

/** A redirect managed by ACS Redirect Manager. */
data class AcsRedirectRule(
    val source: String,
    val target: String,
    val statusCode: Int?,
    val repositoryPath: String,
    val file: VirtualFile,
)

/** A shared or global ACS component-properties resource. */
data class AcsSharedProperties(
    val scope: Scope,
    val componentResourceType: String?,
    val repositoryPath: String,
    val file: VirtualFile,
) {
    enum class Scope { GLOBAL, SHARED }
}
