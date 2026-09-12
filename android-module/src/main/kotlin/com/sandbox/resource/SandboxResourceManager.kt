package com.sandbox.resource

import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Responsável por baixar, retomar, validar e manter o rootfs do sandbox.
 *
 * Este arquivo é independente de Android Framework de propósito (só usa
 * java.net/java.io/java.security) para poder ser testado em JVM pura,
 * sem precisar de emulador. A integração com Context/armazenamento do
 * Android entra como uma camada fina por cima (targetFile já resolvido
 * a partir de filesDir, por exemplo).
 *
 * Nunca baixa para dentro de assets/ nem do APK — sempre para armazenamento
 * privado do app (equivalente a filesDir/sandbox/rootfs/).
 */
class SandboxResourceManager(
    private val targetFile: File
) {

    sealed class DownloadResult {
        data class Success(val file: File) : DownloadResult()
        data class Failure(val reason: String, val cause: Throwable? = null) : DownloadResult()
    }

    /**
     * Callback de progresso: bytes já baixados e total esperado.
     */
    fun interface ProgressListener {
        fun onProgress(bytesDownloaded: Long, totalBytes: Long)
    }

    /**
     * Garante que o recurso descrito pelo manifesto está disponível e válido
     * em [targetFile]. Se já existir e o hash conferir, não baixa de novo.
     * Se existir parcialmente, tenta retomar via header Range.
     */
    fun ensureAvailable(
        manifest: DownloadManifest,
        progressListener: ProgressListener? = null
    ): DownloadResult {
        if (targetFile.exists() && verifySha256(targetFile, manifest.sha256)) {
            return DownloadResult.Success(targetFile)
        }

        return try {
            downloadWithResume(manifest, progressListener)
            if (!verifySha256(targetFile, manifest.sha256)) {
                targetFile.delete()
                return DownloadResult.Failure(
                    "Hash SHA-256 não confere após download. Arquivo removido."
                )
            }
            DownloadResult.Success(targetFile)
        } catch (e: Exception) {
            DownloadResult.Failure("Falha no download: ${e.message}", e)
        }
    }

    private fun downloadWithResume(
        manifest: DownloadManifest,
        progressListener: ProgressListener?
    ) {
        val partialFile = File(targetFile.parentFile, "${targetFile.name}.part")
        var existingBytes = if (partialFile.exists()) partialFile.length() else 0L

        val connection = (URL(manifest.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            if (existingBytes > 0) {
                setRequestProperty("Range", "bytes=$existingBytes-")
            }
        }

        connection.connect()

        val supportsResume = connection.responseCode == HttpURLConnection.HTTP_PARTIAL
        if (!supportsResume) {
            // Servidor não suporta Range: começa do zero.
            existingBytes = 0L
            if (partialFile.exists()) partialFile.delete()
        }

        val totalBytes = manifest.sizeBytes
        val output = RandomAccessFile(partialFile, "rw")
        // Evita deixar bytes antigos no final quando uma retomada falha ou
        // quando o servidor responde 200 (arquivo completo) ao invés de 206.
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

        if (!partialFile.renameTo(targetFile)) {
            throw IllegalStateException("Não foi possível mover .part para o arquivo final")
        }
    }

    private fun verifySha256(file: File, expectedHash: String): Boolean {
        if (!file.exists()) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
        return actualHash.equals(expectedHash, ignoreCase = true)
    }

    /**
     * Remove o rootfs baixado (usado para reset completo do sandbox).
     */
    fun purge() {
        if (targetFile.exists()) targetFile.delete()
        val partial = File(targetFile.parentFile, "${targetFile.name}.part")
        if (partial.exists()) partial.delete()
    }
}
