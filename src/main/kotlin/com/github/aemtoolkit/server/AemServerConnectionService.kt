package com.github.aemtoolkit.server

import com.github.aemtoolkit.resolver.AemRepositoryPath
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64
import java.io.IOException
import java.io.OutputStream

/**
 * Builds server URLs and performs lightweight AEM connection checks.
 */
@Service(Service.Level.PROJECT)
class AemServerConnectionService(private val project: Project) {
    private val settings: AemServerSettings
        get() = AemServerSettings.getInstance(project)

    /** Returns the JCR path represented by [file]. */
    fun repositoryPath(file: VirtualFile): String? =
        AemRepositoryPath.fromFilePath(file.path)
            ?.removeSuffix("/.content.xml")

    /** Opens [repositoryPath] in CRXDE Lite. */
    fun openInCrxde(repositoryPath: String) {
        BrowserUtil.browse(
            "${settings.normalizedBaseUrl()}/crx/de/index.jsp#${encodePath(repositoryPath)}",
        )
    }

    /** Opens the AEM Web Console. */
    fun openWebConsole() {
        BrowserUtil.browse("${settings.normalizedBaseUrl()}/system/console")
    }

    /** Opens AEM Package Manager. */
    fun openPackageManager() {
        BrowserUtil.browse("${settings.normalizedBaseUrl()}/crx/packmgr")
    }

    /** Checks whether the configured server responds with a successful HTTP status. */
    fun testConnection(): ConnectionResult {
        if (!settings.state.enabled) {
            return ConnectionResult(false, "AEM server features are disabled")
        }
        val connection = openConnection(
            "${settings.normalizedBaseUrl()}/libs/granite/core/content/login.html",
        )
        connection.instanceFollowRedirects = true
        return try {
            val status = connection.responseCode
            ConnectionResult(status in 200..399, "HTTP $status")
        } finally {
            connection.disconnect()
        }
    }

    /** Loads a JSON representation of [repositoryPath] from AEM Author. */
    @Throws(IOException::class)
    fun getRepositoryJson(repositoryPath: String): String {
        return getJson("$repositoryPath.1.json")
    }

    /** Loads a JSON endpoint relative to the configured AEM server. */
    @Throws(IOException::class)
    fun getJson(relativePath: String): String {
        val normalizedPath = if (relativePath.startsWith('/')) relativePath else "/$relativePath"
        val connection = openConnection("${settings.normalizedBaseUrl()}$normalizedPath")
        return try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("AEM Author returned HTTP $status")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** Opens a path relative to the configured AEM server in the browser. */
    fun openServerPath(relativePath: String) {
        val normalizedPath = if (relativePath.startsWith('/')) relativePath else "/$relativePath"
        BrowserUtil.browse("${settings.normalizedBaseUrl()}$normalizedPath")
    }

    private fun openConnection(url: String): HttpURLConnection {
        val connection = URI.create(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        val password = settings.getPassword()
        if (password != null) {
            val token = Base64.getEncoder()
                .encodeToString("${settings.state.username}:$password".toByteArray())
            connection.setRequestProperty("Authorization", "Basic $token")
        }
        connection.setRequestProperty("Accept", "application/json")
        return connection
    }

    /** Creates an authenticated connection to a server-relative endpoint. */
    fun createConnection(relativePath: String): HttpURLConnection {
        val normalizedPath = if (relativePath.startsWith('/')) relativePath else "/$relativePath"
        return openConnection("${settings.normalizedBaseUrl()}$normalizedPath")
    }

    private fun encodePath(path: String): String =
        path.split('/').joinToString("/") { segment ->
            if (segment.isEmpty()) "" else java.net.URLEncoder.encode(segment, Charsets.UTF_8)
        }

    companion object {
        /** Returns the project connection service. */
        fun getInstance(project: Project): AemServerConnectionService = project.service()
    }
}

/** Result of an AEM server connectivity check. */
data class ConnectionResult(
    val successful: Boolean,
    val message: String,
)
