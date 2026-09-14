package com.sandbox.resource

import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class SandboxResourceManager(
    private val targetFile: File,
    private val rootfsSignatureVerifier: RootfsSignatureVerifier? = null,
    private val connectionFactory: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
    private val addressResolver: (String) -> Array<InetAddress> = { InetAddress.getAllByName(it) }
) {
    sealed class DownloadResult {
        data class Success(val file: File) : DownloadResult()
        data class Failure(val reason: String, val cause: Throwable? = null) : DownloadResult()
    }

    fun interface ProgressListener {
        fun onProgress(bytesDownloaded: Long, totalBytes: Long)
    }

    fun ensureAvailable(manifest: DownloadManifest, progressListener: ProgressListener? = null): DownloadResult {
        if (targetFile.exists() && verifyArtifact(targetFile, manifest)) return DownloadResult.Success(targetFile)
        return try {
            downloadWithResume(manifest, progressListener)
            if (!verifyArtifact(targetFile, manifest)) {
                targetFile.delete()
                return DownloadResult.Failure("Hash SHA-256 ou assinatura não confere após download. Arquivo removido.")
            }
            DownloadResult.Success(targetFile)
        } catch (e: Exception) {
            DownloadResult.Failure("Falha no download: ${e.message}", e)
        }
    }

    private fun downloadWithResume(manifest: DownloadManifest, progressListener: ProgressListener?) {
        val partialFile = File(targetFile.parentFile, "${targetFile.name}.part")
        var existingBytes = if (partialFile.exists()) partialFile.length() else 0L
        var currentUrl = URL(manifest.url)
        require(currentUrl.protocol.equals("https", ignoreCase = true)) { "RootFS exige HTTPS" }
        require(currentUrl.userInfo == null && currentUrl.ref == null) { "destino RootFS inválido ou reservado" }

        var redirects = 0
        var connection: HttpURLConnection
        while (true) {
            val validatedAddress = resolveValidatedAddress(currentUrl.host)
            val pinnedUrl = URL(currentUrl.protocol, validatedAddress.hostAddress, currentUrl.port, currentUrl.file)
            connection = connectionFactory(pinnedUrl).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                instanceFollowRedirects = false
                setRequestProperty("Host", hostHeader(currentUrl))
                if (existingBytes > 0) setRequestProperty("Range", "bytes=$existingBytes-")
            }
            if (connection is HttpsURLConnection) {
                connection.hostnameVerifier = HostnameVerifier { _, session ->
                    HttpsURLConnection.getDefaultHostnameVerifier().verify(currentUrl.host, session)
                }
                connection.sslSocketFactory = SniSocketFactory(connection.sslSocketFactory, currentUrl.host)
            }
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode !in 300..399) break

            val location = connection.getHeaderField("Location")
                ?: throw IllegalStateException("redirect de RootFS sem Location")
            val redirectUrl = URL(currentUrl, location)
            require(redirectUrl.protocol.equals("https", ignoreCase = true)) { "redirect RootFS HTTPS -> HTTP bloqueado" }
            require(redirectUrl.userInfo == null && redirectUrl.ref == null) { "destino RootFS inválido ou reservado" }
            redirects++
            require(redirects <= 5) { "cadeia de redirects RootFS excedeu o limite" }
            connection.disconnect()
            currentUrl = redirectUrl
            existingBytes = if (partialFile.exists()) partialFile.length() else 0L
        }

        val supportsResume = connection.responseCode == HttpURLConnection.HTTP_PARTIAL
        if (!supportsResume) {
            existingBytes = 0L
            if (partialFile.exists()) partialFile.delete()
        }
        val totalBytes = manifest.sizeBytes
        val output = RandomAccessFile(partialFile, "rw")
        output.setLength(existingBytes)
        output.seek(existingBytes)
        connection.inputStream.use { input ->
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var totalDownloaded = existingBytes
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalDownloaded += bytesRead
                progressListener?.onProgress(totalDownloaded, totalBytes)
            }
        }
        output.close()
        connection.disconnect()
        runCatching {
            java.nio.file.Files.move(partialFile.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
        }.recoverCatching {
            java.nio.file.Files.move(partialFile.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }.getOrElse { throw IllegalStateException("Não foi possível mover .part para o arquivo final: ${it.message}", it) }
    }

    private fun verifySha256(file: File, expectedHash: String): Boolean {
        if (!file.exists()) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) digest.update(buffer, 0, bytesRead)
        }
        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
        return actualHash.equals(expectedHash, ignoreCase = true)
    }

    private fun verifyArtifact(file: File, manifest: DownloadManifest): Boolean {
        if (!verifySha256(file, manifest.sha256)) return false
        val rootfs = manifest as? RootfsManifest ?: return true
        if (!rootfs.signatureRequired) return true
        return rootfsSignatureVerifier?.verify(file, rootfs) == true
    }

    private fun resolveValidatedAddress(host: String): InetAddress {
        if (host.isBlank() || host.equals("localhost", true) || host.endsWith(".localhost", true)) error("destino RootFS inválido ou reservado")
        val addresses = runCatching { addressResolver(host) }.getOrNull() ?: error("hostname RootFS não pôde ser resolvido")
        require(addresses.isNotEmpty() && addresses.none { address ->
            address.isLoopbackAddress || address.isSiteLocalAddress || address.isLinkLocalAddress || address.isAnyLocalAddress || address.isMulticastAddress || isMappedPrivate(address.address) || isUla(address.address)
        }) { "destino RootFS inválido ou reservado" }
        return addresses.first()
    }

    private fun hostHeader(url: URL): String = if (url.port == -1 || (url.protocol == "https" && url.port == 443)) url.host else "${url.host}:${url.port}"

    private fun isMappedPrivate(bytes: ByteArray): Boolean {
        if (bytes.size != 16 || !bytes.copyOfRange(0, 10).all { it == 0.toByte() } || bytes[10] != 0xff.toByte() || bytes[11] != 0xff.toByte()) return false
        val a = bytes[12].toInt() and 0xff
        val b = bytes[13].toInt() and 0xff
        return a == 10 || a == 127 || (a == 169 && b == 254) || (a == 172 && b in 16..31) || (a == 192 && b == 168)
    }

    private fun isUla(bytes: ByteArray): Boolean = bytes.size == 16 && ((bytes[0].toInt() and 0xff) in 0xfc..0xfd)

    fun purge() {
        if (targetFile.exists()) targetFile.delete()
        val partial = File(targetFile.parentFile, "${targetFile.name}.part")
        if (partial.exists()) partial.delete()
    }
}

private class SniSocketFactory(private val delegate: SSLSocketFactory, private val hostname: String) : SSLSocketFactory() {
    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites
    override fun createSocket(host: String, port: Int): java.net.Socket = configure(delegate.createSocket(host, port))
    override fun createSocket(host: String, port: Int, localAddress: java.net.InetAddress, localPort: Int): java.net.Socket = configure(delegate.createSocket(host, port, localAddress, localPort))
    override fun createSocket(host: java.net.InetAddress, port: Int): java.net.Socket = configure(delegate.createSocket(host, port))
    override fun createSocket(address: java.net.InetAddress, port: Int, localAddress: java.net.InetAddress, localPort: Int): java.net.Socket = configure(delegate.createSocket(address, port, localAddress, localPort))
    override fun createSocket(socket: java.net.Socket, host: String, port: Int, autoClose: Boolean): java.net.Socket = configure(delegate.createSocket(socket, host, port, autoClose))
    private fun configure(socket: java.net.Socket): java.net.Socket {
        if (socket is SSLSocket) {
            val parameters = socket.sslParameters
            parameters.serverNames = listOf(SNIHostName(hostname))
            socket.sslParameters = parameters
        }
        return socket
    }
}
