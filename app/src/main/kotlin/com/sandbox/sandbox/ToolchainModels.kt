package com.sandbox.sandbox

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
