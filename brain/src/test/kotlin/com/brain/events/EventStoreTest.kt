package com.brain.events

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventStoreTest {
    private fun event(runId: String = "run-1", type: String = "JobStarted") = BrainEvent(
        runId = runId, sessionId = "session-1", taskId = "task-1", type = type, sequence = 999,
        payload = mapOf("message" to "ok")
    )

    @Test
    fun `in memory assigns monotonic sequence and filters by run`() {
        val store = InMemoryEventStore()
        assertEquals(0L, store.append(event()).sequence)
        assertEquals(1L, store.append(event()).sequence)
        assertEquals(2L, store.append(event("run-2")).sequence)
        assertEquals(2, store.replay("run-1").size)
    }

    @Test
    fun `file store recovers events after reopening`() {
        val file = Files.createTempFile("brain-events", ".log").toFile()
        try {
            FileEventStore(file).append(event(payload = mapOf("message" to "persisted")))
            val recovered = FileEventStore(file).replay("run-1")
            assertEquals(1, recovered.size)
            assertEquals("persisted", recovered.single().payload["message"])
        } finally {
            file.delete()
        }
    }

    @Test
    fun `redacted payload hides secret-like values`() {
        val redacted = event(payload = mapOf("api_key" to "api_key=super-secret", "safe" to "ok")).redactedPayload()
        assertTrue(redacted["api_key"]!!.contains("[REDACTED]"))
        assertEquals("ok", redacted["safe"])
    }

    private fun event(runId: String = "run-1", type: String = "JobStarted", payload: Map<String, String>) = BrainEvent(
        runId = runId, sessionId = "session-1", taskId = "task-1", type = type, sequence = 999, payload = payload
    )
}
