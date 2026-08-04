package com.github.aemtoolkit.server

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.io.IOException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.net.URLEncoder

/**
 * Loads Felix bundle metadata from the configured AEM server.
 */
@Service(Service.Level.PROJECT)
class AemBundleService(private val project: Project) {
    /** Returns bundles exposed by Felix Web Console. */
    @Throws(IOException::class)
    fun getBundles(): List<AemBundle> {
        if (!AemServerSettings.getInstance(project).state.enabled) return emptyList()
        val json = AemServerConnectionService.getInstance(project)
            .getJson("/system/console/bundles.json")
        return AemBundleJsonParser.parse(json)
    }

    /** Opens [bundle] in Felix Web Console. */
    fun openBundle(bundle: AemBundle) {
        AemServerConnectionService.getInstance(project)
            .openServerPath("/system/console/bundles/${bundle.id}")
    }

    /**
     * Executes a Felix lifecycle [action] for [bundle].
     *
     * This modifies the configured AEM instance.
     */
    @Throws(IOException::class)
    fun changeState(bundle: AemBundle, action: AemBundleAction) {
        val body = "action=${URLEncoder.encode(action.parameter, Charsets.UTF_8)}"
            .toByteArray(Charsets.UTF_8)
        val connection = AemServerConnectionService.getInstance(project)
            .createConnection("/system/console/bundles/${bundle.id}")
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8",
            )
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }
            val status = connection.responseCode
            if (status !in 200..399) {
                val response = connection.errorStream
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()
                throw IOException(
                    "Felix ${action.label.lowercase()} returned HTTP $status: $response",
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    /** Downloads [bundle] and attaches its JAR as an IDE project library. */
    @Throws(IOException::class)
    fun fetchAndAttach(bundle: AemBundle): Path {
        val destination = AemBundleCache.jarPath(project, bundle)
        Files.createDirectories(destination.parent)
        val connection = AemServerConnectionService.getInstance(project)
            .createConnection("/system/console/bundles/${bundle.id}.jar")
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("Bundle download returned HTTP $status")
            }

            connection.inputStream.use { input ->
                Files.newOutputStream(destination).use(input::copyTo)
            }
        } finally {
            connection.disconnect()
        }
        attachLibrary(bundle, destination)
        return destination
    }

    private fun attachLibrary(bundle: AemBundle, jarPath: Path) {
        ApplicationManager.getApplication().invokeAndWait {
            WriteAction.run<RuntimeException> {
                val localJar = LocalFileSystem.getInstance()
                    .refreshAndFindFileByNioFile(jarPath)
                    ?: throw IllegalStateException("Downloaded bundle JAR is not visible to VFS")
                val jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(localJar)
                    ?: throw IllegalStateException("Downloaded bundle is not a valid JAR")
                val table = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
                val libraryName = "AEM Bundle: ${bundle.symbolicName}"
                val existing = table.getLibraryByName(libraryName)
                if (existing == null) {
                    val tableModel = table.modifiableModel
                    val library = tableModel.createLibrary(libraryName)
                    tableModel.commit()
                    library.modifiableModel.apply {
                        addRoot(jarRoot, OrderRootType.CLASSES)
                        commit()
                    }
                } else {
                    existing.modifiableModel.apply {
                        getUrls(OrderRootType.CLASSES).forEach {
                            removeRoot(it, OrderRootType.CLASSES)
                        }
                        addRoot(jarRoot, OrderRootType.CLASSES)
                        commit()
                    }
                }

            }
        }
    }

    companion object {
        /** Returns the project bundle service. */
        fun getInstance(project: Project): AemBundleService = project.service()
    }
}

/** Supported Felix bundle lifecycle operations. */
enum class AemBundleAction(
    val parameter: String,
    val label: String,
) {
    START("start", "Start"),
    STOP("stop", "Stop"),
    REFRESH("refresh", "Refresh"),
}
