package com.brain.memory

import java.util.Locale
import java.util.UUID

/** Proveniência de um conhecimento aprendido fora da memória do Brain. */
data class KnowledgeSource(
    val type: String,
    val uri: String? = null,
    val providerId: String? = null,
    val modelId: String? = null,
    val repository: String? = null,
    val path: String? = null,
    val commit: String? = null
)

data class KnowledgeEntry(
    val id: String = UUID.randomUUID().toString(),
    val problem: String,
    val answer: String,
    val source: KnowledgeSource?,
    val retrievalHints: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val confidence: Double = 0.0,
    val validated: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val confirmations: Int = 0
)

interface KnowledgeMemory {
    fun findValidated(problem: String, minScore: Double = 0.55): KnowledgeEntry?
    fun saveCandidate(entry: KnowledgeEntry): KnowledgeEntry
    fun confirm(id: String, confidence: Double, source: KnowledgeSource? = null): KnowledgeEntry?
    fun recordCorrection(id: String, correctedAnswer: String, confidence: Double): KnowledgeEntry?
    fun all(): List<KnowledgeEntry>
}

/** Implementação determinística para testes e fallback; o Android pode instalar memória persistente. */
class InMemoryKnowledgeMemory : KnowledgeMemory {
    private val entries = LinkedHashMap<String, KnowledgeEntry>()

    @Synchronized
    override fun findValidated(problem: String, minScore: Double): KnowledgeEntry? =
        entries.values.asSequence()
            .filter { it.validated }
            .map { it to similarity(problem, it.problem) }
            .filter { it.second >= minScore }
            .maxByOrNull { it.second }?.first

    @Synchronized
    override fun saveCandidate(entry: KnowledgeEntry): KnowledgeEntry {
        entries[entry.id] = entry
        return entry
    }

    @Synchronized
    override fun confirm(id: String, confidence: Double, source: KnowledgeSource?): KnowledgeEntry? {
        val current = entries[id] ?: return null
        val updated = current.copy(
            confidence = confidence.coerceIn(0.0, 1.0),
            validated = true,
            source = source ?: current.source,
            confirmations = current.confirmations + 1
        )
        entries[id] = updated
        return updated
    }

    @Synchronized
    override fun recordCorrection(id: String, correctedAnswer: String, confidence: Double): KnowledgeEntry? {
        val current = entries[id] ?: return null
        val updated = current.copy(
            answer = correctedAnswer,
            confidence = confidence.coerceIn(0.0, 1.0),
            validated = true,
            confirmations = current.confirmations + 1
        )
        entries[id] = updated
        return updated
    }

    @Synchronized
    override fun all(): List<KnowledgeEntry> = entries.values.toList()

    private fun similarity(a: String, b: String): Double {
        val left = tokens(a)
        val right = tokens(b)
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val intersection = left.intersect(right).size.toDouble()
        return (2.0 * intersection / (left.size + right.size)).coerceIn(0.0, 1.0)
    }

    private fun tokens(value: String): Set<String> = value.lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}_]+"), " ")
        .split(' ').filter { it.length >= 3 }.toSet()
}

object KnowledgeMemoryRegistry {
    @Volatile private var currentMemory: KnowledgeMemory = InMemoryKnowledgeMemory()
    fun current(): KnowledgeMemory = currentMemory
    fun install(memory: KnowledgeMemory) { currentMemory = memory }
}
