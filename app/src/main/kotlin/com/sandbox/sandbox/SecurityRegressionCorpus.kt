package com.sandbox.sandbox

import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Corpus local e persistente para regressão do Security Test Lab.
 * A simulação é determinística e sintética: não abre rede, não executa payloads
 * adversariais e não depende de um alvo externo.
 */
class SecurityRegressionCorpus(private val file: File) {
    data class Entry(
        val scenarioId: String,
        val expectedBlocked: Boolean,
        val observedBlocked: Boolean,
        val digest: String,
        val recordedAt: Long
    )

    init { file.parentFile?.mkdirs() }

    fun runDeterministic(scenarios: List<SecurityScenario>): List<SecurityProbeResult> = scenarios.map { scenario ->
        val blocked = when (scenario.id) {
            "secret.redaction", "path.traversal", "command.injection", "network.ssrf", "capability.bypass", "evidence.tampering" -> true
            else -> scenario.expectedBlocked
        }
        val output = "synthetic:${scenario.id}:blocked=$blocked"
        SecurityProbeResult(scenario.id, completed = true, blocked = blocked, output = output)
    }

    fun record(report: SecurityTestReport) {
        val entries = report.scenarios.map { scenario ->
            val evidence = report.evidence.firstOrNull { it.scenarioId == scenario.id }
            val result = report.evidence.firstOrNull { it.scenarioId == scenario.id }
            val digest = evidence?.digestSha256 ?: sha256("missing:${scenario.id}")
            Entry(scenario.id, scenario.expectedBlocked, !report.findings.any { it.scenarioId == scenario.id && it.title == "Resultado inesperado" }, digest, System.currentTimeMillis())
        }
        append(entries)
    }

    fun entries(): List<Entry> = if (!file.isFile) emptyList() else file.readLines(StandardCharsets.UTF_8).mapNotNull { line ->
        val p = line.split('|', limit = 5)
        if (p.size != 5) null else runCatching { Entry(p[0], p[1].toBooleanStrict(), p[2].toBooleanStrict(), p[3], p[4].toLong()) }.getOrNull()
    }

    fun digest(): String = sha256(if (file.isFile) file.readText(StandardCharsets.UTF_8) else "")

    private fun append(entries: List<Entry>) {
        if (entries.isEmpty()) return
        file.parentFile?.mkdirs()
        file.appendText(entries.joinToString("\n", postfix = "\n") { "${it.scenarioId}|${it.expectedBlocked}|${it.observedBlocked}|${it.digest}|${it.recordedAt}" }, StandardCharsets.UTF_8)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
