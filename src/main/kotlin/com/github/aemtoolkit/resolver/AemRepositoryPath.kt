package com.github.aemtoolkit.resolver

/**
 * Normalizes JCR repository paths used in AEM XML attributes.
 */
object AemRepositoryPath {
    /** Returns a canonical absolute repository path. */
    fun normalize(path: String): String =
        "/" + path
            .substringBefore('?')
            .substringBefore('#')
            .removePrefix("file:")
            .replace('\\', '/')
            .trim('/')

    /** Returns the repository path represented by a file below `jcr_root`. */
    fun fromFilePath(filePath: String): String? {
        val normalized = filePath.replace('\\', '/')
        val marker = "/jcr_root/"
        val index = normalized.indexOf(marker)
        if (index < 0) return null
        return "/" + normalized.substring(index + marker.length).trim('/')
    }
}
