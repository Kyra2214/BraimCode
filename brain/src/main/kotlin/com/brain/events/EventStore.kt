package com.brain.events

import java.io.File
import java.time.Instant
import java.util.UUID

data class BrainEvent(
    val runId: String,
    val sessionId: String,
    val taskId: String,
    val type: String,
    val sequence: Long,
    val eventId: String = UUID.randomUUID().toString(),
    val timestamp: Instant = Instant.now(),
    val payload: Map<String, String> = emptyMap()
)

interface EventStore {
    fun append(event: BrainEvent): BrainEvent
    fun replay(runId: String? = null): List<BrainEvent>
}

class InMemoryEventStore : EventStore {
    private val events = mutableListOf<BrainEvent>()
    @Synchronized override fun append(event: BrainEvent): BrainEvent {
        val stored = event.copy(sequence = events.count { it.runId == event.runId }.toLong())
        events += stored
        return stored
    }
    @Synchronized override fun replay(runId: String?): List<BrainEvent> =
        events.filter { runId == null || it.runId == runId }.toList()
}

class FileEventStore(private val file: File) : EventStore {
    init { file.parentFile?.mkdirs() }
    @Synchronized override fun append(event: BrainEvent): BrainEvent {
        val stored = event.copy(sequence = replay(event.runId).size.toLong())
        file.appendText(stored.toLine() + "\n")
        return stored
    }
    @Synchronized override fun replay(runId: String?): List<BrainEvent> =
        if (file.exists()) file.readLines().mapNotNull { parseEvent(it) }
            .filter { runId == null || it.runId == runId } else emptyList()
}

private fun BrainEvent.toLine() = listOf(runId, sessionId, taskId, type, sequence, eventId, timestamp, payload.entries.joinToString(";") { "${it.key}=${it.value}" }).joinToString("\t")
private fun parseEvent(line: String): BrainEvent? = runCatching {
    val p = line.split("\t", limit = 8)
    BrainEvent(p[0], p[1], p[2], p[3], p[4].toLong(), p[5], Instant.parse(p[6]), if (p.size == 8 && p[7].isNotEmpty()) p[7].split(";").associate { it.substringBefore("=") to it.substringAfter("=") } else emptyMap())
}.getOrNull()

fun BrainEvent.redactedPayload(): Map<String, String> = payload.mapValues { (_, value) ->
    Regex("(?i)(key|token|secret|password)\\s*[=:]\\s*[^,;\\s]+").replace(value, "[REDACTED]")
}
