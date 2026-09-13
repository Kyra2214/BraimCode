package com.sandbox.app

import com.brain.discovery.ExplorerCandidate
import com.brain.discovery.ExplorerIntelligencePipeline
import com.brain.discovery.ExplorerItemType
import com.brain.discovery.ExplorerRegion
import com.brain.discovery.ExplorerSource
import com.brain.discovery.ExplorerLicense
import com.brain.discovery.OpenSourceStatus
import com.brain.delivery.DeliveryReceipt
import com.brain.delivery.ObservableDelivery
import com.brain.memory.Experiencia
import com.brain.memory.ExperienceMemory
import com.brain.memory.FileExperienceMemory
import com.brain.memory.ResultadoExperiencia
import com.brain.skill.SkillManifest
import com.brain.skill.SkillRecord
import com.brain.skill.SkillRegistry
import com.brain.skill.TrustLevel
import com.brain.workflow.WorkflowEngine
import com.brain.workflow.WorkflowManifest
import com.brain.workflow.WorkflowNode
import com.brain.workflow.WorkflowRunResult
import com.brain.workflow.WorkflowStepResult
import java.io.File
import java.time.Instant

/** Fachada Android para os subsistemas Brain que não devem ser instanciados pela UI. */
class BrainIntegrationFacade(stateDir: File) {
    private val skills = SkillRegistry()
    private val workflows = WorkflowEngine(File(stateDir, "workflows.json"))
    private val memory: ExperienceMemory = FileExperienceMemory(File(stateDir, "memory.jsonl"))
    private val discovery = ExplorerIntelligencePipeline()
    private val delivery = ObservableDelivery()

    init {
        skills.register(
            SkillManifest(
                id = "sandbox-health",
                name = "Sandbox Health",
                version = "1.0.0",
                description = "Verifica a saúde do runtime Sandbox.",
                category = "validation",
                capabilities = setOf("sandbox.health"),
                trustLevel = TrustLevel.CORE,
                sourceId = "brain-builtin",
                license = "Apache-2.0"
            )
        )
    }

    fun enabledSkills(): List<SkillRecord> = skills.listEnabled()

    fun runHealthWorkflow(runId: String): WorkflowRunResult = workflows.run(
        manifest = WorkflowManifest(
            id = "sandbox-health-workflow",
            version = "1.0.0",
            nodes = listOf(WorkflowNode("health", "sandbox.health", retryLimit = 1))
        ),
        runId = runId,
        idempotencyKey = "sandbox-health:$runId",
        authorize = { it == "sandbox.health" },
        execute = { node, attempt -> WorkflowStepResult(node.id, success = true, attempts = attempt, output = mapOf("capability" to node.capability)) }
    )

    suspend fun recordExperience(runId: String, success: Boolean) {
        memory.registrar(
            Experiencia(
                id = "ui:$runId",
                tarefaId = "ui:$runId",
                problema = "operação iniciada pela aba Operações",
                estrategiaUsada = "local-sandbox",
                promptUsado = null,
                resultado = if (success) ResultadoExperiencia.SUCESSO else ResultadoExperiencia.FALHA,
                custoEstimado = 0.0,
                tempoTotalMs = 0,
                erros = emptyList(),
                registradoEm = Instant.now()
            )
        )
    }

    suspend fun memoryRate(): Double = memory.taxaSucessoPorEstrategia("local-sandbox")

    /** Publica somente um recibo local dos artefatos; não envia dados para rede. */
    fun publishLocalDelivery(root: File, runId: String): DeliveryReceipt = delivery.publish(
        root = root,
        runId = runId,
        sessionId = "android-local",
        taskId = "workspace-delivery",
        stepId = "collect-artifacts",
        eventId = "delivery:$runId",
        provider = "local-sandbox",
        startedAt = Instant.now()
    )

    fun discoverBuiltInCandidate(): com.brain.discovery.ExplorerPipelineResult = discovery.run(
        weekEpochMs = System.currentTimeMillis(),
        sources = listOf(ExplorerSource("builtin", "Catálogo local", ExplorerRegion.GLOBAL, priority = 1, official = true)),
        candidates = listOf(
            ExplorerCandidate(
                id = "braincode",
                name = "BrainCode",
                type = ExplorerItemType.FRAMEWORK,
                region = ExplorerRegion.GLOBAL,
                officialUrl = "https://github.com/Kyra2214/BrainCode",
                repositoryUrl = "https://github.com/Kyra2214/BrainCode",
                description = "Runtime Brain e Sandbox Mobile.",
                capabilities = setOf("sandbox", "workflow", "policy"),
                openSource = OpenSourceStatus.OPEN,
                license = ExplorerLicense.APACHE_2,
                licenseVerified = true,
                sourcePriority = 1,
                confidence = 1.0
            )
        )
    )
}
