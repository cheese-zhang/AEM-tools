package com.github.aemtoolkit.clientlib

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

/** Resolves `js.txt` and `css.txt` includes without requiring a custom language. */
object ClientLibraryManifestSupport {
    fun isManifest(file: VirtualFile?): Boolean =
        file?.name in setOf("js.txt", "css.txt")

    fun extension(file: VirtualFile): String =
        if (file.name == "js.txt") "js" else "css"

    fun currentLinePrefix(text: String, offset: Int): String =
        text.substring(0, offset.coerceIn(0, text.length))
            .substringAfterLast('\n')
            .substringBefore("IntellijIdeaRulezzz")
            .trimStart()

    fun basePath(text: String, offset: Int): String? =
        BASE.findAll(text.substring(0, offset.coerceIn(0, text.length)))
            .lastOrNull()
            ?.groupValues
            ?.get(1)

    fun resolveInclude(
        manifest: VirtualFile,
        include: String,
        offset: Int,
    ): VirtualFile? {
        val root = manifest.parent ?: return null
        val base = basePath(manifest.inputStream.bufferedReader().use { it.readText() }, offset)
        val relative = listOfNotNull(base, include)
            .joinToString("/")
            .replace('\\', '/')
            .trim('/')
        val target = root.findFileByRelativePath(relative) ?: return null
        return target.takeIf { VfsUtilCore.isAncestor(root, it, false) }
    }

    fun resolveBase(manifest: VirtualFile, base: String): VirtualFile? {
        val root = manifest.parent ?: return null
        val target = root.findFileByRelativePath(base.trim('/')) ?: return null
        return target.takeIf { it.isDirectory && VfsUtilCore.isAncestor(root, it, false) }
    }

    fun includeCandidates(manifest: VirtualFile, base: String?): List<String> {
        val root = manifest.parent ?: return emptyList()
        val baseDirectory = base?.let { resolveBase(manifest, it) } ?: root
        val expectedExtension = extension(manifest)
        return baseDirectory.children
            .filter { !it.isDirectory && it.extension.equals(expectedExtension, true) }
            .map(VirtualFile::getName)
            .sorted()
    }

    fun baseCandidates(manifest: VirtualFile): List<String> =
        manifest.parent?.children
            ?.filter(VirtualFile::isDirectory)
            ?.map(VirtualFile::getName)
            ?.sorted()
            .orEmpty()

    private val BASE = Regex("""(?m)^\s*#base=([^\s]+)\s*$""")
}
