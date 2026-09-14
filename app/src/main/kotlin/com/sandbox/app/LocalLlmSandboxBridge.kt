package com.sandbox.app

import android.content.Context
import com.sandbox.resource.InferenceEngineManifest
import com.sandbox.resource.SandboxResourceManager
import com.sandbox.runtime.TarGzExtractor
import java.io.File
import java.nio.file.Files

/**
 * Ponte entre o modelo GGUF baixado pelo app e o sandbox proot.
 *
 * O modelo continua fora do RootFS e é exposto por hard-link/cópia apenas
 * dentro do guest. O motor llama.cpp é baixado separadamente, verificado por
 * SHA-256 e extraído dentro do RootFS para que a inferência aconteça de fato
 * no mesmo ambiente em que os comandos do Sandbox rodam.
 */
class LocalLlmSandboxBridge(private val context: Context) {
    private companion object {
        const val ENGINE_ASSET = "inference_engine_manifest.json"
        const val ENGINE_ARCHIVE = "llama-engine.tar.gz"
        const val ENGINE_DIR = "opt/llama"
        const val ENGINE_MARKER = ".llama-engine-b10901"
        const val ENGINE_PATH = "/usr/local/bin/llama-cli"
    }

    private val sandboxDir = File(context.filesDir, "sandbox")
    private val rootfsDir = File(sandboxDir, "rootfs")
    private val modelDir = File(sandboxDir, "models")
    private val engineArchive = File(sandboxDir, ENGINE_ARCHIVE)
    private val engineDir = File(rootfsDir, ENGINE_DIR)
    private val engineMarker = File(sandboxDir, ENGINE_MARKER)

    fun ensureEngine(): Result<String> = runCatching {
        require(rootfsDir.isDirectory) { "RootFS não está preparado." }
        val manifest = loadManifest()

        // Fast path: depois da primeira instalação o tarball é apagado, mas
        // o motor permanece no RootFS. Não baixamos os ~13,5 MB novamente.
        val existing = findLlamaCli()
        if (existing != null && engineMarker.readTextOrNull() == manifest.version) {
            exposeOnGuestPath(existing)
            return@runCatching guestPath(existing)
        }

        val archiveResult = SandboxResourceManager(engineArchive).ensureAvailable(manifest)
        val archive = when (archiveResult) {
            is SandboxResourceManager.DownloadResult.Success -> archiveResult.file
            is SandboxResourceManager.DownloadResult.Failure -> error(archiveResult.reason)
        }

        engineDir.deleteRecursively()
        engineDir.mkdirs()
        TarGzExtractor.extract(archive, engineDir)
        val executable = findLlamaCli()
            ?: error("llama-cli não foi encontrado no pacote ${manifest.version}.")
        executable.setExecutable(true, false)
        exposeOnGuestPath(executable)
        engineMarker.writeText(manifest.version)
        SandboxResourceManager(engineArchive).purge()
        guestPath(executable)
    }

    fun linkModel(modelId: String): String? {
        val source = File(modelDir, "$modelId.gguf")
        if (!source.isFile || !rootfsDir.isDirectory) return null
        val guestRelative = "home/sandbox/models/$modelId.gguf"
        val destination = File(rootfsDir, guestRelative)
        if (!destination.exists() || destination.length() != source.length()) {
            destination.parentFile?.mkdirs()
            destination.delete()
            val linked = runCatching {
                Files.createLink(destination.toPath(), source.toPath())
            }.isSuccess
            if (!linked) {
                runCatching { source.copyTo(destination, overwrite = true) }
                    .getOrElse { return null }
            }
        }
        return "/$guestRelative"
    }

    fun buildCommand(
        enginePathInGuest: String,
        modelPathInGuest: String,
        prompt: String,
        maxTokens: Int = 200
    ): List<String> {
        require(maxTokens in 1..1024)
        return listOf(
            enginePathInGuest,
            "-m", modelPathInGuest,
            "-p", prompt,
            "-n", maxTokens.toString(),
            "-c", "1024",
            "-t", "2",
            "--temp", "0.7"
        )
    }

    private fun exposeOnGuestPath(executable: File) {
        val destination = File(rootfsDir, ENGINE_PATH.removePrefix("/"))
        destination.parentFile?.mkdirs()
        if (destination.exists() || Files.isSymbolicLink(destination.toPath())) {
            destination.delete()
        }
        Files.createSymbolicLink(destination.toPath(), "/${executable.relativeTo(rootfsDir).invariantSeparatorsPath}")
    }

    private fun loadManifest(): InferenceEngineManifest {
        val json = context.assets.open(ENGINE_ASSET).bufferedReader().use { it.readText() }
        val obj = org.json.JSONObject(json)
        return InferenceEngineManifest(
            id = obj.getString("id"),
            version = obj.getString("version"),
            architecture = obj.getString("architecture"),
            url = obj.getString("url"),
            sizeBytes = obj.getLong("sizeBytes"),
            sha256 = obj.getString("sha256"),
            license = obj.getString("license")
        ).also {
            require(it.url.startsWith("https://")) { "URL do motor deve usar HTTPS" }
            require(it.architecture == "arm64-v8a") { "Motor incompatível com este sandbox: ${it.architecture}" }
            require(it.sha256.matches(Regex("[0-9a-fA-F]{64}"))) { "SHA-256 do motor inválido" }
        }
    }

    private fun findLlamaCli(): File? =
        if (!engineDir.isDirectory) null
        else engineDir.walkTopDown().firstOrNull { it.isFile && it.name == "llama-cli" }

    private fun guestPath(file: File): String =
        "/${file.relativeTo(rootfsDir).invariantSeparatorsPath}"
}

private fun File.readTextOrNull(): String? =
    runCatching { if (isFile) readText() else null }.getOrNull()
