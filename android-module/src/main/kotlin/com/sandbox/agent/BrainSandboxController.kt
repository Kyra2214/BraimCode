package com.sandbox.agent

import com.brain.planner.PassoPlano
import com.brain.planner.PlanoExecucao
import com.brain.execution.RiskClass
import com.brain.policy.PolicyBroker
import com.brain.policy.FileApprovalStore
import com.brain.router.ApiCatalogRegistry
import com.brain.router.DefaultAIRouter
import com.brain.router.InMemoryApiCatalog
import com.sandbox.runtime.ManagedSandboxRuntime
import java.io.File

/**
 * Primeira fatia vertical da unificação Brain + Sandbox.
 *
 * O catálogo de APIs é instalado pelo app através do ApiCatalogRegistry. Se o
 * app ainda não tiver carregado as chaves/catálogo, mantém o catálogo vazio
 * como fallback seguro — nunca inventa provider/modelo.
 */
class BrainSandboxController(
    runtime: ManagedSandboxRuntime,
    rootfsDir: File,
    private val actor: String = "android-app"
) {
    private val approvals = FileApprovalStore(File(rootfsDir.parentFile ?: rootfsDir, "approvals.jsonl"))
    private val sandbox = Sandbox(runtime = runtime, rootfsDir = rootfsDir)
    private val policy = PolicyBroker(
        allowedCapabilities = setOf("sandbox.health"),
        actorCapabilities = mapOf(actor to setOf("sandbox.health"))
    )
    private val apiCatalog = ApiCatalogRegistry.current() ?: InMemoryApiCatalog(emptyList())
    private val bridge = BrainSandboxExecutionBridge(
        CicloExecucaoPlano(
            policyBroker = policy,
            sandbox = sandbox,
            router = DefaultAIRouter(),
            catalog = apiCatalog,
            approvalStore = approvals
        )
    )

    /** Executa o primeiro caso de uso real do Brain dentro do Sandbox preparado. */
    fun healthCheck(runId: String): ResultadoCiclo {
        require(runId.isNotBlank()) { "runId não pode ser vazio" }
        val plano = PlanoExecucao(
            objetivo = "verificar saúde do Sandbox pelo Brain",
            passos = listOf(
                PassoPlano(
                    id = "sandbox-health",
                    capacidade = "sandbox.health",
                    criterioSucesso = "a capability sandbox.health deve concluir sem falha"
                )
            )
        )
        return bridge.authorizeAndExecute(plano, runId = runId, actor = actor)
    }

    fun executePlan(plano: PlanoExecucao, runId: String = "plan-${System.currentTimeMillis()}"): ResultadoCiclo =
        bridge.authorizeAndExecute(plano, runId = runId, actor = actor)

    fun resumePlan(plano: PlanoExecucao, runId: String, approvalId: String): ResultadoCiclo =
        bridge.resume(plano, runId = runId, actor = actor, approvalId = approvalId)

    fun approvalDemoPlan(): PlanoExecucao = PlanoExecucao(
        objetivo = "executar plano de demonstração com aprovação humana",
        passos = listOf(
            PassoPlano(
                id = "approval-demo",
                capacidade = "sandbox.health",
                criterioSucesso = "sandbox.health deve concluir após aprovação",
                riskClass = RiskClass.HIGH
            )
        )
    )
}
