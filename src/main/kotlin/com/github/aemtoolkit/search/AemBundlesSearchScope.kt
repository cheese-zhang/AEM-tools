package com.github.aemtoolkit.search

import com.github.aemtoolkit.server.AemBundleCache
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope

/**
 * Search scope containing JARs downloaded by the Felix bundle browser.
 */
class AemBundlesSearchScope(private val currentProject: Project) :
    GlobalSearchScope(currentProject) {
    private val cachePath = AemBundleCache.directory(currentProject)
        .toString()
        .replace('\\', '/')

    override fun contains(file: VirtualFile): Boolean {
        val localFile = JarFileSystem.getInstance().getVirtualFileForJar(file) ?: file
        return localFile.path.replace('\\', '/').startsWith(cachePath)
    }

    override fun isSearchInModuleContent(
        aModule: com.intellij.openapi.module.Module,
    ): Boolean = false

    override fun isSearchInLibraries(): Boolean = true

    override fun getDisplayName(): String = "AEM Felix Bundles"
}
