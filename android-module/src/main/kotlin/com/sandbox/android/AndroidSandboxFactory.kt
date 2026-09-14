package com.sandbox.android

import android.content.Context
import android.os.Build
import com.sandbox.agent.Sandbox
import com.sandbox.resource.SandboxResourceManager
import com.sandbox.resource.Ed25519RootfsSignatureVerifier
import com.sandbox.runtime.FileExecutionLogRepository
import com.sandbox.runtime.FileRuntimeEventStore
import com.sandbox.runtime.ManagedSandboxRuntime
import com.sandbox.runtime.ProotProcessLauncher
import com.sandbox.runtime.ProotResourceLimits
import com.sandbox.runtime.SandboxRuntime
import com.sandbox.runtime.PackagedRuntime
import com.sandbox.runtime.TarGzExtractor
import java.io.File
import java.util.zip.ZipFile

class AndroidSandboxFactory(private val context: Context) {
    private companion object {
        // 5: composição base + agent-extra + agent-android. O valor 4
        // identificava a extração de uma única camada e não pode ser
        // reutilizado após a migração para os três artefatos.
        private const val EXTRACTOR_VERSION = "5"
        private const val SESSION_PREFS = "sandbox_runtime"
        private const val SESSION_ID = "session_id"
    }

    private val sandboxBaseDir = File(context.filesDir, "sandbox")
    private val downloadedArchives = listOf(
        File(sandboxBaseDir, "rootfs-base.tar.gz"),
        File(sandboxBaseDir, "rootfs-extra.tar.gz"),
        File(sandboxBaseDir, "rootfs-android.tar.gz")
    )
    private val modelDir = File(sandboxBaseDir, "models")
    private val extractedRootfsDir = File(sandboxBaseDir, "rootfs")
    private val extractionMarker = File(sandboxBaseDir, ".extractor-version")
    private val prootTmpDir = File(context.cacheDir, "sandbox-tmp")

    private val rootfsVerifier by lazy {
        val trustedKeys = runCatching {
            context.assets.open("rootfs_trusted_keys.json").bufferedReader().use { reader ->
                val json = org.json.JSONObject(reader.readText())
                val keys = json.optJSONObject("keys") ?: org.json.JSONObject()
                keys.keys().asSequence().associateWith { java.util.Base64.getDecoder().decode(keys.getString(it)) }
            }
        }.getOrDefault(emptyMap())
        Ed25519RootfsSignatureVerifier(trustedKeys)
    }

    private fun ensureProotExecutable(): File {
        val extracted = File(context.applicationInfo.nativeLibraryDir, "libproot.so")
        if (extracted.exists()) {
            extracted.setExecutable(true, false)
            return extracted
        }
        val destination = File(sandboxBaseDir, "bin/libproot.so")
        if (!destination.exists() || destination.length() == 0L) {
            destination.parentFile?.mkdirs()
            val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
            ZipFile(context.applicationInfo.sourceDir).use { apk ->
                listOf("libproot.so", "libtalloc.so", "libandroid-shmem.so").forEach { library ->
                    val entryName = "lib/$abi/$library"
                    val entry = apk.getEntry(entryName) ?: error("biblioteca nativa não encontrada no APK em $entryName")
                    val outputFile = File(destination.parentFile, library)
                    apk.getInputStream(entry).use { input -> outputFile.outputStream().use { output -> input.copyTo(output) } }
                }
            }
            destination.setExecutable(true, false)
        }
        return destination
    }

    fun resourceManager(): SandboxResourceManager {
        sandboxBaseDir.mkdirs()
        return SandboxResourceManager(downloadedArchives.first(), rootfsVerifier)
    }

    fun layerResourceManager(layer: Int): SandboxResourceManager {
        require(layer in downloadedArchives.indices) { "Camada RootFS inválida: $layer" }
        sandboxBaseDir.mkdirs()
        return SandboxResourceManager(downloadedArchives[layer], rootfsVerifier)
    }

    /** Gerenciador de artefatos de modelo; mantém a mini-LLM fora do RootFS. */
    fun modelResourceManager(modelId: String): SandboxResourceManager {
        require(modelId.matches(Regex("[a-z0-9][a-z0-9._-]*"))) { "ID de modelo inválido" }
        modelDir.mkdirs()
        return SandboxResourceManager(File(modelDir, "$modelId.gguf"))
    }

    fun modelFile(modelId: String): File = File(modelDir, "$modelId.gguf")

    /**
     * O .gguf baixado fica em [modelDir], FORA do rootfs (de propósito —
     * ver o comentário da classe). Só que um processo rodando via proot
     * (o chatbox de teste da mini-LLM, por exemplo) não enxerga caminhos
     * fora do rootfs sem um bind explícito. Em vez de mexer no launcher
     * do proot pra adicionar mais um bind, este método faz um hard link
     * do arquivo pra dentro do rootfs (mesmo filesystem — não duplica os
     * ~100 MiB em disco) em /home/sandbox/models/<modelId>.gguf, caminho
     * já resolvível pelo comando guest. Cai pra cópia se o SO recusar o
     * link (ex.: partições diferentes).
     *
     * Retorna o caminho absoluto GUEST (dentro do rootfs) do modelo, ou
     * null se o .gguf ainda não foi baixado ou se o rootfs ainda não foi
     * extraído.
     */
    fun ensureLocalModelLinkedIntoRootfs(modelId: String): String? {
        val source = modelFile(modelId)
        if (!source.isFile || !extractedRootfsDir.isDirectory) return null
        val guestRelativePath = "home/sandbox/models/$modelId.gguf"
        val destination = File(extractedRootfsDir, guestRelativePath)
        if (!destination.exists() || destination.length() != source.length()) {
            destination.parentFile?.mkdirs()
            destination.delete()
            val linked = runCatching {
                java.nio.file.Files.createLink(destination.toPath(), source.toPath())
            }.isSuccess
            if (!linked) {
                val copied = runCatching { source.copyTo(destination, overwrite = true) }.isSuccess
                if (!copied) return null
            }
        }
        return "/$guestRelativePath"
    }

    /**
     * Monta o comando que o chatbox de teste manda pro sandbox. Isto é
     * deliberadamente um CHATBOX DE TESTE, não uma UI final: o app ainda
     * não empacota um motor de inferência (llama.cpp ou similar) dentro
     * do rootfs — ver docs/LOCAL_MODEL.md. Por isso o script procura por
     * um binário conhecido em tempo de execução e, se não achar, devolve
     * um erro claro em vez de fingir sucesso — isso já é suficiente pra
     * validar visualmente o caminho completo (UI → runtime → rootfs)
     * mesmo antes do motor de inferência existir.
     */
    fun buildLocalModelChatCommand(modelPathInGuest: String, prompt: String, maxTokens: Int = 200): List<String> {
        val script = """
            BIN=${'$'}(command -v llama-cli 2>/dev/null || command -v llama-server 2>/dev/null || command -v llama 2>/dev/null || command -v main 2>/dev/null)
            if [ -z "${'$'}BIN" ]; then
              echo "LLAMA_CPP_NAO_ENCONTRADO: nenhum binario de inferencia (llama-cli/llama-server/llama/main) foi encontrado no rootfs. Falta empacotar o motor de inferencia (veja docs/LOCAL_MODEL.md) — este chatbox serve pra testar o fluxo ate aqui." >&2
              exit 127
            fi
            exec "${'$'}BIN" -m "$modelPathInGuest" -p "${'$'}1" -n $maxTokens --temp 0.7
        """.trimIndent()
        return listOf("/bin/bash", "-c", script, "chat", prompt)
    }

    /**
     * True quando o rootfs já foi extraído com sucesso e está íntegro
     * (marcador de versão bate e as entradas essenciais existem). Quando
     * isto é true, os arquivos baixados (rootfs-*.tar.gz) não são mais
     * necessários — só servem de "instalador", e podem já ter sido
     * removidos por [prepareRuntime] para liberar espaço.
     */
    fun isRootfsReady(): Boolean = rootfsExtractionValid()

    private fun rootfsExtractionValid(): Boolean =
        extractedRootfsDir.exists() &&
            !extractedRootfsDir.list().isNullOrEmpty() &&
            extractionMarker.readTextOrNull() == EXTRACTOR_VERSION &&
            hasRequiredRootfsEntries()

    /**
     * Apaga os arquivos .tar.gz baixados (e eventuais .part remanescentes).
     * Só deve ser chamado depois que a extração foi validada com sucesso:
     * assim como um instalador, uma vez que o conteúdo já foi "instalado"
     * (extraído e verificado) em [extractedRootfsDir], os pacotes de
     * origem só ocupam espaço à toa.
     */
    private fun deleteDownloadedArchives() {
        downloadedArchives.forEach { archive ->
            SandboxResourceManager(archive).purge()
        }
    }

    fun prepareRuntime(
        forceReExtract: Boolean = false,
        progressListener: ((completed: Long, total: Long, stage: String) -> Unit)? = null
    ): SandboxRuntime {
        if (forceReExtract && extractedRootfsDir.exists()) extractedRootfsDir.deleteRecursively()
        val needsReExtract = forceReExtract || !rootfsExtractionValid()
        if (needsReExtract) {
            // As camadas só precisam existir em disco quando é preciso
            // (re)extrair. Se o rootfs já está extraído e válido, os
            // arquivos baixados podem já ter sido apagados por uma
            // preparação anterior — nesse caso nem chegamos aqui.
            require(downloadedArchives.all { it.exists() }) {
                "As três camadas RootFS ainda não foram baixadas. Prepare o sandbox novamente."
            }
            if (extractedRootfsDir.exists()) extractedRootfsDir.deleteRecursively()
            extractionMarker.delete()
            val totalArchiveBytes = downloadedArchives.sumOf { it.length() }.coerceAtLeast(1L)
            var completedArchiveBytes = 0L
            downloadedArchives.forEachIndexed { index, archive ->
                progressListener?.invoke(completedArchiveBytes, totalArchiveBytes, "Extraindo camada ${index + 1}/3")
                TarGzExtractor.extract(archive, extractedRootfsDir) { bytesRead ->
                    progressListener?.invoke(
                        (completedArchiveBytes + bytesRead).coerceAtMost(totalArchiveBytes),
                        totalArchiveBytes,
                        "Extraindo camada ${index + 1}/3"
                    )
                }
                completedArchiveBytes += archive.length()
            }
            validateExtractedRootfs()
            extractionMarker.writeText(EXTRACTOR_VERSION)
            // "Instalação" concluída e validada: os .tar.gz baixados não
            // servem mais pra nada (só pra re-extrair do zero, e pra isso
            // dá pra baixar de novo) — apaga pra liberar espaço no device.
            deleteDownloadedArchives()
        }
        progressListener?.invoke(1L, 1L, "Inicializando runtime")
        ensureResolvConf()
        val packagedRuntime = PackagedRuntime(context, extractedRootfsDir)
        packagedRuntime.prepare()
        val runtime = SandboxRuntime(
            prootExecutable = packagedRuntime.preparedRunnerPath,
            rootfsDir = extractedRootfsDir,
            tmpDir = packagedRuntime.prootTmpPath,
            nativeLibraryDir = packagedRuntime.nativeLibraryPath,
            prootLoader = packagedRuntime.packagedLoaderPath,
            // Explícito (não só o default) para deixar claro na composição
            // real de produção que todo comando via proot carrega RLIMIT_*
            // real — ver ProotResourceLimits.kt e AUDITORIA_PESADA.md item 4.
            resourceLimits = ProotResourceLimits.DEFAULT
        )
        ensureVenv(runtime)
        return runtime
    }

    fun prepareManagedRuntime(
        sessionId: String = persistentSessionId(),
        progressListener: ((completed: Long, total: Long, stage: String) -> Unit)? = null
    ): ManagedSandboxRuntime {
        // A checagem de que as 3 camadas existem só faz sentido quando uma
        // (re)extração é realmente necessária — e quem decide isso, de
        // forma consistente com a limpeza pós-instalação, é prepareRuntime.
        prepareRuntime(progressListener = progressListener)
        val packagedRuntime = PackagedRuntime(context, extractedRootfsDir)
        packagedRuntime.prepare()
        val logDir = File(sandboxBaseDir, "execution-logs")
        val eventDir = File(sandboxBaseDir, "runtime-events")
        val repository = FileExecutionLogRepository(logDir)
        val eventStore = FileRuntimeEventStore(eventDir)
        val launcher = ProotProcessLauncher(
            prootExecutable = packagedRuntime.preparedRunnerPath,
            rootfsDir = extractedRootfsDir,
            tmpDir = packagedRuntime.prootTmpPath,
            nativeLibraryDir = packagedRuntime.nativeLibraryPath,
            prootLoader = packagedRuntime.packagedLoaderPath,
            resourceLimits = ProotResourceLimits.DEFAULT
        )
        return ManagedSandboxRuntime(launcher, repository, sessionId, runtimeEventRepository = eventStore)
    }

    /**
     * Etapa 3: monta o [Sandbox] real de device, já sabendo onde o rootfs
     * foi extraído. Chame [prepareManagedRuntime] antes (ou deixe este
     * método chamar via [prepareManagedRuntime] internamente) — aqui ele
     * é chamado sempre, então basta o rootfs já ter sido baixado.
     */
    fun createSandbox(sessionId: String = persistentSessionId()): Sandbox =
        Sandbox(runtime = prepareManagedRuntime(sessionId), rootfsDir = extractedRootfsDir)

    fun persistentSessionId(): String {
        val prefs = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(SESSION_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val created = FileExecutionLogRepository.newId()
        prefs.edit().putString(SESSION_ID, created).apply()
        return created
    }

    fun clearPersistentSession() {
        context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE).edit().remove(SESSION_ID).apply()
    }

    fun inspectExtractedRootfs(): String {
        if (!extractedRootfsDir.exists()) return "Rootfs ainda não foi extraído (pasta ${extractedRootfsDir.path} não existe)."
        val report = StringBuilder()
        report.appendLine("Pasta: ${extractedRootfsDir.path}")
        report.appendLine("Marcador de versão: ${extractionMarker.readTextOrNull() ?: "(ausente)"}")
        val topLevel = extractedRootfsDir.listFiles()?.sortedBy { it.name } ?: emptyList()
        report.appendLine("Entradas na raiz (${topLevel.size}): ${topLevel.joinToString(", ") { it.name }}")
        var totalFiles = 0
        var totalDirs = 0
        var totalSymlinks = 0
        var totalBytes = 0L
        var walkErrors = 0
        fun walk(dir: File) {
            val children = runCatching { dir.listFiles() }.getOrNull() ?: return
            for (child in children) {
                try {
                    when {
                        java.nio.file.Files.isSymbolicLink(child.toPath()) -> totalSymlinks++
                        child.isDirectory -> { totalDirs++; walk(child) }
                        child.isFile -> { totalFiles++; totalBytes += child.length() }
                    }
                } catch (_: Exception) { walkErrors++ }
            }
        }
        walk(extractedRootfsDir)
        report.appendLine("Total: $totalFiles arquivos, $totalDirs pastas, $totalSymlinks symlinks, ${totalBytes / (1024 * 1024)} MB" + if (walkErrors > 0) " ($walkErrors entradas com erro ao inspecionar)" else "")
        report.appendLine()
        report.appendLine("Caminhos essenciais:")
        listOf("bin", "usr/bin/bash", "usr/bin/dash", "bin/bash", "home", "home/sandbox", "etc/resolv.conf").forEach { relativePath ->
            val target = File(extractedRootfsDir, relativePath)
            val exists = java.nio.file.Files.exists(target.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)
            val kind = when {
                !exists -> "AUSENTE"
                java.nio.file.Files.isSymbolicLink(target.toPath()) -> "symlink -> ${runCatching { java.nio.file.Files.readSymbolicLink(target.toPath()) }.getOrNull()}"
                target.isDirectory -> "pasta"
                else -> "arquivo (${target.length()} bytes)"
            }
            report.appendLine("  $relativePath: $kind")
        }
        report.appendLine()
        report.appendLine("Cadeia de interpretador:")
        listOf("bin/bash", "lib/ld-linux-aarch64.so.1").forEach { relativePath ->
            report.appendLine("  $relativePath: ${describeInterpreterChain(File(extractedRootfsDir, relativePath))}")
        }
        report.appendLine()
        report.appendLine("Teste de execução direta (sem proot):")
        report.appendLine("  ${directExecProbe()}")
        return report.toString()
    }

    private fun directExecProbe(): String {
        val bash = File(extractedRootfsDir, "usr/bin/bash")
        if (!bash.isFile) return "usr/bin/bash não é um arquivo regular, não dá pra testar."
        return try {
            val process = ProcessBuilder(bash.absolutePath, "--version").redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) { process.destroyForcibly(); "travou (timeout de 5s)" }
            else "sucesso, exit code ${process.exitValue()}: ${output.take(120)}"
        } catch (e: Exception) {
            val message = e.message.orEmpty()
            val interpretation = when {
                message.contains("error=13") || message.contains("Permission denied") -> " → EACCES: forte indício de restrição de exec do Android."
                message.contains("error=2") || message.contains("No such file") -> " → ENOENT esperado fora do proot; o kernel procura o loader na raiz real do Android."
                else -> " → erro sem padrão reconhecido."
            }
            "FALHOU: ${e.javaClass.simpleName}: $message$interpretation"
        }
    }

    private fun validateExtractedRootfs() {
        val problems = mutableListOf<String>()
        listOf("bin/bash", "lib/ld-linux-aarch64.so.1").forEach { relativePath ->
            val description = describeInterpreterChain(File(extractedRootfsDir, relativePath))
            if (!description.startsWith("OK")) problems += "${relativeLabel(File(extractedRootfsDir, relativePath))}: $description"
        }
        check(problems.isEmpty()) {
            "Rootfs extraído está incompleto ou corrompido:\n" + problems.joinToString("\n") { "  - $it" } +
                "\nToque em Resetar sandbox e baixe o rootfs novamente."
        }
    }

    private fun hasRequiredRootfsEntries(): Boolean =
        File(extractedRootfsDir, "home/sandbox").isDirectory &&
            describeInterpreterChain(File(extractedRootfsDir, "bin/bash")).startsWith("OK") &&
            describeInterpreterChain(File(extractedRootfsDir, "lib/ld-linux-aarch64.so.1")).startsWith("OK")

    private fun describeInterpreterChain(start: File, maxHops: Int = 10): String {
        var current = start.toPath().toAbsolutePath().normalize()
        val chain = mutableListOf(current)
        repeat(maxHops) {
            if (!java.nio.file.Files.exists(current, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return "QUEBRADO em ${relativeLabel(current.toFile())} (não existe) — cadeia: " + chain.joinToString(" -> ") { relativeLabel(it.toFile()) }
            if (!java.nio.file.Files.isSymbolicLink(current)) {
                return if (current.toFile().isFile) "OK -> ${relativeLabel(current.toFile())} (${current.toFile().length()} bytes)" + if (chain.size > 1) " — cadeia: ${chain.joinToString(" -> ") { relativeLabel(it.toFile()) }}" else ""
                else "QUEBRADO: alvo final ${relativeLabel(current.toFile())} não é um arquivo regular"
            }
            val link = java.nio.file.Files.readSymbolicLink(current)
            // Caminhos absolutos no RootFS são absolutos para o guest, não
            // para o Android host. Sem este prefixo, /usr/bin/... é resolvido
            // fora da árvore extraída e uma cadeia válida aparece quebrada.
            current = (if (link.isAbsolute) {
                extractedRootfsDir.toPath().resolve(link.toString().removePrefix("/"))
            } else {
                current.parent.resolve(link)
            }).normalize()
            chain.add(current)
        }
        return "QUEBRADO: cadeia de symlink excede $maxHops saltos (possível loop) — " + chain.joinToString(" -> ") { relativeLabel(it.toFile()) }
    }

    private fun relativeLabel(file: File): String = runCatching { file.relativeTo(extractedRootfsDir).path }.getOrDefault(file.path)
    private fun File.readTextOrNull(): String? = if (isFile) runCatching { readText() }.getOrNull() else null

    private fun ensureResolvConf() {
        val resolvConf = File(extractedRootfsDir, "etc/resolv.conf")
        resolvConf.parentFile?.mkdirs()
        resolvConf.writeText("nameserver 8.8.8.8\nnameserver 1.1.1.1\n")
    }

    private fun ensureVenv(runtime: SandboxRuntime) {
        val venvMarker = File(extractedRootfsDir, "home/sandbox/venv/bin/python3")
        if (venvMarker.exists()) return
        runtime.execute(
            command = listOf("/bin/bash", "-c", "python3 -m venv /home/sandbox/venv --system-site-packages 2>&1 || echo 'venv indisponível, seguindo sem ela'"),
            timeoutSeconds = 120
        )
    }

    fun purgeAll() {
        downloadedArchives.forEach { archive -> SandboxResourceManager(archive).purge() }
        if (extractedRootfsDir.exists()) extractedRootfsDir.deleteRecursively()
        if (extractionMarker.exists()) extractionMarker.delete()
        if (prootTmpDir.exists()) prootTmpDir.deleteRecursively()
    }
}
