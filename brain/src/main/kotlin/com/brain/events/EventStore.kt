package com.brain.events

import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID
import java.security.MessageDigest

data class BrainEvent(
    val runId: String,
    val sessionId: String,
    val taskId: String,
    val type: String,
    val sequence: Long,
    val eventId: String = UUID.randomUUID().toString(),
    val timestamp: Instant = Instant.now(),
    val payload: Map<String, String> = emptyMap(),
    val version: Int = 1,
    val previousHash: String = "GENESIS",
    val hash: String = "",
    val idempotencyKey: String? = null
) {
    fun finalized(previous: String, sequence: Long): BrainEvent {
        val candidate = copy(sequence = sequence, previousHash = previous, hash = "")
        return candidate.copy(hash = sha256(candidate.canonical()))
    }
    fun canonical() = listOf(runId, sessionId, taskId, type, sequence, eventId, timestamp, version, previousHash, payload.toSortedMap(), idempotencyKey).joinToString("|")
}

interface EventStore {
    fun append(event: BrainEvent): BrainEvent
    fun replay(runId: String? = null): List<BrainEvent>
    fun verifyIntegrity(): Boolean
}

class InMemoryEventStore : EventStore {
    private val events = mutableListOf<BrainEvent>()
    @Synchronized override fun append(event: BrainEvent): BrainEvent {
        event.idempotencyKey?.let { key -> events.firstOrNull { it.idempotencyKey == key }?.let { return it } }
        val previous = events.lastOrNull()?.hash ?: "GENESIS"
        val stored = event.copy(payload = event.redactedPayload()).finalized(previous, events.size.toLong())
        events += stored
        return stored
    }
    @Synchronized override fun replay(runId: String?): List<BrainEvent> = events.filter { runId == null || it.runId == runId }.toList()
    @Synchronized override fun verifyIntegrity(): Boolean = verify(events)
}

class FileEventStore(private val file: File) : EventStore {
    init { file.parentFile?.mkdirs() }
    @Synchronized override fun append(event: BrainEvent): BrainEvent {
        val current = readAll()
        event.idempotencyKey?.let { key -> current.firstOrNull { it.idempotencyKey == key }?.let { return it } }
        val stored = event.copy(payload = event.redactedPayload()).finalized(current.lastOrNull()?.hash ?: "GENESIS", current.size.toLong())
        file.parentFile?.mkdirs()
        file.appendText(toJson(stored).toString() + "\n")
        return stored
    }
    @Synchronized override fun replay(runId: String?): List<BrainEvent> = readAll().filter { runId == null || it.runId == runId }
    @Synchronized override fun verifyIntegrity(): Boolean = verify(readAll())

    private fun readAll(): List<BrainEvent> {
        if (!file.exists()) return emptyList()
        val lines = file.readLines()
        val valid = mutableListOf<String>()
        for ((index, line) in lines.withIndex()) {
            if (line.isBlank()) continue
            val parsed = runCatching { JSONObject(line) }.getOrNull()
            if (parsed == null && index == lines.lastIndex && file.readBytes().lastOrNull() != '\n'.code.toByte()) break
            if (parsed == null) error("evento inválido na linha ${index + 1}")
            valid += line
        }
        if (valid.size < lines.count { it.isNotBlank() } && file.exists()) file.writeText(valid.joinToString("\n") + if (valid.isEmpty()) "" else "\n")
        return valid.map { fromJson(JSONObject(it)) }
    }
}

private fun toJson(e: BrainEvent) = JSONObject().apply {
    put("runId", e.runId); put("sessionId", e.sessionId); put("taskId", e.taskId); put("type", e.type); put("sequence", e.sequence)
    put("eventId", e.eventId); put("timestamp", e.timestamp.toString()); put("version", e.version); put("previousHash", e.previousHash); put("hash", e.hash); put("idempotencyKey", e.idempotencyKey ?: JSONObject.NULL)
    put("payload", JSONObject(e.payload))
}
private fun fromJson(j: JSONObject) = BrainEvent(j.getString("runId"), j.getString("sessionId"), j.getString("taskId"), j.getString("type"), j.getLong("sequence"), j.getString("eventId"), Instant.parse(j.getString("timestamp")), j.optJSONObject("payload")?.let { obj -> obj.keys().asSequence().associateWith { obj.getString(it) } } ?: emptyMap(), j.optInt("version", 1), j.optString("previousHash", "GENESIS"), j.optString("hash", ""), j.optString("idempotencyKey").takeUnless { it == "null" })
private fun verify(events: List<BrainEvent>): Boolean {
    var previous = "GENESIS"
    events.forEachIndexed { index, event -> if (event.sequence != index.toLong() || event.previousHash != previous || event.hash != sha256(event.canonical())) return false else previous = event.hash }
    return true
}
private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
fun BrainEvent.redactedPayload(): Map<String, String> = payload.mapValues { (_, value) -> Regex("(?i)(key|token|secret|password)\\s*[=:]\\s*[^,;\\s]+").replace(value, "[REDACTED]") }
