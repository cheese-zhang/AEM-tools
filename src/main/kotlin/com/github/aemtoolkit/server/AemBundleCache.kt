package com.github.aemtoolkit.server

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import java.nio.file.Path

/** Resolves persistent cache paths for downloaded Felix bundles. */
object AemBundleCache {
    /** Returns the cache directory dedicated to [project]. */
    fun directory(project: Project): Path =
        PathManager.getSystemDir()
            .resolve("aem-toolkit")
            .resolve(project.locationHash)
            .resolve("bundles")

    /** Returns a filesystem-safe JAR path for [bundle]. */
    fun jarPath(project: Project, bundle: AemBundle): Path {
        val name = bundle.symbolicName.replace(Regex("""[^A-Za-z0-9._-]+"""), "-")
        val version = bundle.version
            ?.replace(Regex("""[^A-Za-z0-9._-]+"""), "-")
            ?.let { "-$it" }
            .orEmpty()
        return directory(project).resolve("$name$version-${bundle.id}.jar")
    }
}
