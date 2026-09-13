package com.sandbox.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.assertThrows
import java.io.File

class ProotProcessLauncherNetworkTest {
    private fun launcher(unshare: String? = "/system/bin/unshare"): ProotProcessLauncher {
        val rootfs = createTempDir(prefix = "rootfs-")
        val proot = File.createTempFile("proot-", ".bin").apply { setExecutable(true) }
        val tmp = createTempDir(prefix = "tmp-")
        return ProotProcessLauncher(
            prootExecutable = proot.absolutePath,
            rootfsDir = rootfs,
            tmpDir = tmp,
            executableFinder = { candidates ->
                if (candidates.contains("/system/bin/unshare")) unshare else "/system/bin/setsid"
            }
        )
    }

    @Test
    fun `rede permitida nao adiciona unshare nem n`() {
        val launcher = launcher()
        val args = launcher.buildArgs(listOf("echo", "ok"), "/tmp", "/system/bin/setsid", null)

        assertFalse(args.contains("/system/bin/unshare"))
        assertFalse(args.contains("-n"))
    }

    @Test
    fun `rede proibida adiciona unshare n e separador na ordem`() {
        val launcher = launcher()
        val args = launcher.buildArgs(listOf("echo", "ok"), "/tmp", "/system/bin/setsid", "/system/bin/unshare")

        assertEquals("/system/bin/setsid", args[0])
        assertEquals("/system/bin/unshare", args[1])
        assertEquals("-n", args[2])
        assertEquals("--", args[3])
        assertTrue(args[4].endsWith(".bin"))
    }

    @Test
    fun `rede proibida sem unshare falha fechado`() {
        val launcher = launcher(unshare = null)
        val exception = assertThrows(UnsupportedOperationException::class.java) {
            launcher.launch(listOf("echo", "ok"), "/tmp", networkAllowed = false)
        }
        assertTrue(exception.message.orEmpty().contains("unshare"))
    }
}
