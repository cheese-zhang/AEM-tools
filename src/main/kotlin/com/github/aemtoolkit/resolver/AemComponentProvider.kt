package com.github.aemtoolkit.resolver

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

/**
 * Extension point for component discovery strategies.
 */
interface AemComponentProvider {
    /** Returns components visible in [project]. */
    fun getComponents(project: Project): Collection<AemComponent>

    companion object {
        val EP_NAME: ExtensionPointName<AemComponentProvider> =
            ExtensionPointName.create("com.github.aemtoolkit.componentProvider")
    }
}
