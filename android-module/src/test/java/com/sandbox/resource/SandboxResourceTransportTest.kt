package com.sandbox.resource

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertFalse
import org.junit.Test

class SandboxResourceTransportTest {
    private fun manifest(url: String) = RootfsManifest("1.0.0", "arm64-v8a", "test", url, 1, "0".repeat(64), "1.0.0")

    @Test
    fun `nao abre conexao para destino privado`() {
        val target = Files.createTempFile("rootfs-transport", ".tar.gz").toFile()
        val called = AtomicBoolean(false)
        try {
            val result = SandboxResourceManager(target, connectionFactory = { called.set(true); error("não deveria conectar") })
                .ensureAvailable(manifest("https://127.0.0.1/rootfs.tar.gz"))
            assertFalse(called.get())
            assert(result is SandboxResourceManager.DownloadResult.Failure)
        } finally { target.delete() }
    }
}
