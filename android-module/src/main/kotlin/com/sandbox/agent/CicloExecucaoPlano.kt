package com.sandbox.agent

import com.brain.planner.AuthorizedPlan
import com.brain.planner.PassoPlano
import com.brain.planner.PlanoExecucao
import com.brain.policy.*
import com.brain.qa.EvidenciaComando
import com.brain.qa.ExecutorValidacaoProjeto
import com.brain.qa.ResultadoValidacao
import com.brain.router.AIRouter
import com.brain.router.ApiCatalog
import com.brain.router.RoutingDecision
import com.brain.router.RoutingProfile


enum class StatusPasso { APROVADO, REPROVADO, NEGADO_PELA_POLICY, AGUARDANDO_APROVACAO, BLOQUEADO_POR_DEPENDENCIA }

data class ResultadoPasso(
    val passoId: String,
    val status: StatusPasso,
    val decisaoPolicy: PolicyDecision? = null,
    val decisaoRouter: RoutingDecision? = null,
    val execucao: AgentSandboxSession.CommandOutcome? = null,
    val evidencias: List<EvidenciaComando> = emptyList(),
    val motivo: String? = null,
    val approvalId: String? = null
)

data class ResultadoCiclo(
    val objetivo: String,
    val runId: String,
    val passos: List<ResultadoPasso>
) {
    val aprovado: Boolean get() = passos.isNotEmpty() && passos.all { it.status == StatusPasso.APROVADO }
}

/** Executa somente planos que já carregam autorizações por passo emitidas pelo Brain. */
class CicloExecucaoPlano(
    private val policyBroker: PolicyBroker,
    private val sandbox: Sandbox,
    private val router: AIRouter,
    private val catalog: ApiCatalog,
    private val validador: ExecutorValidacaoProjeto = ExecutorValidacaoProjeto(),
    private val profiles: List<RoutingProfile> = emptyList(),
    private val approvalStore: ApprovalStore? = null
) {
    private val approvedSteps = mutableSetOf<String>()

    /** Fronteira Agent/Sandbox: não aceita PlanoExecucao cru. */
    fun executar(autorizado: AuthorizedPlan, runId: String, actor: String): ResultadoCiclo {
        val plano = autorizado.plan
        val resultados = mutableListOf<ResultadoPasso>()
        val concluidos = mutableSetOf<String>()
        var abortado = false
        for (passo in plano.ordemDeExecucao) {
            if (abortado) {
                resultados += ResultadoPasso(passo.id, StatusPasso.BLOQUEADO_POR_DEPENDENCIA, motivo = "ciclo abortado por passo anterior")
                continue
            }
            val dependencia = passo.dependeDe.firstOrNull { it !in concluidos }
            if (dependencia != null) {
                resultados += ResultadoPasso(passo.id, StatusPasso.BLOQUEADO_POR_DEPENDENCIA, motivo = "depende de '$dependencia'")
                continue
            }
            val resultado = processarPasso(passo, autorizado.authorizations.getValue(passo.id), autorizado.decisions[passo.id])
            resultados += resultado
            if (resultado.status == StatusPasso.APROVADO) concluidos += passo.id else abortado = true
        }
        return ResultadoCiclo(plano.objetivo, runId, resultados)
    }

    /** Autoriza todos os passos antes de emitir o wrapper aceito pelo Agent. */
    fun autorizarEExecutar(plano: PlanoExecucao, runId: String, actor: String): ResultadoCiclo {
        val authorizations = linkedMapOf<String, ExecutionAuthorization>()
        val decisions = linkedMapOf<String, PolicyDecision>()
        for (passo in plano.ordemDeExecucao) {
            val highRisk = passo.riskClass == com.brain.execution.RiskClass.HIGH || passo.riskClass == com.brain.execution.RiskClass.CRITICAL
            val contexto = PolicyContext(runId, passo.id, actor, riskClass = passo.riskClass, approval = if (highRisk && passo.id !in approvedSteps) ApprovalRequired.USER else ApprovalRequired.NONE)
            val decision = policyBroker.authorize(actor, passo.capacidade, passo.id, contexto)
            if (decision.decision != Decision.ALLOW) {
                val approvalId = if (decision.decision == Decision.ASK) approvalStore?.create(ApprovalRequest(runId, passo.id, passo.capacidade, passo.id, java.time.Instant.parse(decision.expiresAt)))?.request?.id else null
                return ResultadoCiclo(plano.objetivo, runId, listOf(ResultadoPasso(passo.id, if (decision.decision == Decision.ASK) StatusPasso.AGUARDANDO_APROVACAO else StatusPasso.NEGADO_PELA_POLICY, decisaoPolicy = decision, motivo = decision.reason, approvalId = approvalId)))
            }
            authorizations[passo.id] = ExecutionAuthorization.fromDecision(decision)
                ?: return ResultadoCiclo(plano.objetivo, runId, listOf(ResultadoPasso(passo.id, StatusPasso.NEGADO_PELA_POLICY, decisaoPolicy = decision, motivo = "autorização inválida")))
            decisions[passo.id] = decision
        }
        return executar(AuthorizedPlan.issue(plano, authorizations, decisions), runId, actor)
    }

    fun retomar(plano: PlanoExecucao, runId: String, actor: String, approvalId: String): ResultadoCiclo {
        val approval = approvalStore?.consume(approvalId)
            ?: return ResultadoCiclo(plano.objetivo, runId, listOf(ResultadoPasso("approval", StatusPasso.NEGADO_PELA_POLICY, motivo = "aprovação inexistente, já consumida ou expirada", approvalId = approvalId)))
        require(approval.request.runId == runId) { "aprovação pertence a outro runId" }
        approvedSteps += approval.request.taskId
        return try { autorizarEExecutar(plano, runId, actor) } finally { approvedSteps -= approval.request.taskId }
    }

    private fun processarPasso(passo: PassoPlano, authorization: ExecutionAuthorization, decision: PolicyDecision?): ResultadoPasso {
        val decisaoRouter = passo.papel?.let { router.decidir(it, catalog, profiles) }
        sandbox.abrirSessao(authorization).use { sessao ->
            val execucao = sessao.rodarCapacidade(passo.parametros)
            if (execucao is AgentSandboxSession.CommandOutcome.Refused) return ResultadoPasso(passo.id, StatusPasso.REPROVADO, decisaoPolicy = decision, decisaoRouter = decisaoRouter, execucao = execucao, motivo = execucao.reason)
            val evidencias = validador.validar(sessao.workspaceHostPath)
            val reprovado = evidencias.any { it.resultado == ResultadoValidacao.FALHOU }
            return ResultadoPasso(passo.id, if (reprovado) StatusPasso.REPROVADO else StatusPasso.APROVADO, decisaoPolicy = decision, decisaoRouter = decisaoRouter, execucao = execucao, evidencias = evidencias, motivo = if (reprovado) "validação encontrou evidência FALHOU" else null)
        }
    }
}
