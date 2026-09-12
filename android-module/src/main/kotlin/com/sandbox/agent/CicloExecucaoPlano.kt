package com.sandbox.agent

import com.brain.planner.PassoPlano
import com.brain.planner.PlanoExecucao
import com.brain.policy.Decision
import com.brain.policy.ExecutionAuthorization
import com.brain.policy.PolicyBroker
import com.brain.policy.PolicyContext
import com.brain.policy.PolicyDecision
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
    val motivo: String? = null
)

data class ResultadoCiclo(
    val objetivo: String,
    val runId: String,
    val passos: List<ResultadoPasso>
) {
    val aprovado: Boolean get() = passos.isNotEmpty() && passos.all { it.status == StatusPasso.APROVADO }
}

/**
 * Etapa 6 do plano de integração Brain+Sandbox
 * (docs/PLANO_INTEGRACAO_BRAIN_SANDBOX.md): o primeiro ciclo real ligando
 * Policy (Etapa 2) + Sessão (Etapa 3) + CapabilityResolver (Etapa 4, já
 * embutido em [AgentSandboxSession.rodarCapacidade]) + Router (Etapa 5)
 * num fluxo único, com o Validator revisando cada passo antes do próximo
 * rodar — a "virada" que o plano descrevia como faltante.
 *
 * Por passo, nesta ordem: Router escolhe provider (só quando o passo
 * declara [PassoPlano.papel]) -> Policy autoriza -> Sandbox abre sessão ->
 * capacidade roda -> Validator roda contra o workspace da sessão. O ciclo
 * percorre [PlanoExecucao.ordemDeExecucao] (já topologicamente ordenada) e
 * aborta assim que um passo não fecha (política nega/pergunta, ou
 * validação reprova) — os passos restantes ficam BLOQUEADO_POR_DEPENDENCIA,
 * nunca chegam a rodar.
 *
 * Limitações conscientes desta etapa:
 * - O Router só registra a escolha (via [RoutingDecision] no resultado); a
 *   chamada de rede pro provider/modelo escolhido ainda não existe — quem
 *   executa de fato é sempre [AgentSandboxSession.rodarCapacidade], dentro
 *   do Sandbox. Ligar o Router a uma chamada de API real fica para depois.
 * - Validação roda no processo do host (JVM), não dentro do proot — mesma
 *   limitação já documentada em [ExecutorValidacaoProjeto]; aqui só se
 *   aponta o executor pro [AgentSandboxSession.workspaceHostPath].
 * - "Corrigir e tentar de novo" (o Critic reescrevendo um passo REPROVADO)
 *   não está implementado — [executar] só reporta o resultado; decidir se
 *   tenta de novo é do chamador (mesma divisão de responsabilidade que
 *   [com.brain.router.AIRouter.proximaAlternativa] já assume na Etapa 5).
 * - Decision.ASK só é reportada como AGUARDANDO_APROVACAO; não há
 *   mecanismo de espera/retomada de aprovação humana aqui.
 * - Todos os passos compartilham o mesmo `actor`/`runId` e correm em
 *   série — sem paralelismo entre ramos independentes do plano ainda.
 */
class CicloExecucaoPlano(
    private val policyBroker: PolicyBroker,
    private val sandbox: Sandbox,
    private val router: AIRouter,
    private val catalog: ApiCatalog,
    private val validador: ExecutorValidacaoProjeto = ExecutorValidacaoProjeto(),
    private val profiles: List<RoutingProfile> = emptyList()
) {

    fun executar(plano: PlanoExecucao, runId: String, actor: String): ResultadoCiclo {
        val resultados = mutableListOf<ResultadoPasso>()
        val concluidos = mutableSetOf<String>()
        var abortado = false

        for (passo in plano.ordemDeExecucao) {
            if (abortado) {
                resultados += ResultadoPasso(
                    passoId = passo.id,
                    status = StatusPasso.BLOQUEADO_POR_DEPENDENCIA,
                    motivo = "ciclo abortado por um passo anterior não ter fechado"
                )
                continue
            }

            val dependenciaFaltando = passo.dependeDe.firstOrNull { it !in concluidos }
            if (dependenciaFaltando != null) {
                resultados += ResultadoPasso(
                    passoId = passo.id,
                    status = StatusPasso.BLOQUEADO_POR_DEPENDENCIA,
                    motivo = "depende de '$dependenciaFaltando', que não foi concluído com sucesso"
                )
                continue
            }

            val resultado = processarPasso(passo, runId, actor)
            resultados += resultado
            if (resultado.status == StatusPasso.APROVADO) concluidos += passo.id else abortado = true
        }

        return ResultadoCiclo(plano.objetivo, runId, resultados)
    }

    private fun processarPasso(passo: PassoPlano, runId: String, actor: String): ResultadoPasso {
        val decisaoRouter = passo.papel?.let { router.decidir(it, catalog, profiles) }

        val contexto = PolicyContext(runId = runId, taskId = passo.id, actor = actor, riskClass = passo.riskClass)
        val decisaoPolicy = policyBroker.authorize(actor, passo.capacidade, resource = passo.id, contexto)
        val autorizacao = ExecutionAuthorization.fromDecision(decisaoPolicy)
            ?: return ResultadoPasso(
                passoId = passo.id,
                status = if (decisaoPolicy.decision == Decision.ASK) StatusPasso.AGUARDANDO_APROVACAO else StatusPasso.NEGADO_PELA_POLICY,
                decisaoPolicy = decisaoPolicy,
                decisaoRouter = decisaoRouter,
                motivo = decisaoPolicy.reason
            )

        sandbox.abrirSessao(autorizacao).use { sessao ->
            val execucao = sessao.rodarCapacidade(passo.parametros)
            if (execucao is AgentSandboxSession.CommandOutcome.Refused) {
                return ResultadoPasso(
                    passoId = passo.id,
                    status = StatusPasso.REPROVADO,
                    decisaoPolicy = decisaoPolicy,
                    decisaoRouter = decisaoRouter,
                    execucao = execucao,
                    motivo = execucao.reason
                )
            }

            val evidencias = validador.validar(sessao.workspaceHostPath)
            val reprovado = evidencias.any { it.resultado == ResultadoValidacao.FALHOU }
            return ResultadoPasso(
                passoId = passo.id,
                status = if (reprovado) StatusPasso.REPROVADO else StatusPasso.APROVADO,
                decisaoPolicy = decisaoPolicy,
                decisaoRouter = decisaoRouter,
                execucao = execucao,
                evidencias = evidencias,
                motivo = if (reprovado) "validação encontrou ao menos uma evidência FALHOU" else null
            )
        }
    }
}
