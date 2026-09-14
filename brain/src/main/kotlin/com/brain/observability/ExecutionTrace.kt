package com.brain.observability

import java.time.Instant

/** Etapas observáveis do fluxo arquitetural do BrainCode. */
enum class TraceStage { TASK, PLAN, CAPABILITY, POLICY, AGENT, SANDBOX, EVIDENCE, CRITIC }

data class TraceEvent(
    val traceId: String,
    val stage: TraceStage,
    val status: String,
    val subjectId: String,
    val timestamp: Instant = Instant.now(),
    val details: Map<String, String> = emptyMap()
)

interface TraceSink { fun append(event: TraceEvent) }

class InMemoryTraceSink : TraceSink {
    private val events = mutableListOf<TraceEvent>()
    private val lock = Any()
    override fun append(event: TraceEvent) = synchronized(lock) { events += event }
    fun all(traceId: String? = null): List<TraceEvent> = synchronized(lock) {
        events.filter { traceId == null || it.traceId == traceId }.toList()
    }
}

/** Renderiza a visualização técnica em texto estável para logs e auditoria. */
class ExecutionTrace(private val sink: TraceSink) {
    fun record(traceId: String, stage: TraceStage, status: String, subjectId: String, details: Map<String, String> = emptyMap()) =
        sink.append(TraceEvent(traceId, stage, status, subjectId, details = details))

    fun render(events: List<TraceEvent>): String = events.sortedBy { it.timestamp }.joinToString("\n") {
        "${it.stage.name}(${it.status}):${it.subjectId}"
    }
}
