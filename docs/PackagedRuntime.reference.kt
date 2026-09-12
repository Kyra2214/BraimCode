package your.app.package.runtime

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.util.concurrent.TimeUnit

/** Um bind mount a ser passado ao proot via `-b source[:target]`. */
data class ProotBindMount(val source: String, val target: String = source)

/**
 * Empacota e executa um userland Linux via proot, contornando restrições de
 * `noexec`/W^X ao disfarçar o binário do proot (e seu loader) como
 * bibliotecas nativas JNI — o único caminho que o Android garante ser
 * executável dentro da sandbox do app.
 *
 * Pré-requisitos (ver README.md deste pacote):
 *   1. proot renomeado para "libapp_proot.so"
 *   2. loader do proot renomeado para "libapp_proot_loader.so"
 *   3. Ambos colocados em src/main/jniLibs/<abi>/
 *   4. build.gradle com packagingOptions.jniLibs.useLegacyPackaging = true
 *   5. AndroidManifest com android:extractNativeLibs="true"
 */
class PackagedRuntime(
    context: Context,
    private val rootfsDir: File,
) {
    private val appContext = context.applicationContext

    // Onde o Android extraiu os .so (garantidamente executável pelo sistema).
    private val runnerLib = File(appContext.applicationInfo.nativeLibraryDir, RUNNER_ASSET_NAME)
    private val loaderLib = File(appContext.applicationInfo.nativeLibraryDir, LOADER_ASSET_NAME)

    // Diretório privado onde criamos o "atalho" com nome normal para exec.
    private val launchDir = File(appContext.noBackupFilesDir, "runner")
    private val launchRunner = File(launchDir, "proot")
    private val launchLoader = File(launchDir, "loader")

    private val tmpDir = File(appContext.cacheDir, "proot-tmp")

    /**
     * Verifica se os binários empacotados existem e são legíveis.
     *
     * IMPORTANTE: não confie em `canExecute()` aqui — em alguns fabricantes
     * (ex.: Honor/荣耀), arquivos extraídos em nativeLibraryDir sempre
     * reportam `canExecute() == false` mesmo sendo executáveis pelo sistema
     * operacional através do caminho de carregamento nativo.
     */
    fun runnerAvailable(): Boolean =
        runnerLib.isFile && runnerLib.canRead() &&
            loaderLib.isFile && loaderLib.canRead()

    /**
     * Prepara o diretório de lançamento: cria (ou reaproveita) os links
     * executáveis para o proot e o loader. Lança [RuntimeFailure] se a ABI
     * atual não tiver um runner empacotado, ou se nenhuma estratégia de
     * exec funcionar neste dispositivo.
     */
    @Synchronized
    fun prepare() {
        if (!runnerAvailable()) {
            throw RuntimeFailure(
                "RUNNER_UNAVAILABLE",
                "APK não contém um runner confiável para a ABI deste dispositivo",
            )
        }
        if (!launchDir.exists() && !launchDir.mkdirs()) {
            throw RuntimeFailure("RUNNER_PREPARE_FAILED", "Não foi possível criar o diretório do runner")
        }
        if (!tmpDir.exists()) tmpDir.mkdirs()

        Os.chmod(launchDir.absolutePath, 0x1c0) // 0700
        refreshExecutableLink(runnerLib, launchRunner)
        refreshExecutableLink(loaderLib, launchLoader)
    }

    /**
     * Monta o comando do proot e executa via ProcessBuilder.
     *
     * @param entrypoint comando a rodar dentro do rootfs, ex: ["/bin/bash", "--login"]
     * @param bindMounts caminhos do host a montar dentro do rootfs (ex: /dev, /proc)
     * @param env variáveis de ambiente do processo convidado (dentro do rootfs)
     * @param disableSeccomp usado internamente para o fallback de compatibilidade
     */
    fun launch(
        entrypoint: List<String>,
        bindMounts: List<ProotBindMount> = emptyList(),
        env: Map<String, String> = emptyMap(),
        disableSeccomp: Boolean = false,
    ): Process {
        val argv = buildArgv(entrypoint, bindMounts, env)
        val hostEnv = hostEnvironment(disableSeccomp)
        return ProcessBuilder(argv)
            .directory(rootfsDir)
            .redirectErrorStream(true)
            .apply {
                environment().clear()
                environment().putAll(hostEnv)
            }
            .start()
    }

    /**
     * Roda um probe curto do rootfs (ex: `exit 0`) e, se falhar por
     * incompatibilidade de seccomp, tenta de novo automaticamente com
     * PROOT_NO_SECCOMP=1. Retorna o profile que funcionou (para você
     * cachear e reutilizar em `launch()`), ou lança RuntimeFailure.
     */
    fun detectSeccompFallback(timeoutSeconds: Long = 10L): Boolean {
        val probe = listOf("/bin/sh", "-c", "exit 0")

        val direct = runProbe(probe, disableSeccomp = false, timeoutSeconds)
        if (direct.exitCode == 0) return false

        val output = direct.output.lowercase()
        val seccompIncompatible =
            "seccomp" in output && ("not supported" in output || "operation not permitted" in output)
        if (!seccompIncompatible) {
            throw classifyFailure(direct.output)
        }

        val fallback = runProbe(probe, disableSeccomp = true, timeoutSeconds)
        if (fallback.exitCode == 0) return true

        throw classifyFailure(fallback.output)
    }

    // -------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------

    private fun buildArgv(
        entrypoint: List<String>,
        bindMounts: List<ProotBindMount>,
        env: Map<String, String>,
    ): List<String> = buildList {
        add(launchRunner.absolutePath)
        add("-r")
        add(rootfsDir.absolutePath)
        add("-0") // roda como root dentro do guest
        add("-w")
        add("/root")
        bindMounts.forEach { mount ->
            add("-b")
            add(if (mount.source == mount.target) mount.source else "${mount.source}:${mount.target}")
        }
        add("/usr/bin/env")
        add("-i")
        env.forEach { (key, value) -> add("$key=$value") }
        addAll(entrypoint)
    }

    private fun hostEnvironment(disableSeccomp: Boolean): Map<String, String> = buildMap {
        put("PROOT_LOADER", launchLoader.absolutePath)
        put("PROOT_TMP_DIR", tmpDir.absolutePath)
        put("TMPDIR", tmpDir.absolutePath)
        put("LD_LIBRARY_PATH", "")
        if (disableSeccomp) put("PROOT_NO_SECCOMP", "1")
    }

    private data class ProbeResult(val exitCode: Int?, val output: String)

    private fun runProbe(entrypoint: List<String>, disableSeccomp: Boolean, timeoutSeconds: Long): ProbeResult {
        val process = launch(entrypoint, disableSeccomp = disableSeccomp)
        val output = process.inputStream.bufferedReader().readText()
        val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if (!completed) process.destroyForcibly()
        return ProbeResult(if (process.isAlive) null else process.exitValue(), output)
    }

    private fun classifyFailure(output: String): RuntimeFailure {
        val normalized = output.lowercase()
        return when {
            "ptrace" in normalized && ("operation not permitted" in normalized || "permission denied" in normalized) ->
                RuntimeFailure("PROOT_PTRACE_DENIED", "O kernel deste dispositivo nega o ptrace exigido pelo proot")
            "seccomp" in normalized ->
                RuntimeFailure("PROOT_SECCOMP_UNAVAILABLE", "A política de seccomp do kernel é incompatível com o proot")
            "proot error" in normalized || "execve(" in normalized || "loader" in normalized ->
                RuntimeFailure("PROOT_GUEST_EXEC_FAILED", "O proot não conseguiu carregar o programa dentro do rootfs")
            else ->
                RuntimeFailure("PROOT_GUEST_START_FAILED", "O proot não conseguiu iniciar o userland")
        }
    }

    /**
     * Garante um caminho de arquivo com nome normal (não ".so") e permissão
     * de execução, apontando para o binário empacotado como lib nativa.
     *
     * Estratégia primária: symlink (barato, atômico via rename).
     * Fallback (ROMs que bloqueiam symlink via SELinux — ex: Honor/荣耀):
     *   copia o arquivo, marca chmod +x, e tenta selar com o xattr
     *   security.android.exec exigido pelo Android 15+.
     */
    private fun refreshExecutableLink(target: File, link: File) {
        if (isPrepared(target, link)) return

        val pending = File(launchDir, ".${link.name}.new")
        if (pending.exists() && !pending.delete()) {
            throw RuntimeFailure("RUNNER_PREPARE_FAILED", "Não foi possível limpar link temporário do runner")
        }

        try {
            try {
                Os.symlink(target.absolutePath, pending.absolutePath)
            } catch (error: ErrnoException) {
                val blockedBySelinuxOrFs = error.errno == OsConstants.EACCES ||
                    error.errno == OsConstants.EPERM ||
                    error.errno == OsConstants.ENOTSUP ||
                    error.errno == OsConstants.EXDEV
                if (!blockedBySelinuxOrFs) throw error
                copyWithExecFallback(target, pending)
            }

            if (!isPrepared(target, pending)) {
                throw RuntimeFailure("RUNNER_PREPARE_FAILED", "Link temporário do runner não ficou executável")
            }
            // rename é atômico: processos já em execução nunca veem o link ausente.
            Os.rename(pending.absolutePath, link.absolutePath)
        } finally {
            if (pending.exists()) pending.delete()
        }

        if (!isPrepared(target, link)) {
            throw RuntimeFailure("RUNNER_PREPARE_FAILED", "Link privado do runner não ficou executável")
        }
    }

    private fun copyWithExecFallback(target: File, destination: File) {
        target.copyTo(destination, overwrite = false)
        destination.setExecutable(true, false)
        try {
            Os.setxattr(destination.absolutePath, EXEC_XATTR_NAME, EXEC_XATTR_VALUE, 0)
        } catch (_: Throwable) {
            // Kernel/ROM antigo sem suporte a esse xattr: a política do
            // sistema decide se o arquivo pode rodar mesmo assim.
        }
    }

    private fun isSymlinkTo(target: File, link: File): Boolean = try {
        val stat = Os.lstat(link.absolutePath)
        OsConstants.S_ISLNK(stat.st_mode) && Os.readlink(link.absolutePath) == target.absolutePath &&
            link.isFile && link.canRead() && link.canExecute()
    } catch (error: ErrnoException) {
        if (error.errno == OsConstants.ENOENT) false else throw error
    }

    /** Aceita tanto a forma symlink quanto a forma copiada (fallback). */
    private fun isPrepared(target: File, path: File): Boolean {
        if (isSymlinkTo(target, path)) return true
        return try {
            val stat = Os.lstat(path.absolutePath)
            OsConstants.S_ISREG(stat.st_mode) && stat.st_size > 0
        } catch (error: ErrnoException) {
            if (error.errno == OsConstants.ENOENT) false else throw error
        }
    }

    companion object {
        // Nomes dos .so como empacotados em jniLibs/<abi>/.
        private const val RUNNER_ASSET_NAME = "libapp_proot.so"
        private const val LOADER_ASSET_NAME = "libapp_proot_loader.so"

        // Selo exigido pelo Android 15+ para permitir execução de arquivos
        // fora dos caminhos "confiáveis" (mesmo selo usado em binários do
        // próprio rootfs, quando aplicável).
        private const val EXEC_XATTR_NAME = "user.security.android.exec"
        private val EXEC_XATTR_VALUE = ByteArray(0)
    }
}
