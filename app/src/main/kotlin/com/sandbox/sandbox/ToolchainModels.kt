package com.sandbox.sandbox

import java.io.File

/** Toolchains suportadas pelo catálogo declarativo do Sandbox. */
enum class ToolchainKind { ANDROID, JAVA, PYTHON, NODE, CPP, RUST, GO }

data class ToolchainProfile(
    val id: String,
    val kind: ToolchainKind,
    val displayName: String,
    val executable: String,
    val versionArguments: List<String>,
    val packages: List<String>,
    val validationArguments: List<String> = versionArguments
) {
    init {
        require(id.matches(Regex("[a-z0-9][a-z0-9._-]*"))) { "ID de toolchain inválido" }
        require(displayName.isNotBlank())
        require(executable.matches(Regex("[a-zA-Z0-9._+-]+"))) { "Executável de toolchain inválido" }
        require(versionArguments.isNotEmpty() && validationArguments.isNotEmpty())
        require(packages.isNotEmpty() && packages.all { it.matches(Regex("[A-Za-z0-9][A-Za-z0-9+._:-]*")) }) {
            "Pacotes de toolchain inválidos"
        }
    }
}

data class ToolchainDetection(
    val profile: ToolchainProfile,
    val installed: Boolean,
    val versionOutput: String = "",
    val diagnostic: String? = null
)

data class ToolchainInstallPlan(
    val profile: ToolchainProfile,
    val command: List<String>,
    val timeoutSeconds: Long = 900
) {
    init {
        require(command == listOf("bash", "-c", command.getOrNull(2).orEmpty()))
        require(timeoutSeconds in 60..3600)
    }
}

object BuiltInToolchains {
    val all: List<ToolchainProfile> = listOf(
        ToolchainProfile("java", ToolchainKind.JAVA, "Java", "java", listOf("--version"), listOf("default-jdk")),
        ToolchainProfile("python", ToolchainKind.PYTHON, "Python", "python3", listOf("--version"), listOf("python3", "python3-pip")),
        ToolchainProfile("node", ToolchainKind.NODE, "Node.js", "node", listOf("--version"), listOf("nodejs", "npm")),
        ToolchainProfile("cpp", ToolchainKind.CPP, "C/C++", "g++", listOf("--version"), listOf("g++", "make")),
        ToolchainProfile("rust", ToolchainKind.RUST, "Rust", "rustc", listOf("--version"), listOf("rustc", "cargo")),
        ToolchainProfile("go", ToolchainKind.GO, "Go", "go", listOf("version"), listOf("golang-go"))
    )
}

/** Detecta somente comandos declarados pelo perfil; nunca aceita entrada do usuário como shell. */
class ToolchainDetector(private val executor: SandboxCommandExecutor) {
    fun detect(profile: ToolchainProfile): ToolchainDetection {
        val result = executor.execute(listOf(profile.executable) + profile.versionArguments, timeoutSeconds = 30)
        return ToolchainDetection(
            profile = profile,
            installed = result.succeeded,
            versionOutput = (result.stdout.ifBlank { result.stderr }).take(4096),
            diagnostic = result.stderr.takeIf { !result.succeeded }?.take(4096)
        )
    }

    fun planInstall(profile: ToolchainProfile): ToolchainInstallPlan {
        val packages = profile.packages.joinToString(" ")
        val script = "set -o pipefail; export DEBIAN_FRONTEND=noninteractive; " +
            "apt-get update -qq && apt-get -o Dpkg::Use-Pty=0 install -y --no-install-recommends $packages"
        return ToolchainInstallPlan(profile, listOf("bash", "-c", script))
    }
}

enum class ToolchainState { NOT_INSTALLED, INSTALLING, INSTALLED, FAILED, REMOVING }

data class ToolchainStatus(
    val profileId: String,
    val state: ToolchainState,
    val versionOutput: String = "",
    val error: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

/** Lifecycle explícito: instala, valida, persiste estado e remove somente pacotes do perfil. */
class ToolchainManager(
    private val executor: SandboxCommandExecutor,
    private val stateDir: File,
    profiles: List<ToolchainProfile> = BuiltInToolchains.all
) {
    private val profilesById = profiles.associateBy { it.id }
    private val detector = ToolchainDetector(executor)
    private val lock = Any()

    init { stateDir.mkdirs() }

    fun status(id: String): ToolchainStatus = synchronized(lock) {
        val profile = profile(id)
        val file = stateFile(profile)
        if (!file.isFile) return@synchronized ToolchainStatus(id, ToolchainState.NOT_INSTALLED)
        val parts = file.readLines()
        ToolchainStatus(id, runCatching { ToolchainState.valueOf(parts.firstOrNull().orEmpty()) }.getOrDefault(ToolchainState.FAILED), parts.getOrNull(1).orEmpty(), parts.getOrNull(2))
    }

    fun install(id: String): ToolchainStatus = synchronized(lock) {
        val profile = profile(id)
        val before = detector.detect(profile)
        if (before.installed) return@synchronized persist(ToolchainStatus(id, ToolchainState.INSTALLED, before.versionOutput))
        persist(ToolchainStatus(id, ToolchainState.INSTALLING))
        return@synchronized try {
            val execution = executor.execute(detector.planInstall(profile).command, 900)
            if (!execution.succeeded) error(execution.stderr.ifBlank { "instalação falhou" })
            val after = detector.detect(profile)
            check(after.installed) { "validação pós-instalação falhou" }
            persist(ToolchainStatus(id, ToolchainState.INSTALLED, after.versionOutput))
        } catch (error: Exception) {
            persist(ToolchainStatus(id, ToolchainState.FAILED, error = error.message ?: error.javaClass.simpleName))
        }
    }

    fun remove(id: String): ToolchainStatus = synchronized(lock) {
        val profile = profile(id)
        persist(ToolchainStatus(id, ToolchainState.REMOVING))
        return@synchronized try {
            val packages = profile.packages.joinToString(" ")
            val command = listOf("bash", "-c", "export DEBIAN_FRONTEND=noninteractive; apt-get -o Dpkg::Use-Pty=0 remove -y $packages")
            val execution = executor.execute(command, 900)
            check(execution.succeeded) { execution.stderr.ifBlank { "remoção falhou" } }
            persist(ToolchainStatus(id, ToolchainState.NOT_INSTALLED))
        } catch (error: Exception) {
            persist(ToolchainStatus(id, ToolchainState.FAILED, error = error.message ?: error.javaClass.simpleName))
        }
    }

    private fun profile(id: String): ToolchainProfile = profilesById[id] ?: error("Toolchain desconhecida: $id")
    private fun stateFile(profile: ToolchainProfile) = File(stateDir, "${profile.id}.state")
    private fun persist(status: ToolchainStatus): ToolchainStatus {
        stateDir.mkdirs()
        stateFile(profile(status.profileId)).writeText(listOf(status.state.name, status.versionOutput, status.error.orEmpty()).joinToString("\n"))
        return status
    }
}
