package com.sandbox.runtime

import java.io.File

/** Default launcher matching the already validated SandboxRuntime proot contract. */
class ProotProcessLauncher(
    private val prootExecutable: String,
    private val rootfsDir: File,
    private val tmpDir: File,
    private val disableSeccompAcceleration: Boolean = true,
    private val nativeLibraryDir: String? = null,
    private val prootLoader: String? = null
) : SandboxProcessLauncher {

    init {
        require(File(prootExecutable).isFile) { "Binário proot não encontrado em $prootExecutable" }
        require(rootfsDir.isDirectory) { "Rootfs não encontrado em ${rootfsDir.path}" }
        tmpDir.mkdirs()
        listOf("dev", "proc", "sys", "tmp").forEach { File(rootfsDir, it).mkdirs() }
    }

    override fun launch(command: List<String>, workingDir: String): Process {
        val args = buildList {
            add(prootExecutable)
            add("-r"); add(rootfsDir.absolutePath)
            add("-w"); add(workingDir)
            add("-0")
            add("-b"); add("/dev")
            add("-b"); add("/proc")
            add("-b"); add("/sys")
            add("--link2symlink")
            add("--kill-on-exit")
            add("/bin/bash"); add("-c")
            add(command.joinToString(" ") { shellEscape(it) })
        }
        return ProcessBuilder(args).redirectErrorStream(false).apply {
            environment().clear()
            environment()["LD_LIBRARY_PATH"] = nativeLibraryDir ?: File(prootExecutable).parentFile.absolutePath
            environment()["PATH"] = "/home/sandbox/venv/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
            environment()["HOME"] = workingDir
            environment()["USER"] = "sandbox"
            environment()["TERM"] = "xterm-256color"
            environment()["LANG"] = "C.UTF-8"
            environment()["TMPDIR"] = "/tmp"
            environment()["PROOT_TMP_DIR"] = tmpDir.absolutePath
            prootLoader?.let { environment()["PROOT_LOADER"] = it }
            if (disableSeccompAcceleration) environment()["PROOT_NO_SECCOMP"] = "1"
        }.start()
    }

    private fun shellEscape(arg: String): String = if (arg.matches(Regex("^[A-Za-z0-9_\\-./=]+$"))) arg
    else "'" + arg.replace("'", "'\\''") + "'"
}
