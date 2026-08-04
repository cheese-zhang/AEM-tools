package com.github.aemtoolkit.server

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

/**
 * Uploads and installs FileVault packages through AEM Package Manager.
 */
@Service(Service.Level.PROJECT)
class AemPackageService(private val project: Project) {
    /**
     * Uploads [packageFile] and installs the resulting package.
     *
     * This operation modifies the configured AEM instance.
     */
    @Throws(IOException::class)
    fun uploadAndInstall(packageFile: VirtualFile, indicator: ProgressIndicator) {
        require(packageFile.extension.equals("zip", ignoreCase = true)) {
            "A FileVault package must be a ZIP file"
        }
        indicator.text = "Uploading ${packageFile.name}"
        val packagePath = upload(packageFile, indicator)
        indicator.checkCanceled()
        indicator.text = "Installing ${packageFile.name}"
        install(packagePath)
    }

    /**
     * Creates a temporary server-side package for [repositoryPath] and downloads it.
     */
    @Throws(IOException::class)
    fun pullContent(
        repositoryPath: String,
        destination: Path,
        indicator: ProgressIndicator,
    ) {
            require(repositoryPath.startsWith('/')) { "Repository path must be absolute" }
            val group = "aem-toolkit"
            val name = packageName(repositoryPath)
            val version = "1.0"
            val packagePath = "/etc/packages/$group/$name-$version.zip"
            indicator.text = "Creating temporary FileVault package"
            createPackage(group, name, version)
            var operationFailure: Throwable? = null
            try {
                indicator.checkCanceled()
                indicator.text = "Configuring package filter"
                updateFilter(packagePath, group, name, version, repositoryPath)
                indicator.checkCanceled()
                indicator.text = "Building package for $repositoryPath"
                executePackageCommand(packagePath, "build")
                indicator.checkCanceled()
                indicator.text = "Downloading ${destination.fileName}"
                download(packagePath, destination, indicator)
            } catch (error: Throwable) {
                operationFailure = error
                throw error
            } finally {
                indicator.text = "Removing temporary package"
                try {
                    executePackageCommand(packagePath, "delete")
                } catch (cleanupError: Throwable) {
                    operationFailure?.addSuppressed(cleanupError) ?: throw cleanupError
                }
            }
        }

    private fun createPackage(group: String, name: String, version: String) {
            val path = "/crx/packmgr/service/.json/etc/packages/$group/$name?cmd=create"
            val response = postForm(
                path,
                mapOf(
                    "packageName" to name,
                    "groupName" to group,
                    "version" to version,
                ),
            )
            requireSuccess(response, "Package creation failed")
        }

    private fun updateFilter(
            packagePath: String,
            group: String,
            name: String,
            version: String,
            repositoryPath: String,
        ) {
            val escapedRoot = repositoryPath
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
            val response = postForm(
                "/crx/packmgr/update.jsp",
                mapOf(
                    "path" to packagePath,
                    "packageName" to name,
                    "groupName" to group,
                    "version" to version,
                    "filter" to """[{"root":"$escapedRoot","rules":[]}]""",
                ),
            )
            requireSuccess(response, "Package filter update failed")
        }

    private fun executePackageCommand(packagePath: String, command: String) {
            val encodedPath = packagePath.split('/')
                .joinToString("/") { segment ->
                    if (segment.isEmpty()) "" else URLEncoder.encode(segment, Charsets.UTF_8)
                }
            val connection = AemServerConnectionService.getInstance(project)
                .createConnection("/crx/packmgr/service/.json$encodedPath?cmd=$command")
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.outputStream.use { it.write(ByteArray(0)) }
            val response = readResponse(connection)
            requireSuccess(response, "Package $command failed")
        }

    private fun download(
            packagePath: String,
            destination: Path,
            indicator: ProgressIndicator,
        ) {
            val connection = AemServerConnectionService.getInstance(project)
                .createConnection(packagePath)
            try {
                val status = connection.responseCode
                if (status !in 200..299) {
                    throw IOException("Package download returned HTTP $status")
                }
                destination.parent?.let(Files::createDirectories)
                connection.inputStream.use { input ->
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
            } finally {
                connection.disconnect()
            }
        }

    private fun postForm(path: String, fields: Map<String, String>): String {
            val body = fields.entries.joinToString("&") { (name, value) ->
                "${URLEncoder.encode(name, Charsets.UTF_8)}=" +
                    URLEncoder.encode(value, Charsets.UTF_8)
            }.toByteArray(Charsets.UTF_8)
            val connection = AemServerConnectionService.getInstance(project)
                .createConnection(path)
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8",
            )
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }
            return readResponse(connection)
        }

    private fun requireSuccess(response: String, fallback: String) {
            if (AemJsonFields.read(response, "success") == "false") {
                throw IOException(AemJsonFields.read(response, "msg") ?: fallback)
            }
        }

    private fun packageName(repositoryPath: String): String {
            val base = repositoryPath.trim('/').replace(Regex("""[^A-Za-z0-9._-]+"""), "-")
                .ifBlank { "root" }
                .takeLast(60)
            return "$base-${Instant.now().epochSecond}"
        }
    private fun upload(
        packageFile: VirtualFile,
        indicator: ProgressIndicator,
    ): String {
        val boundary = "AemToolkit-${UUID.randomUUID()}"
        val connection = AemServerConnectionService.getInstance(project)
            .createConnection("/crx/packmgr/service/.json/?cmd=upload&force=true")
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.outputStream.buffered().use { output ->
            writeAscii(output, "--$boundary\r\n")
            writeAscii(
                output,
                "Content-Disposition: form-data; name=\"package\"; " +
                    "filename=\"${packageFile.name}\"\r\n",
            )
            writeAscii(output, "Content-Type: application/zip\r\n\r\n")
            packageFile.inputStream.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    indicator.checkCanceled()
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
            }
            writeAscii(output, "\r\n--$boundary--\r\n")
        }
        val response = readResponse(connection)
        return AemJsonFields.read(response, "path")
            ?: throw IOException("Package Manager did not return an uploaded package path")
    }

    private fun install(packagePath: String) {
        executePackageCommand(packagePath, "install")
    }

    private fun readResponse(connection: HttpURLConnection): String =
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IOException("AEM Package Manager returned HTTP $status: $response")
            }
            response
        } finally {
            connection.disconnect()
        }

    private fun writeAscii(output: java.io.OutputStream, value: String) {
        output.write(value.toByteArray(Charsets.UTF_8))
    }

    companion object {
        /** Returns the project package service. */
        fun getInstance(project: Project): AemPackageService = project.service()
    }
}
