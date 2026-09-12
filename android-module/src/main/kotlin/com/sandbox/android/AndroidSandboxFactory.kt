package com.sandbox.android

import android.content.Context
import android.os.Build
import com.sandbox.agent.Sandbox
import com.sandbox.resource.SandboxResourceManager
import com.sandbox.runtime.FileExecutionLogRepository
import com.sandbox.runtime.FileRuntimeEventStore
import com.sandbox.runtime.ManagedSandboxRuntime
import com.sandbox.runtime.ProotProcessLauncher
import com.sandbox.runtime.SandboxRuntime
import com.sandbox.runtime.PackagedRuntime
import com.sandbox.runtime.TarGzExtractor
import java.io.File
import java.util.zip.ZipFile

class AndroidSandboxFactory(private val context: Context) {
    private companion object {
        private const val EXTRACTOR_VERSION = "3"
        private const val SESSION_PREFS = "sandbox_runtime"
        private const val SESSION_ID = "session_id"
    }

    private val sandboxBaseDir = File(context.filesDir, "sandbox")
    private val downloadedArchive = File(sandboxBaseDir, "rootfs.tar.gz")
    private val extractedRootfsDir = File(sandboxBaseDir, "rootfs")
    private val extractionMarker = File(sandboxBaseDir, ".extractor-version")
    private val prootTmpDir = File(context.cacheDir, "sandbox-tmp")

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
        return SandboxResourceManager(downloadedArchive)
    }

    fun prepareRuntime(forceReExtract: Boolean = false): SandboxRuntime {
        require(downloadedArchive.exists()) { "Rootfs ainda não foi baixado. Chame resourceManager().ensureAvailable() primeiro." }
        if (forceReExtract && extractedRootfsDir.exists()) extractedRootfsDir.deleteRecursively()
        val needsReExtract = !extractedRootfsDir.exists() || extractedRootfsDir.list().isNullOrEmpty() || extractionMarker.readTextOrNull() != EXTRACTOR_VERSION
        if (needsReExtract) {
            if (extractedRootfsDir.exists()) extractedRootfsDir.deleteRecursively()
            TarGzExtractor.extract(downloadedArchive, extractedRootfsDir)
            validateExtractedRootfs()
            extractionMarker.writeText(EXTRACTOR_VERSION)
        }
        ensureResolvConf()
        val packagedRuntime = PackagedRuntime(context, extractedRootfsDir)
        packagedRuntime.prepare()
        val runtime = SandboxRuntime(
            prootExecutable = packagedRuntime.preparedRunnerPath,
            rootfsDir = extractedRootfsDir,
            tmpDir = packagedRuntime.prootTmpPath,
            nativeLibraryDir = packagedRuntime.nativeLibraryPath,
            prootLoader = packagedRuntime.packagedLoaderPath
        )
        ensureVenv(runtime)
        return runtime
    }

    fun prepareManagedRuntime(sessionId: String = persistentSessionId()): ManagedSandboxRuntime {
        require(downloadedArchive.exists()) { "Rootfs ainda não foi baixado. Chame resourceManager().ensureAvailable() primeiro." }
        prepareRuntime()
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
            prootLoader = packagedRuntime.packagedLoaderPath
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
            current = (if (link.isAbsolute) link else current.parent.resolve(link)).normalize()
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
        resourceManager().purge()
        if (extractedRootfsDir.exists()) extractedRootfsDir.deleteRecursively()
        if (extractionMarker.exists()) extractionMarker.delete()
        if (prootTmpDir.exists()) prootTmpDir.deleteRecursively()
    }
}
