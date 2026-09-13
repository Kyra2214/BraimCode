package com.sandbox.agent

import com.brain.planner.PassoPlano
import com.brain.planner.PlanoExecucao
import com.brain.policy.PolicyBroker
import com.brain.router.DefaultAIRouter
import com.brain.router.InMemoryApiCatalog
import com.sandbox.runtime.ManagedSandboxRuntime
import java.io.File

/**
 * Primeira fatia vertical da unificação Brain + Sandbox.
 *
 * O app cria este controlador somente depois de preparar o runtime real. A
 * execução passa pelo mesmo caminho que os testes exercitam: plano -> Policy
 * -> sessão autorizada -> capability catalogada -> runtime proot -> evidência.
 * Não existe um executor paralelo para esta operação.
 */
class BrainSandboxController(
    runtime: ManagedSandboxRuntime,
    rootfsDir: File,
    private val actor: String = "android-app"
) {
    private val sandbox = Sandbox(runtime = runtime, rootfsDir = rootfsDir)
    private val policy = PolicyBroker(
        allowedCapabilities = setOf("sandbox.health"),
        actorCapabilities = mapOf(actor to setOf("sandbox.health"))
    )
    private val bridge = BrainSandboxExecutionBridge(
        CicloExecucaoPlano(
            policyBroker = policy,
            sandbox = sandbox,
            router = DefaultAIRouter(),
            catalog = InMemoryApiCatalog(emptyList())
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
        return bridge.execute(plano, runId = runId, actor = actor)
    }
}
