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
    private val targetFile: File,
    private val rootfsSignatureVerifier: RootfsSignatureVerifier? = null
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
        if (targetFile.exists() && verifyArtifact(targetFile, manifest)) {
            return DownloadResult.Success(targetFile)
        }

        return try {
            downloadWithResume(manifest, progressListener)
            if (!verifyArtifact(targetFile, manifest)) {
                targetFile.delete()
                return DownloadResult.Failure(
                    "Hash SHA-256 ou assinatura não confere após download. Arquivo removido."
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

        // File.renameTo() usa rename(2) cru: no Linux ele falha (retorna
        // false, sem detalhe do motivo) tanto quando origem e destino estão
        // em filesystems diferentes (EXDEV — comum quando targetFile fica
        // em armazenamento externo/adotável montado via FUSE) quanto, em
        // algumas implementações de JVM, quando o destino já existe. Files.move
        // com REPLACE_EXISTING cobre o caso de destino existente; a
        // tentativa ATOMIC_MOVE cobre o caminho comum (mesmo filesystem) sem
        // custo extra, e o fallback sem ATOMIC_MOVE deixa o NIO copiar +
        // apagar quando os arquivos estão em filesystems diferentes, algo
        // que rename(2)/renameTo nunca conseguem fazer.
        runCatching {
            java.nio.file.Files.move(
                partialFile.toPath(), targetFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE
            )
        }.recoverCatching {
            java.nio.file.Files.move(
                partialFile.toPath(), targetFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            )
        }.getOrElse {
            throw IllegalStateException("Não foi possível mover .part para o arquivo final: ${it.message}", it)
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

    private fun verifyArtifact(file: File, manifest: DownloadManifest): Boolean {
        if (!verifySha256(file, manifest.sha256)) return false
        val rootfs = manifest as? RootfsManifest ?: return true
        if (!rootfs.signatureRequired) return true
        return rootfsSignatureVerifier?.verify(file, rootfs) == true
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
