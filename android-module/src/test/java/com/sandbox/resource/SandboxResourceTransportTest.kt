package com.sandbox.resource

import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SandboxResourceTransportTest {
    private val bytes = "rootfs-test".toByteArray()
    private val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun manifest(url: String) = RootfsManifest("1.0.0", "arm64-v8a", "test", url, bytes.size.toLong(), sha256, "1.0.0", signatureRequired = false)

    private fun publicAddress(host: String): Array<InetAddress> = arrayOf(InetAddress.getByAddress(host, byteArrayOf(93, 0, 0, 34)))

    @Test
    fun `nao abre conexao para destino privado`() {
        val target = Files.createTempFile("rootfs-transport", ".tar.gz").toFile()
        val called = AtomicInteger()
        try {
            val result = SandboxResourceManager(target, connectionFactory = { called.incrementAndGet(); error("não deveria conectar") })
                .ensureAvailable(manifest("https://127.0.0.1/rootfs.tar.gz"))
            assertEquals(0, called.get())
            assertTrue(result is SandboxResourceManager.DownloadResult.Failure)
        } finally { target.delete() }
    }

    @Test
    fun `segue redirect HTTPS do GitHub e valida SHA256`() {
        val target = Files.createTempFile("rootfs-redirect", ".tar.gz").toFile()
        val connections = mutableListOf<FakeConnection>()
        try {
            val manager = SandboxResourceManager(
                target,
                connectionFactory = { url ->
                    val connection = if (connections.isEmpty()) {
                        FakeConnection(url, 302, null, "https://release-assets.githubusercontent.com/rootfs.tar.gz")
                    } else {
                        FakeConnection(url, 200, bytes, null)
                    }
                    connections += connection
                    connection
                },
                addressResolver = ::publicAddress
            )
            val result = manager.ensureAvailable(manifest("https://github.com/Kyra2214/BrainCode/releases/download/rootfs-v0.3.3/rootfs.tar.gz"))
            assertTrue(result is SandboxResourceManager.DownloadResult.Success)
            assertEquals(2, connections.size)
            assertEquals("github.com", connections[0].hostHeader)
            assertEquals("release-assets.githubusercontent.com", connections[1].hostHeader)
            assertEquals(bytes.toList(), target.readBytes().toList())
        } finally { target.delete() }
    }

    @Test
    fun `rejeita downgrade HTTPS para HTTP`() {
        val target = Files.createTempFile("rootfs-downgrade", ".tar.gz").toFile()
        val called = AtomicInteger()
        try {
            val result = SandboxResourceManager(
                target,
                connectionFactory = { url ->
                    called.incrementAndGet()
                    FakeConnection(url, 302, null, "http://example.com/rootfs.tar.gz")
                },
                addressResolver = ::publicAddress
            ).ensureAvailable(manifest("https://github.com/rootfs.tar.gz"))
            assertTrue(result is SandboxResourceManager.DownloadResult.Failure)
            assertEquals(1, called.get())
            assertFalse(target.exists() && target.length() > 0)
        } finally { target.delete() }
    }

    private class FakeConnection(
        url: URL,
        private val code: Int,
        private val body: ByteArray?,
        private val location: String?
    ) : HttpURLConnection(url) {
        var hostHeader: String? = null

        override fun setRequestProperty(key: String, value: String) {
            if (key.equals("Host", ignoreCase = true)) hostHeader = value
        }
        override fun getResponseCode(): Int = code
        override fun getHeaderField(name: String): String? = if (name.equals("Location", ignoreCase = true)) location else null
        override fun getInputStream() = ByteArrayInputStream(body ?: ByteArray(0))
        override fun connect() { connected = true }
        override fun disconnect() { connected = false }
        override fun usingProxy(): Boolean = false
    }
}
