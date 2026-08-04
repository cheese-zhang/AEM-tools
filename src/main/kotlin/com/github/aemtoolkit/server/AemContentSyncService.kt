package com.github.aemtoolkit.server

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.Base64
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Synchronizes selected files and directories directly through AEM WebDAV.
 */
@Service(Service.Level.PROJECT)
class AemContentSyncService(private val project: Project) {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /** Uploads [file] directly to its matching repository path. */
    @Throws(IOException::class)
    fun upload(file: VirtualFile, indicator: ProgressIndicator): Int {
        val selection = selection(file)
        if (Files.isRegularFile(selection.source)) {
            uploadFile(selection.source, selection.repositoryPath, indicator)
            return 1
        }
        var uploaded = 0
        Files.walk(selection.source).use { paths ->
            paths.forEach { path ->
                indicator.checkCanceled()
                val relative = selection.source.relativize(path).toRepositoryPath()
                val remotePath = selection.repositoryPath.append(relative)
                if (Files.isDirectory(path)) {
                    createCollection(remotePath)
                } else {
                    uploadFile(path, remotePath, indicator)
                    uploaded++
                }
            }
        }
        return uploaded
    }

    /** Downloads [file]'s matching repository resource directly into the project. */
    @Throws(IOException::class)
    fun download(file: VirtualFile, indicator: ProgressIndicator): Int {
        val selection = selection(file)
        val downloaded = if (Files.isDirectory(selection.source)) {
            downloadDirectory(selection, indicator)
        } else {
            get(selection.repositoryPath, selection.source, indicator)
            1
        }
        LocalFileSystem.getInstance()
            .refreshAndFindFileByNioFile(selection.jcrRoot)
            ?.let { VfsUtil.markDirtyAndRefresh(false, true, true, it) }
        return downloaded
    }

    /** Returns synchronization metadata for [file], or null outside `jcr_root`. */
    fun selectionOrNull(file: VirtualFile): AemContentSelection? =
        runCatching { selection(file) }.getOrNull()

    private fun selection(file: VirtualFile): AemContentSelection {
        val repositoryPath = com.github.aemtoolkit.resolver.AemRepositoryPath.fromFilePath(file.path)
            ?: throw IllegalArgumentException("The selected resource must be below jcr_root")
        val source = file.toNioPath().toAbsolutePath().normalize()
        val jcrRoot = generateSequence(source) { it.parent }
            .firstOrNull { it.fileName?.toString() == "jcr_root" }
            ?: throw IllegalArgumentException("The selected resource must be below jcr_root")
        return AemContentSelection(source, jcrRoot, repositoryPath)
    }

    private fun put(source: Path, repositoryPath: String, indicator: ProgressIndicator) {
        ensureCollectionHierarchy(repositoryPath.substringBeforeLast('/', ""))
        indicator.text = "Uploading $repositoryPath"
        val request = request(repositoryPath)
            .PUT(HttpRequest.BodyPublishers.ofFile(source))
            .build()
        val response = send(request, HttpResponse.BodyHandlers.ofString())
        requireStatus(response, setOf(200, 201, 204))
    }

    private fun uploadFile(
        source: Path,
        repositoryPath: String,
        indicator: ProgressIndicator,
    ) {
        if (source.fileName.toString() == ".content.xml") {
            uploadContentXml(
                source,
                repositoryPath.removeSuffix("/.content.xml"),
                indicator,
            )
        } else {
            put(source, repositoryPath, indicator)
        }
    }

    private fun uploadContentXml(
        source: Path,
        repositoryPath: String,
        indicator: ProgressIndicator,
    ) {
        val root = AemContentXmlParser.parse(source, repositoryPath)
        root.flatten().forEach { node ->
            indicator.checkCanceled()
            indicator.text = "Updating JCR node ${node.repositoryPath}"
            postNode(node)
        }
    }

    private fun postNode(node: AemContentXmlNode) {
        val fields = buildList {
            node.properties.forEach { property ->
                property.type?.let { type ->
                    val typeHint = if (property.values.size > 1) "$type[]" else type
                    add("${property.name}@TypeHint" to typeHint)
                }
                property.values.forEach { value -> add(property.name to value) }
            }
        }.ifEmpty {
            listOf("jcr:primaryType" to "nt:unstructured")
        }
        val body = fields.joinToString("&") { (name, value) ->
            "${encodeForm(name)}=${encodeForm(value)}"
        }
        val request = slingRequest(node.repositoryPath)
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = send(request, HttpResponse.BodyHandlers.ofString())
        requireStatus(response, setOf(200, 201))
    }

    private fun ensureCollectionHierarchy(repositoryPath: String) {
        if (repositoryPath.isBlank() || repositoryPath == "/") return
        var current = ""
        repositoryPath.trim('/').split('/').forEach { segment ->
            current += "/$segment"
            createCollection(current)
        }
    }

    private fun createCollection(repositoryPath: String) {
        val request = request(repositoryPath)
            .method("MKCOL", HttpRequest.BodyPublishers.noBody())
            .build()
        val response = send(request, HttpResponse.BodyHandlers.ofString())
        requireStatus(response, setOf(200, 201, 204, 405))
    }

    private fun downloadDirectory(selection: AemContentSelection, indicator: ProgressIndicator): Int {
        val entries = list(selection.repositoryPath)
        var downloaded = 0
        entries.forEach { entry ->
            indicator.checkCanceled()
            val relative = entry.path.removePrefix(selection.repositoryPath).trimStart('/')
            if (relative.isBlank()) return@forEach
            val target = selection.source.resolve(relative).normalize()
            if (!target.startsWith(selection.source)) {
                throw IOException("Unsafe WebDAV path: ${entry.path}")
            }
            if (entry.collection) {
                Files.createDirectories(target)
            } else {
                get(entry.path, target, indicator)
                downloaded++
            }
        }
        return downloaded
    }

    private fun get(repositoryPath: String, destination: Path, indicator: ProgressIndicator) {
        indicator.text = "Downloading $repositoryPath"
        val request = request(repositoryPath).GET().build()
        val response = send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() != 200) {
            response.body().close()
            throw IOException("AEM WebDAV returned HTTP ${response.statusCode()}")
        }
        response.body().use { input ->
            destination.parent?.let(Files::createDirectories)
            Files.newOutputStream(destination).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    indicator.checkCanceled()
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
            }
        }
    }

    private fun list(repositoryPath: String): List<WebDavEntry> {
        val body = """
                <?xml version="1.0" encoding="UTF-8"?>
                <d:propfind xmlns:d="DAV:"><d:prop><d:resourcetype/></d:prop></d:propfind>
                """.trimIndent()
        val request = request(repositoryPath)
            .header("Depth", "infinity")
            .header("Content-Type", "application/xml; charset=UTF-8")
            .method("PROPFIND", HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() != 207) {
            throw IOException("AEM WebDAV returned HTTP ${response.statusCode()}")
        }
        return parseListing(response.body())
    }

    private fun request(repositoryPath: String): HttpRequest.Builder {
        return authenticatedRequest(
            "${AemServerSettings.getInstance(project).normalizedBaseUrl()}" +
                "$WEB_DAV_PREFIX${repositoryPath.encodeRepositoryPath()}",
        )
    }

    private fun slingRequest(repositoryPath: String): HttpRequest.Builder =
        authenticatedRequest(
            "${AemServerSettings.getInstance(project).normalizedBaseUrl()}" +
                repositoryPath.encodeRepositoryPath(),
        )

    private fun authenticatedRequest(url: String): HttpRequest.Builder {
        val settings = AemServerSettings.getInstance(project)
        val builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(2))
        settings.getPassword()?.let { password ->
            val token = Base64.getEncoder()
                .encodeToString("${settings.state.username}:$password".toByteArray())
            builder.header("Authorization", "Basic $token")
        }
        return builder
    }

    private fun encodeForm(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")

    private fun <T> send(
        request: HttpRequest,
        handler: HttpResponse.BodyHandler<T>,
    ): HttpResponse<T> =
        try {
            client.send(request, handler)
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("AEM WebDAV request was interrupted", error)
        }

    private fun requireStatus(response: HttpResponse<String>, expected: Set<Int>) {
        if (response.statusCode() !in expected) {
            val message = response.body()
            throw IOException(
                "AEM WebDAV returned HTTP ${response.statusCode()}" +
                    if (message.isBlank()) "" else ": $message",
            )
        }
    }

    internal fun parseListing(xml: ByteArray): List<WebDavEntry> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        val document = factory.newDocumentBuilder().parse(xml.inputStream())
        val responses = document.getElementsByTagNameNS("DAV:", "response")
        return (0 until responses.length).mapNotNull { index ->
            val response = responses.item(index) as? Element ?: return@mapNotNull null
            val href = response.getElementsByTagNameNS("DAV:", "href").item(0)?.textContent
                ?: return@mapNotNull null
            val rawPath = runCatching { URI.create(href).path }.getOrNull() ?: href
            val path = URLDecoder.decode(rawPath, Charsets.UTF_8)
                .removePrefix(WEB_DAV_PREFIX)
                .trimEnd('/')
                .ifBlank { "/" }
            val collection = response.getElementsByTagNameNS("DAV:", "collection").length > 0
            WebDavEntry(path, collection)
        }
    }

    private fun String.encodeRepositoryPath(): String =
        split('/').joinToString("/") { segment ->
            if (segment.isEmpty()) "" else URLEncoder.encode(segment, Charsets.UTF_8).replace("+", "%20")
        }

    private fun String.append(relative: String): String =
        if (relative.isBlank()) this else "${trimEnd('/')}/$relative"

    private fun Path.toRepositoryPath(): String =
        joinToString("/") { it.toString() }

    companion object {
        private const val WEB_DAV_PREFIX = "/crx/repository/crx.default"

        /** Returns the project content synchronization service. */
        fun getInstance(project: Project): AemContentSyncService = project.service()
    }
}

/** Local and repository paths represented by a selected FileVault resource. */
data class AemContentSelection(
    val source: Path,
    val jcrRoot: Path,
    val repositoryPath: String,
)

/** A resource returned by a WebDAV directory listing. */
data class WebDavEntry(
    val path: String,
    val collection: Boolean,
)
