package com.brain.execution

import com.brain.events.BrainEvent
import com.brain.events.EventStore
import com.brain.memory.Experiencia
import com.brain.memory.ExperienceMemory
import com.brain.memory.ResultadoExperiencia
import com.brain.planner.PlanoExecucao
import com.brain.policy.*
import com.brain.router.*
import java.time.Instant
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

data class StepAttempt(val success: Boolean, val output: String = "", val error: String? = null)
interface StepExecutor { fun execute(step: com.brain.planner.PassoPlano, provider: ProviderModel?): StepAttempt }
enum class CoordinatorStatus { COMPLETED, FAILED, WAITING_APPROVAL }
data class CoordinatorResult(val runId: String, val status: CoordinatorStatus, val attempts: Map<String, Int>, val errors: List<String> = emptyList())

class BrainExecutionCoordinator(
    private val policy: PolicyBroker,
    private val events: EventStore,
    private val approvals: ApprovalStore,
    private val router: AIRouter,
    private val catalog: ApiCatalog,
    private val profiles: List<RoutingProfile> = emptyList(),
    private val maxRetries: Int = 1,
    private val memory: ExperienceMemory? = null
) {
    fun execute(plano: PlanoExecucao, runId: String, actor: String, executor: StepExecutor): CoordinatorResult {
        val attempts = linkedMapOf<String, Int>(); val errors = mutableListOf<String>()
        for (step in plano.ordemDeExecucao) {
            val route = step.papel?.let { router.decidir(it, catalog, profiles) }
            val decision = policy.authorize(actor, step.capacidade, step.id, PolicyContext(runId = runId, taskId = step.id, actor = actor, riskClass = step.riskClass))
            emit(runId, step.id, "PolicyChecked", mapOf("decision" to decision.decision.name, "capability" to step.capacidade))
            if (decision.decision == Decision.ASK) {
                approvals.create(ApprovalRequest(runId = runId, taskId = step.id, capability = step.capacidade, resource = step.id, expiresAt = Instant.parse(decision.expiresAt)))
                emit(runId, step.id, "ApprovalRequested", emptyMap())
                return CoordinatorResult(runId, CoordinatorStatus.WAITING_APPROVAL, attempts, errors)
            }
            if (decision.decision != Decision.ALLOW) {
                errors += "${step.id}: ${decision.reason}"; emit(runId, step.id, "ValidationFailed", mapOf("error" to decision.reason)); return CoordinatorResult(runId, CoordinatorStatus.FAILED, attempts, errors)
            }
            emit(runId, step.id, "AgentDispatched", mapOf("provider" to (route?.escolhido?.providerId ?: "local")))
            val startedAt = System.currentTimeMillis()
            var result: StepAttempt? = null
            for (attempt in 0..maxRetries) {
                attempts[step.id] = attempt + 1
                result = executor.execute(step, route?.escolhido)
                if (result.success) break
                catalog.registrarResultado(route?.escolhido?.providerId ?: "local", route?.escolhido?.modeloId ?: "local", false, 0L, ErroObservado(TipoErro.DESCONHECIDO, Instant.now()))
                emit(runId, step.id, if (attempt < maxRetries) "Retry" else "ValidationFailed", mapOf("attempt" to (attempt + 1).toString(), "error" to (result.error ?: "failed")))
                if (attempt < maxRetries) emit(runId, step.id, "CorrectionRequested", mapOf("reason" to (result.error ?: "failed")))
            }
            val finalResult = result ?: StepAttempt(false, error = "failed")
            recordExperience(runId, plano, step, route?.escolhido, finalResult, attempts.getValue(step.id), System.currentTimeMillis() - startedAt)
            if (!finalResult.success) { errors += "${step.id}: ${finalResult.error ?: "failed"}"; return CoordinatorResult(runId, CoordinatorStatus.FAILED, attempts, errors) }
            emit(runId, step.id, "ValidationPassed", emptyMap())
        }
        emit(runId, "plan", "Delivered", emptyMap())
        return CoordinatorResult(runId, CoordinatorStatus.COMPLETED, attempts, errors)
    }

    private fun emit(runId: String, taskId: String, type: String, payload: Map<String, String>) {
        events.append(BrainEvent(runId = runId, sessionId = runId, taskId = taskId, type = type, sequence = 0, payload = payload, idempotencyKey = "$runId:$taskId:$type:${events.replay(runId).size}"))
    }

    private fun recordExperience(
        runId: String,
        plano: PlanoExecucao,
        step: com.brain.planner.PassoPlano,
        provider: ProviderModel?,
        result: StepAttempt,
        attemptCount: Int,
        elapsedMs: Long
    ) {
        val target = memory ?: return
        val strategy = provider?.let { "${it.providerId}/${it.modeloId}" } ?: "local"
        val outcome = when {
            !result.success -> ResultadoExperiencia.FALHA
            attemptCount > 1 -> ResultadoExperiencia.CORRIGIDO_APOS_FALHA
            else -> ResultadoExperiencia.SUCESSO
        }
        val experience = Experiencia(
            id = "$runId:${step.id}",
            tarefaId = step.id,
            problema = plano.objetivo,
            estrategiaUsada = strategy,
            promptUsado = null,
            resultado = outcome,
            custoEstimado = 0.0,
            tempoTotalMs = elapsedMs,
            erros = result.error?.let { listOf(it) } ?: emptyList(),
            registradoEm = Instant.now()
        )
        runCatching { await { target.registrar(experience) } }
            .onFailure { emit("memory", step.id, "MemoryRecordFailed", mapOf("error" to (it.message ?: "recording failed"))) }
    }

    private fun <T> await(block: suspend () -> T): T {
        var completed: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) { completed = result }
        })
        return completed!!.getOrThrow()
    }
}
